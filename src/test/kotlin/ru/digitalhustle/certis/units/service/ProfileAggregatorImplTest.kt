package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.exception.custom.PhotoProcessingException
import ru.digitalhustle.certis.gateway.MinioGateway
import ru.digitalhustle.certis.model.NewProfile
import ru.digitalhustle.certis.model.NewProfilePhotoMeta
import ru.digitalhustle.certis.model.ProcessedProfilePhoto
import ru.digitalhustle.certis.model.UpdateProfileData
import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
import ru.digitalhustle.certis.model.objectName
import ru.digitalhustle.certis.service.aggregation.impl.ProfileAggregatorImpl
import ru.digitalhustle.certis.service.domain.ProfilePhotoMetaService
import ru.digitalhustle.certis.service.domain.ProfileService
import ru.digitalhustle.certis.service.photo.ProfilePhotoProcessor
import ru.digitalhustle.certis.service.photo.ProfilePhotoUrlProvider
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ProfileAggregatorImplTest {

    private val profileService = mock(ProfileService::class.java)
    private val profilePhotoMetaService = mock(ProfilePhotoMetaService::class.java)
    private val minioGateway = mock(MinioGateway::class.java)
    private val profilePhotoProcessor = mock(ProfilePhotoProcessor::class.java)
    private val profilePhotoUrlProvider = mock(ProfilePhotoUrlProvider::class.java)

    private val profileAggregator = ProfileAggregatorImpl(
        profileService = profileService,
        profilePhotoMetaService = profilePhotoMetaService,
        minioGateway = minioGateway,
        profilePhotoProcessor = profilePhotoProcessor,
        profilePhotoUrlProvider = profilePhotoUrlProvider,
    )

    private companion object {
        private const val NAME = "John"
        private const val SURNAME = "Doe"
        private const val PHOTO_URL = "http://localhost:9000/test-bucket/photo.jpg"
        private const val CONTENT_TYPE = MediaType.IMAGE_JPEG_VALUE
    }

    @Test
    fun `should get profile preview`() {
        // given
        val profile = createProfile()
        val photoMeta = createProfilePhotoMeta(profileId = profile.id)

        `when`(profileService.getById(profile.id))
            .thenReturn(profile)

        `when`(profilePhotoMetaService.getByProfileId(profile.id))
            .thenReturn(photoMeta)

        `when`(profilePhotoUrlProvider.get(profile.id))
            .thenReturn(PHOTO_URL)

        // when
        val profilePreview = profileAggregator.getProfilePreview(profile.id)

        // then
        assertThat(profilePreview.id).isEqualTo(profile.id)
        assertThat(profilePreview.name).isEqualTo(profile.name)
        assertThat(profilePreview.surname).isEqualTo(profile.surname)
        assertThat(profilePreview.dateOfBirth).isEqualTo(profile.dateOfBirth)
        assertThat(profilePreview.photoUrl).isEqualTo(PHOTO_URL)
    }

    @Test
    fun `should get profile photo`() {
        // given
        val profile = createProfile()
        val photoMeta = createProfilePhotoMeta(profileId = profile.id)
        val photoContent = "photo-content".toByteArray()

        `when`(profileService.getById(profile.id))
            .thenReturn(profile)

        `when`(profilePhotoMetaService.getByProfileId(profile.id))
            .thenReturn(photoMeta)

        `when`(minioGateway.getPhoto(photoMeta.objectName))
            .thenReturn(photoContent)

        // when
        val photo = profileAggregator.getPhoto(profile.id)

        // then
        assertThat(photo.content).isEqualTo(photoContent)
        assertThat(photo.contentType).isEqualTo(photoMeta.contentType)
    }

    @Test
    fun `should throw not found exception when profile photo is missing`() {
        // given
        val profile = createProfile()

        `when`(profileService.getById(profile.id))
            .thenReturn(profile)

        `when`(profilePhotoMetaService.getByProfileId(profile.id))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            profileAggregator.getPhoto(profile.id)
        }.isInstanceOf(NotFoundException::class.java)

        verifyNoInteractions(minioGateway)
    }

    @Test
    fun `should save profile`() {
        // given
        val newProfile = createNewProfile()
        val profile = createProfile(id = newProfile.id)

        `when`(profileService.save(newProfile))
            .thenReturn(profile)

        // when
        val savedProfile = profileAggregator.saveProfile(newProfile)

        // then
        assertThat(savedProfile).isEqualTo(profile)

        verify(profileService)
            .save(newProfile)
    }

    @Test
    fun `should update profile`() {
        // given
        val updateProfileData = createUpdateProfileData()
        val profile = createProfile(id = updateProfileData.id)

        `when`(profileService.update(updateProfileData))
            .thenReturn(profile)

        // when
        val updatedProfile = profileAggregator.updateProfile(updateProfileData)

        // then
        assertThat(updatedProfile).isEqualTo(profile)

        verify(profileService)
            .update(updateProfileData)
    }

    @Test
    fun `should upload photo`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()
        val processedPhoto = createProcessedPhoto(profileId)
        val savedPhotoMeta = createProfilePhotoMeta(
            id = processedPhoto.meta.id,
            profileId = profileId,
            extension = processedPhoto.meta.extension,
        )

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(null)

        `when`(profilePhotoProcessor.process(profileId, photo))
            .thenReturn(processedPhoto)

        `when`(profilePhotoMetaService.save(processedPhoto.meta))
            .thenReturn(savedPhotoMeta)

        // when
        val photoMeta = profileAggregator.uploadPhoto(profileId, photo)

        // then
        assertThat(photoMeta).isEqualTo(savedPhotoMeta)

        verify(minioGateway)
            .savePhoto(processedPhoto.objectName, photo, processedPhoto.contentType)
    }

    @Test
    fun `should delete uploaded photo when transaction rolls back`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()
        val processedPhoto = createProcessedPhoto(profileId)
        val savedPhotoMeta = createProfilePhotoMeta(
            id = processedPhoto.meta.id,
            profileId = profileId,
            extension = processedPhoto.meta.extension,
        )

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(null)

        `when`(profilePhotoProcessor.process(profileId, photo))
            .thenReturn(processedPhoto)

        `when`(profilePhotoMetaService.save(processedPhoto.meta))
            .thenReturn(savedPhotoMeta)

        TransactionSynchronizationManager.initSynchronization()
        try {
            // when
            profileAggregator.uploadPhoto(profileId, photo)
            TransactionSynchronizationManager.getSynchronizations()
                .forEach { it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK) }

            // then
            verify(minioGateway)
                .deletePhoto(processedPhoto.objectName)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `should throw not found exception when uploading photo for missing profile`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()

        `when`(profileService.exists(profileId))
            .thenReturn(false)

        // when, then
        assertThatThrownBy {
            profileAggregator.uploadPhoto(profileId, photo)
        }.isInstanceOf(NotFoundException::class.java)

        verifyNoInteractions(profilePhotoProcessor)
        verifyNoInteractions(minioGateway)
    }

    @Test
    fun `should throw already exists exception when uploading duplicate photo`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(createProfilePhotoMeta(profileId = profileId))

        // when, then
        assertThatThrownBy {
            profileAggregator.uploadPhoto(profileId, photo)
        }.isInstanceOf(EntityAlreadyExistsException::class.java)

        verifyNoInteractions(profilePhotoProcessor)
        verifyNoInteractions(minioGateway)
    }

    @Test
    fun `should update photo`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()
        val oldPhotoMeta = createProfilePhotoMeta(profileId = profileId)
        val processedPhoto = createProcessedPhoto(profileId)
        val savedPhotoMeta = createProfilePhotoMeta(
            id = processedPhoto.meta.id,
            profileId = profileId,
            extension = processedPhoto.meta.extension,
        )

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(oldPhotoMeta)

        `when`(profilePhotoProcessor.process(profileId, photo))
            .thenReturn(processedPhoto)

        `when`(profilePhotoMetaService.save(processedPhoto.meta))
            .thenReturn(savedPhotoMeta)

        // when
        val photoMeta = profileAggregator.updatePhoto(profileId, photo)

        // then
        assertThat(photoMeta).isEqualTo(savedPhotoMeta)

        verify(profilePhotoMetaService)
            .deleteByProfileId(profileId)

        verify(minioGateway)
            .savePhoto(processedPhoto.objectName, photo, processedPhoto.contentType)

        verify(minioGateway)
            .deletePhoto(oldPhotoMeta.objectName)
    }

    @Test
    fun `should delete replaced photo only after transaction commit`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()
        val oldPhotoMeta = createProfilePhotoMeta(profileId = profileId)
        val processedPhoto = createProcessedPhoto(profileId)
        val savedPhotoMeta = createProfilePhotoMeta(
            id = processedPhoto.meta.id,
            profileId = profileId,
            extension = processedPhoto.meta.extension,
        )

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(oldPhotoMeta)

        `when`(profilePhotoProcessor.process(profileId, photo))
            .thenReturn(processedPhoto)

        `when`(profilePhotoMetaService.save(processedPhoto.meta))
            .thenReturn(savedPhotoMeta)

        TransactionSynchronizationManager.initSynchronization()
        try {
            // when
            profileAggregator.updatePhoto(profileId, photo)

            // then
            verify(minioGateway, never())
                .deletePhoto(oldPhotoMeta.objectName)

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit)

            verify(minioGateway)
                .deletePhoto(oldPhotoMeta.objectName)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `should update photo when old photo does not exist`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()
        val processedPhoto = createProcessedPhoto(profileId)
        val savedPhotoMeta = createProfilePhotoMeta(
            id = processedPhoto.meta.id,
            profileId = profileId,
            extension = processedPhoto.meta.extension,
        )

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(null)

        `when`(profilePhotoProcessor.process(profileId, photo))
            .thenReturn(processedPhoto)

        `when`(profilePhotoMetaService.save(processedPhoto.meta))
            .thenReturn(savedPhotoMeta)

        // when
        val photoMeta = profileAggregator.updatePhoto(profileId, photo)

        // then
        assertThat(photoMeta).isEqualTo(savedPhotoMeta)

        verify(profilePhotoMetaService, never())
            .deleteByProfileId(profileId)

        verify(minioGateway, never())
            .deletePhoto(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun `should keep updated photo when deleting old photo fails`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()
        val oldPhotoMeta = createProfilePhotoMeta(profileId = profileId)
        val processedPhoto = createProcessedPhoto(profileId)
        val savedPhotoMeta = createProfilePhotoMeta(
            id = processedPhoto.meta.id,
            profileId = profileId,
            extension = processedPhoto.meta.extension,
        )

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(oldPhotoMeta)

        `when`(profilePhotoProcessor.process(profileId, photo))
            .thenReturn(processedPhoto)

        `when`(profilePhotoMetaService.save(processedPhoto.meta))
            .thenReturn(savedPhotoMeta)

        doThrow(PhotoProcessingException("Delete failed"))
            .`when`(minioGateway)
            .deletePhoto(oldPhotoMeta.objectName)

        // when
        val photoMeta = profileAggregator.updatePhoto(profileId, photo)

        // then
        assertThat(photoMeta).isEqualTo(savedPhotoMeta)

        verify(minioGateway)
            .savePhoto(processedPhoto.objectName, photo, processedPhoto.contentType)
    }

    @Test
    fun `should throw not found exception when updating photo for missing profile`() {
        // given
        val profileId = UUID.randomUUID()
        val photo = createPhoto()

        `when`(profileService.exists(profileId))
            .thenReturn(false)

        // when, then
        assertThatThrownBy {
            profileAggregator.updatePhoto(profileId, photo)
        }.isInstanceOf(NotFoundException::class.java)

        verifyNoInteractions(profilePhotoProcessor)
        verifyNoInteractions(minioGateway)
    }

    @Test
    fun `should delete profile with photo`() {
        // given
        val profileId = UUID.randomUUID()
        val photoMeta = createProfilePhotoMeta(profileId = profileId)

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(photoMeta)

        // when
        profileAggregator.deleteProfile(profileId)

        // then
        verify(profilePhotoMetaService)
            .deleteByProfileId(profileId)

        verify(profileService)
            .delete(profileId)

        verify(minioGateway)
            .deletePhoto(photoMeta.objectName)
    }

    @Test
    fun `should delete photo by profile id`() {
        // given
        val profileId = UUID.randomUUID()
        val photoMeta = createProfilePhotoMeta(profileId = profileId)

        `when`(profileService.exists(profileId))
            .thenReturn(true)

        `when`(profilePhotoMetaService.getByProfileId(profileId))
            .thenReturn(photoMeta)

        // when
        profileAggregator.deletePhotoByProfileId(profileId)

        // then
        verify(profilePhotoMetaService)
            .deleteByProfileId(profileId)

        verify(minioGateway)
            .deletePhoto(photoMeta.objectName)
    }

    @Test
    fun `should throw not found exception when deleting photo for missing profile`() {
        // given
        val profileId = UUID.randomUUID()

        `when`(profileService.exists(profileId))
            .thenReturn(false)

        // when, then
        assertThatThrownBy {
            profileAggregator.deletePhotoByProfileId(profileId)
        }.isInstanceOf(NotFoundException::class.java)

        verify(profilePhotoMetaService, never())
            .deleteByProfileId(profileId)

        verifyNoInteractions(minioGateway)
    }

    private fun createProcessedPhoto(profileId: UUID = UUID.randomUUID()): ProcessedProfilePhoto {
        val newPhotoMeta = createNewProfilePhotoMeta(profileId = profileId)

        return ProcessedProfilePhoto(
            meta = newPhotoMeta,
            objectName = "${newPhotoMeta.id}.${newPhotoMeta.extension}",
            contentType = CONTENT_TYPE,
        )
    }

    private fun createPhoto(): MultipartFile = mock(MultipartFile::class.java)

    private fun createNewProfile(
        id: UUID = UUID.randomUUID(),
        name: String = NAME,
        surname: String = SURNAME,
        dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
    ): NewProfile =
        NewProfile(
            id = id,
            name = name,
            surname = surname,
            dateOfBirth = dateOfBirth,
        )

    private fun createUpdateProfileData(
        id: UUID = UUID.randomUUID(),
        name: String = NAME,
        surname: String = SURNAME,
        dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
    ): UpdateProfileData =
        UpdateProfileData(
            id = id,
            name = name,
            surname = surname,
            dateOfBirth = dateOfBirth,
        )

    private fun createProfile(
        id: UUID = UUID.randomUUID(),
        name: String = NAME,
        surname: String = SURNAME,
        dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
    ): Profile =
        Profile(
            id = id,
            name = name,
            surname = surname,
            dateOfBirth = dateOfBirth,
            updatedAt = OffsetDateTime.now(),
        )

    private fun createNewProfilePhotoMeta(
        id: UUID = UUID.randomUUID(),
        profileId: UUID = UUID.randomUUID(),
        extension: String = "jpg",
    ): NewProfilePhotoMeta =
        NewProfilePhotoMeta(
            id = id,
            profileId = profileId,
            originalFileName = "profile-photo",
            extension = extension,
            fileSize = 1024L,
            width = 100,
            height = 200,
            contentType = CONTENT_TYPE,
            url = PHOTO_URL,
        )

    private fun createProfilePhotoMeta(
        id: UUID = UUID.randomUUID(),
        profileId: UUID = UUID.randomUUID(),
        extension: String = "jpg",
    ): ProfilePhotoMeta =
        ProfilePhotoMeta(
            id = id,
            profileId = profileId,
            originalFileName = "profile-photo",
            extension = extension,
            fileSize = 1024L,
            width = 100,
            height = 200,
            contentType = CONTENT_TYPE,
            url = PHOTO_URL,
            uploadedAt = OffsetDateTime.now(),
        )
}

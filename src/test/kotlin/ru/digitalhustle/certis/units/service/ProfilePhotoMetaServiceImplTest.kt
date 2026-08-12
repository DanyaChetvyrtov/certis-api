package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
import ru.digitalhustle.certis.model.profile.NewProfilePhotoMeta
import ru.digitalhustle.certis.repository.ProfilePhotoMetaRepository
import ru.digitalhustle.certis.service.domain.impl.ProfilePhotoMetaServiceImpl
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ProfilePhotoMetaServiceImplTest {

    private val profilePhotoMetaRepository = mock(ProfilePhotoMetaRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC)

    private val profilePhotoMetaService = ProfilePhotoMetaServiceImpl(profilePhotoMetaRepository, clock)

    private companion object {
        private const val ORIGINAL_FILE_NAME = "profile-photo"
        private const val EXTENSION = "jpg"
        private const val FILE_SIZE = 1024L
        private const val WIDTH = 100
        private const val HEIGHT = 200
        private const val CONTENT_TYPE = "image/jpeg"
        private const val URL = "http://localhost:9000/test-bucket/photo.jpg"
    }

    @Test
    fun `should get photo meta by id`() {
        // given
        val photoMeta = createProfilePhotoMeta()

        `when`(profilePhotoMetaRepository.findById(photoMeta.id))
            .thenReturn(photoMeta)

        // when
        val foundPhotoMeta = profilePhotoMetaService.getById(photoMeta.id)

        // then
        assertThat(foundPhotoMeta).isEqualTo(photoMeta)

        verify(profilePhotoMetaRepository)
            .findById(photoMeta.id)
    }

    @Test
    fun `should throw not found exception when photo meta is not found by id`() {
        // given
        val id = UUID.randomUUID()

        `when`(profilePhotoMetaRepository.findById(id))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            profilePhotoMetaService.getById(id)
        }.isInstanceOf(NotFoundException::class.java)

        verify(profilePhotoMetaRepository)
            .findById(id)
    }

    @Test
    fun `should get photo meta by profile id`() {
        // given
        val profileId = UUID.randomUUID()
        val photoMeta = createProfilePhotoMeta(profileId = profileId)

        `when`(profilePhotoMetaRepository.findByProfileId(profileId))
            .thenReturn(photoMeta)

        // when
        val foundPhotoMeta = profilePhotoMetaService.getByProfileId(profileId)

        // then
        assertThat(foundPhotoMeta).isEqualTo(photoMeta)

        verify(profilePhotoMetaRepository)
            .findByProfileId(profileId)
    }

    @Test
    fun `should save photo meta`() {
        // given
        val newPhotoMeta = createNewProfilePhotoMeta()
        val savedPhotoMeta = createProfilePhotoMeta(id = newPhotoMeta.id, profileId = newPhotoMeta.profileId)

        `when`(profilePhotoMetaRepository.save(anyProfilePhotoMeta()))
            .thenReturn(savedPhotoMeta)

        // when
        val photoMeta = profilePhotoMetaService.save(newPhotoMeta)

        // then
        assertThat(photoMeta).isEqualTo(savedPhotoMeta)

        val photoMetaCaptor = ArgumentCaptor.forClass(ProfilePhotoMeta::class.java)
        verify(profilePhotoMetaRepository)
            .save(captureProfilePhotoMeta(photoMetaCaptor))

        assertAll(
            { assertThat(photoMetaCaptor.value.id).isEqualTo(newPhotoMeta.id) },
            { assertThat(photoMetaCaptor.value.profileId).isEqualTo(newPhotoMeta.profileId) },
            { assertThat(photoMetaCaptor.value.originalFileName).isEqualTo(newPhotoMeta.originalFileName) },
            { assertThat(photoMetaCaptor.value.extension).isEqualTo(newPhotoMeta.extension) },
            { assertThat(photoMetaCaptor.value.fileSize).isEqualTo(newPhotoMeta.fileSize) },
            { assertThat(photoMetaCaptor.value.width).isEqualTo(newPhotoMeta.width) },
            { assertThat(photoMetaCaptor.value.height).isEqualTo(newPhotoMeta.height) },
            { assertThat(photoMetaCaptor.value.contentType).isEqualTo(newPhotoMeta.contentType) },
            { assertThat(photoMetaCaptor.value.url).isEqualTo(newPhotoMeta.url) },
            { assertThat(photoMetaCaptor.value.uploadedAt).isEqualTo(OffsetDateTime.now(clock)) },
        )
    }

    @Test
    fun `should delete photo meta by profile id`() {
        // given
        val profileId = UUID.randomUUID()

        // when
        profilePhotoMetaService.deleteByProfileId(profileId)

        // then
        verify(profilePhotoMetaRepository)
            .deleteByProfileId(profileId)
    }

    private fun createNewProfilePhotoMeta(
        id: UUID = UUID.randomUUID(),
        profileId: UUID = UUID.randomUUID(),
    ): NewProfilePhotoMeta =
        NewProfilePhotoMeta(
            id = id,
            profileId = profileId,
            originalFileName = ORIGINAL_FILE_NAME,
            extension = EXTENSION,
            fileSize = FILE_SIZE,
            width = WIDTH,
            height = HEIGHT,
            contentType = CONTENT_TYPE,
            url = URL,
        )

    private fun createProfilePhotoMeta(
        id: UUID = UUID.randomUUID(),
        profileId: UUID = UUID.randomUUID(),
    ): ProfilePhotoMeta =
        ProfilePhotoMeta(
            id = id,
            profileId = profileId,
            originalFileName = ORIGINAL_FILE_NAME,
            extension = EXTENSION,
            fileSize = FILE_SIZE,
            width = WIDTH,
            height = HEIGHT,
            contentType = CONTENT_TYPE,
            url = URL,
            uploadedAt = OffsetDateTime.now(),
        )

    private fun anyProfilePhotoMeta(): ProfilePhotoMeta {
        org.mockito.Mockito.any<ProfilePhotoMeta>()
        return createProfilePhotoMeta()
    }

    private fun captureProfilePhotoMeta(captor: ArgumentCaptor<ProfilePhotoMeta>): ProfilePhotoMeta {
        captor.capture()
        return createProfilePhotoMeta()
    }
}

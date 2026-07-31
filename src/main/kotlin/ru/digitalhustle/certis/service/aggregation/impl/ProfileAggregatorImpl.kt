package ru.digitalhustle.certis.service.aggregation.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.exception.custom.PhotoProcessingException
import ru.digitalhustle.certis.gateway.MinioGateway
import ru.digitalhustle.certis.mapper.toPreview
import ru.digitalhustle.certis.model.NewProfile
import ru.digitalhustle.certis.model.ProfilePhoto
import ru.digitalhustle.certis.model.ProfilePreview
import ru.digitalhustle.certis.model.UpdateProfileData
import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
import ru.digitalhustle.certis.model.objectName
import ru.digitalhustle.certis.service.aggregation.ProfileAggregator
import ru.digitalhustle.certis.service.domain.ProfilePhotoMetaService
import ru.digitalhustle.certis.service.domain.ProfileService
import ru.digitalhustle.certis.service.photo.ProfilePhotoProcessor
import ru.digitalhustle.certis.service.photo.ProfilePhotoUrlProvider
import java.util.UUID

@Service
class ProfileAggregatorImpl(
    private val profileService: ProfileService,
    private val profilePhotoMetaService: ProfilePhotoMetaService,
    private val minioGateway: MinioGateway,
    private val profilePhotoProcessor: ProfilePhotoProcessor,
    private val profilePhotoUrlProvider: ProfilePhotoUrlProvider,
) : ProfileAggregator {

    private val log = KotlinLogging.logger {}

    override fun getProfilePreview(profileId: UUID): ProfilePreview {
        val profile = profileService.getById(profileId)
        val photoUrl = profilePhotoMetaService.getByProfileId(profileId)
            ?.let { profilePhotoUrlProvider.get(profileId) }

        return profile.toPreview(photoUrl)
    }

    override fun getPhoto(profileId: UUID): ProfilePhoto {
        profileService.getById(profileId)

        val photoMeta = profilePhotoMetaService.getByProfileId(profileId)
            ?: throw NotFoundException.entity("ProfilePhoto")

        return ProfilePhoto(
            content = minioGateway.getPhoto(photoMeta.objectName),
            contentType = photoMeta.contentType,
        )
    }

    override fun saveProfile(profile: NewProfile): Profile = profileService.save(profile)

    @Transactional
    override fun uploadPhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        if (!profileService.exists(profileId)) {
            throw NotFoundException.entity("Profile")
        }
        if (profilePhotoMetaService.getByProfileId(profileId) != null) {
            throw EntityAlreadyExistsException.entity("ProfilePhoto", "profileId")
        }

        return saveNewPhoto(profileId, photo)
    }

    override fun updateProfile(profile: UpdateProfileData): Profile = profileService.update(profile)

    @Transactional
    override fun updatePhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        if (!profileService.exists(profileId)) {
            throw NotFoundException.entity("Profile")
        }

        val oldPhotoMeta = profilePhotoMetaService.getByProfileId(profileId)

        oldPhotoMeta?.let {
            profilePhotoMetaService.deleteByProfileId(profileId)
        }

        val savedPhotoMeta = saveNewPhoto(profileId, photo)

        oldPhotoMeta?.let {
            deletePhotoAfterCommit(it)
        }

        return savedPhotoMeta
    }

    @Transactional
    override fun deleteProfile(profileId: UUID) {
        if (!profileService.exists(profileId)) {
            return
        }

        val photoMeta = profilePhotoMetaService.getByProfileId(profileId)

        profilePhotoMetaService.deleteByProfileId(profileId)
        profileService.delete(profileId)
        photoMeta?.let(::deletePhotoAfterCommit)
    }

    @Transactional
    override fun deletePhotoByProfileId(profileId: UUID) {
        if (!profileService.exists(profileId)) {
            throw NotFoundException.entity("Profile")
        }

        val photoMeta = profilePhotoMetaService.getByProfileId(profileId)

        profilePhotoMetaService.deleteByProfileId(profileId)
        photoMeta?.let(::deletePhotoAfterCommit)
    }

    private fun saveNewPhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        val processedPhoto = profilePhotoProcessor.process(profileId, photo)

        registerTransactionCallbacks(
            afterRollback = {
                deletePhotoSafely(processedPhoto.objectName)
            },
        )

        val savedPhotoMeta = profilePhotoMetaService.save(processedPhoto.meta)

        minioGateway.savePhoto(
            objectName = processedPhoto.objectName,
            photo = photo,
            contentType = processedPhoto.contentType,
        )

        return savedPhotoMeta
    }

    private fun deletePhotoAfterCommit(photoMeta: ProfilePhotoMeta) {
        registerTransactionCallbacks(
            afterCommit = {
                deletePhotoSafely(photoMeta.objectName)
            },
        )
    }

    private fun deletePhotoSafely(objectName: String) {
        try {
            minioGateway.deletePhoto(objectName)
        } catch (exception: PhotoProcessingException) {
            log.warn(exception) {
                "Failed to clean up profile photo '$objectName'"
            }
        }
    }

    private fun registerTransactionCallbacks(
        afterCommit: (() -> Unit)? = null,
        afterRollback: (() -> Unit)? = null,
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            afterCommit?.invoke()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    afterCommit?.invoke()
                }

                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        afterRollback?.invoke()
                    }
                }
            },
        )
    }
}

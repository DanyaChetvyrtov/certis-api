package ru.digitalhustle.certis.features.profile.application.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.profile.application.service.ProfileApplicationService
import ru.digitalhustle.certis.features.profile.command.model.NewProfile
import ru.digitalhustle.certis.features.profile.command.model.UpdateProfileData
import ru.digitalhustle.certis.features.profile.command.service.ProfilePhotoMetaService
import ru.digitalhustle.certis.features.profile.command.service.ProfileService
import ru.digitalhustle.certis.features.profile.command.util.ProfilePhotoProcessor
import ru.digitalhustle.certis.features.profile.exceptions.PhotoProcessingException
import ru.digitalhustle.certis.features.profile.gateway.MinioGateway
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.model.ProfilePreview
import ru.digitalhustle.certis.features.profile.model.objectName
import ru.digitalhustle.certis.features.profile.model.toPreview
import ru.digitalhustle.certis.features.profile.util.ProfilePhotoUrlProvider
import ru.digitalhustle.certis.features.security.api.UserPreferencesCommand
import ru.digitalhustle.certis.features.security.api.UserPreferencesQuery
import java.util.UUID

@Service
class ProfileApplicationServiceImpl(
    private val profileService: ProfileService,
    private val userPreferencesCommand: UserPreferencesCommand,
    private val userPreferencesQuery: UserPreferencesQuery,
    private val profilePhotoMetaService: ProfilePhotoMetaService,
    private val minioGateway: MinioGateway,
    private val profilePhotoProcessor: ProfilePhotoProcessor,
    private val profilePhotoUrlProvider: ProfilePhotoUrlProvider,
) : ProfileApplicationService {

    private val log = KotlinLogging.logger {}

    @Transactional
    override fun saveProfile(
        profile: NewProfile,
        preferredCurrency: Currency,
    ): ProfilePreview {
        val savedProfile = profileService.save(profile)
        userPreferencesCommand.updatePreferredCurrency(savedProfile.id, preferredCurrency)

        return savedProfile.toPreview(
            photoUrl = null,
            preferredCurrency = preferredCurrency,
        )
    }

    @Transactional
    override fun uploadPhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        if (!profileService.exists(profileId)) {
            throw NotFoundException.Companion.entity("Profile")
        }
        if (profilePhotoMetaService.getByProfileId(profileId) != null) {
            throw EntityAlreadyExistsException.Companion.entity("ProfilePhoto", "profileId")
        }

        return saveNewPhoto(profileId, photo)
    }

    @Transactional
    override fun updateProfile(
        profile: UpdateProfileData,
        preferredCurrency: Currency?,
    ): ProfilePreview {
        val updatedProfile = profileService.update(profile)
        val effectiveCurrency = preferredCurrency ?: userPreferencesQuery.getPreferredCurrency(updatedProfile.id)

        preferredCurrency?.let {
            userPreferencesCommand.updatePreferredCurrency(updatedProfile.id, it)
        }

        return updatedProfile.toPreview(
            photoUrl = getPhotoUrl(updatedProfile.id),
            preferredCurrency = effectiveCurrency,
        )
    }

    @Transactional
    override fun updatePhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        if (!profileService.exists(profileId)) {
            throw NotFoundException.Companion.entity("Profile")
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

    private fun getPhotoUrl(profileId: UUID): String? =
        profilePhotoMetaService.getByProfileId(profileId)
            ?.let { profilePhotoUrlProvider.get(profileId) }

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

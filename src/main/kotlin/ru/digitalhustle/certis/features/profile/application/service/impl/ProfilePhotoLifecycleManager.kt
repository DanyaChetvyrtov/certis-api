package ru.digitalhustle.certis.features.profile.application.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.features.profile.application.service.ProfilePhotoSupport
import ru.digitalhustle.certis.features.profile.command.service.ProfilePhotoMetaService
import ru.digitalhustle.certis.features.profile.command.util.ProfilePhotoProcessor
import ru.digitalhustle.certis.features.profile.exceptions.PhotoProcessingException
import ru.digitalhustle.certis.features.profile.gateway.MinioGateway
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.model.objectName
import ru.digitalhustle.certis.features.profile.util.ProfilePhotoUrlProvider
import java.util.UUID

@Component
class ProfilePhotoLifecycleManager(
    private val profilePhotoMetaService: ProfilePhotoMetaService,
    private val minioGateway: MinioGateway,
    private val profilePhotoProcessor: ProfilePhotoProcessor,
    private val profilePhotoUrlProvider: ProfilePhotoUrlProvider,
) : ProfilePhotoSupport {

    private val log = KotlinLogging.logger {}

    override fun getPhotoUrl(profileId: UUID): String? =
        profilePhotoMetaService.getByProfileId(profileId)
            ?.let { profilePhotoUrlProvider.get(profileId) }

    fun upload(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        if (profilePhotoMetaService.getByProfileId(profileId) != null) {
            throw EntityAlreadyExistsException.entity("ProfilePhoto", "profileId")
        }

        return saveNewPhoto(profileId, photo)
    }

    fun replace(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        val oldPhotoMeta = profilePhotoMetaService.getByProfileId(profileId)

        oldPhotoMeta?.let {
            profilePhotoMetaService.deleteByProfileId(profileId)
        }

        val savedPhotoMeta = saveNewPhoto(profileId, photo)

        oldPhotoMeta?.let(::deletePhotoAfterCommit)

        return savedPhotoMeta
    }

    override fun deletePhoto(profileId: UUID) {
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

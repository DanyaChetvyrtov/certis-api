package ru.digitalhustle.certis.features.profile.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.features.profile.application.service.ProfilePhotoApplicationService
import ru.digitalhustle.certis.features.profile.command.service.ProfileService
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import java.util.UUID

@Service
class ProfilePhotoApplicationServiceImpl(
    private val profileService: ProfileService,
    private val profilePhotoLifecycleManager: ProfilePhotoLifecycleManager,
) : ProfilePhotoApplicationService {

    @Transactional
    override fun uploadPhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        profileService.requireExists(profileId)

        return profilePhotoLifecycleManager.upload(profileId, photo)
    }

    @Transactional
    override fun updatePhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta {
        profileService.requireExists(profileId)

        return profilePhotoLifecycleManager.replace(profileId, photo)
    }

    @Transactional
    override fun deletePhotoByProfileId(profileId: UUID) {
        profileService.requireExists(profileId)
        profilePhotoLifecycleManager.deletePhoto(profileId)
    }
}

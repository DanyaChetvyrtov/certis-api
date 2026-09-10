package ru.digitalhustle.certis.features.profile.application.service

import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import java.util.UUID

interface ProfilePhotoApplicationService {

    fun uploadPhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta

    fun updatePhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta

    fun deletePhotoByProfileId(profileId: UUID)
}

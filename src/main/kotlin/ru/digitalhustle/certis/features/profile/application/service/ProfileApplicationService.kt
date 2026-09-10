package ru.digitalhustle.certis.features.profile.application.service

import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.profile.command.model.NewProfile
import ru.digitalhustle.certis.features.profile.command.model.UpdateProfileData
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.model.ProfilePreview
import java.util.UUID

interface ProfileApplicationService {

    fun saveProfile(profile: NewProfile, preferredCurrency: Currency): ProfilePreview

    fun uploadPhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta

    fun updateProfile(profile: UpdateProfileData, preferredCurrency: Currency?): ProfilePreview

    fun updatePhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta

    fun deleteProfile(profileId: UUID)

    fun deletePhotoByProfileId(profileId: UUID)
}

package ru.digitalhustle.certis.service.profile

import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
import ru.digitalhustle.certis.model.profile.NewProfile
import ru.digitalhustle.certis.model.profile.ProfilePhoto
import ru.digitalhustle.certis.model.profile.ProfilePreview
import ru.digitalhustle.certis.model.profile.UpdateProfileData
import java.util.UUID

interface ProfileAggregator {

    fun getProfilePreview(profileId: UUID): ProfilePreview

    fun getPhoto(profileId: UUID): ProfilePhoto

    fun saveProfile(profile: NewProfile): Profile

    fun uploadPhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta

    fun updateProfile(profile: UpdateProfileData): Profile

    fun updatePhoto(profileId: UUID, photo: MultipartFile): ProfilePhotoMeta

    fun deleteProfile(profileId: UUID)

    fun deletePhotoByProfileId(profileId: UUID)
}

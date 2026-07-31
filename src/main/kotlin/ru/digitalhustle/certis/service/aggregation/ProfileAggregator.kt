package ru.digitalhustle.certis.service.aggregation

import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.model.NewProfile
import ru.digitalhustle.certis.model.ProfilePhoto
import ru.digitalhustle.certis.model.ProfilePreview
import ru.digitalhustle.certis.model.UpdateProfileData
import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
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

package ru.digitalhustle.certis.features.profile.command.service

import ru.digitalhustle.certis.features.profile.command.model.NewProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import java.util.UUID

interface ProfilePhotoMetaService {

    fun getByProfileId(profileId: UUID): ProfilePhotoMeta?

    fun save(profilePhotoMeta: NewProfilePhotoMeta): ProfilePhotoMeta

    fun deleteByProfileId(profileId: UUID)
}

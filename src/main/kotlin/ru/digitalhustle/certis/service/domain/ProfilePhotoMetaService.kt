package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
import ru.digitalhustle.certis.model.profile.NewProfilePhotoMeta
import java.util.UUID

interface ProfilePhotoMetaService {

    fun getById(id: UUID): ProfilePhotoMeta

    fun getByProfileId(profileId: UUID): ProfilePhotoMeta?

    fun save(profilePhotoMeta: NewProfilePhotoMeta): ProfilePhotoMeta

    fun deleteByProfileId(profileId: UUID)
}

package ru.digitalhustle.certis.features.profile.query.service

import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import java.util.UUID

interface ProfilePhotoMetaQueryService {

    fun getById(id: UUID): ProfilePhotoMeta

    fun getByProfileId(profileId: UUID): ProfilePhotoMeta?
}

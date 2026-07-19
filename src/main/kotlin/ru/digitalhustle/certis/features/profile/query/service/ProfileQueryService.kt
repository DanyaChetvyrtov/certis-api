package ru.digitalhustle.certis.features.profile.query.service

import ru.digitalhustle.certis.features.profile.model.ProfilePreview
import ru.digitalhustle.certis.features.profile.query.model.ProfilePhoto
import java.util.UUID

interface ProfileQueryService {

    fun getProfilePreview(profileId: UUID): ProfilePreview

    fun getPhoto(profileId: UUID): ProfilePhoto
}

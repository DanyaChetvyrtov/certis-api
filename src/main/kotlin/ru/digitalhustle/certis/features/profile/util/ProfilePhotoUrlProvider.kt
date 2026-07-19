package ru.digitalhustle.certis.features.profile.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.config.properties.AppApiProperties
import java.util.UUID

@Component
class ProfilePhotoUrlProvider(
    private val appApiProperties: AppApiProperties,
) {

    fun get(profileId: UUID): String =
        "${appApiProperties.publicUrl.trimEnd('/')}${PathConstants.profilePhoto(profileId)}"
}

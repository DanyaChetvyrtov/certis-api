package ru.digitalhustle.certis.service.photo

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.config.properties.AppApiProperties
import ru.digitalhustle.certis.constants.PathConstants
import java.util.UUID

@Component
class ProfilePhotoUrlProvider(
    private val appApiProperties: AppApiProperties,
) {

    fun get(profileId: UUID): String =
        "${appApiProperties.publicUrl.trimEnd('/')}${PathConstants.profilePhoto(profileId)}"
}

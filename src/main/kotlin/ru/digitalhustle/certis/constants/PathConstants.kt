package ru.digitalhustle.certis.constants

import java.util.UUID

object PathConstants {
    const val API = "/api"

    const val API_V1 = "$API/v1"

    const val AUTH = "$API_V1/auth"
    const val REGISTRATION = "/registration"

    const val TOKENS = "/tokens"
    const val TOKENS_ACCESS = "$TOKENS/access"
    const val TOKENS_BOTH = "$TOKENS/both"

    const val AUTH_TOKEN = "$AUTH$TOKENS"

    const val PROFILES = "$API_V1/profiles"
    const val PROFILE_ID = "/{profileId}"
    const val PROFILE_PHOTO = "$PROFILE_ID/photo"

    fun profilePhoto(profileId: UUID): String = "$PROFILES/$profileId/photo"
}

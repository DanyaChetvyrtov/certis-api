package ru.digitalhustle.certis.constants

import java.util.UUID

object PathConstants {
    const val API = "/api"

    const val API_V1 = "$API/v1"

    const val AUTH = "$API_V1/auth"
    const val REGISTRATION = "/registration"
    const val TOKENS = "/tokens"
    const val LOGOUT = "/logout"
    const val SESSIONS = "/sessions"
    const val SESSION_ID = "/{sessionId}"

    const val AUTH_REGISTRATION = "$AUTH$REGISTRATION"
    const val AUTH_TOKEN = "$AUTH$TOKENS"
    const val AUTH_LOGOUT = "$AUTH$LOGOUT"
    const val AUTH_SESSIONS = "$AUTH$SESSIONS"
    const val SESSIONS_WITH_ID = "${SESSIONS}${SESSION_ID}"

    const val PROFILES = "$API_V1/profiles"
    const val PROFILE_ID = "/{profileId}"
    const val MY_PROFILE = "/me"
    const val PROFILE_PHOTO = "$PROFILE_ID/photo"

    fun profilePhoto(profileId: UUID): String = "$PROFILES/$profileId/photo"

    const val ACCOUNTS = "$API_V1/accounts"
    const val ACCOUNT_ID = "/{accountId}"
}

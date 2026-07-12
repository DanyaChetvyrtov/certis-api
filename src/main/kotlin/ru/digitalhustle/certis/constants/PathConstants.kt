package ru.digitalhustle.certis.constants

object PathConstants {
    const val API = "/api"

    const val API_V1 = "$API/v1"

    const val AUTH = "$API_V1/auth"
    const val REGISTRATION = "/registration"

    const val TOKENS = "/tokens"
    const val TOKENS_ACCESS = "$TOKENS/access"
    const val TOKENS_BOTH = "$TOKENS/both"
}

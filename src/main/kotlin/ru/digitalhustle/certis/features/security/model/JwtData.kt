package ru.digitalhustle.certis.features.security.model

import java.util.UUID

data class JwtData(

    val id: UUID,

    val email: String,

    val accessToken: String,

    val refreshToken: String,
)

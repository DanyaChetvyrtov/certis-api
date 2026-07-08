package ru.digitalhustle.certis.model.security

import java.util.UUID

data class JwtData(

    val id: UUID,

    val email: String,

    val accessToken: String,

    val refreshToken: String
)

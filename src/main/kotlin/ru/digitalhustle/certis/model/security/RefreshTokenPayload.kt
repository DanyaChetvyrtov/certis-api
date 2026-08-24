package ru.digitalhustle.certis.model.security

import java.util.UUID

data class RefreshTokenPayload(

    val sessionId: UUID,

    val userId: UUID,

    val email: String,
)

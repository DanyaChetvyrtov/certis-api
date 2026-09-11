package ru.digitalhustle.certis.features.security.model

import java.util.UUID

data class RefreshTokenPayload(

    val sessionId: UUID,

    val userId: UUID,

    val email: String,
)

package ru.digitalhustle.certis.dto

import java.time.OffsetDateTime
import java.util.UUID

data class AuthSessionDto(

    val id: UUID,

    val lastRefreshedAt: OffsetDateTime,

    val expiresAt: OffsetDateTime,
)

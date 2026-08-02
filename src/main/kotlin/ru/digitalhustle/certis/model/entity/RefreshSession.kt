package ru.digitalhustle.certis.model.entity

import java.time.OffsetDateTime
import java.util.UUID

data class RefreshSession(

    val id: UUID,

    val familyId: UUID,

    val userId: UUID,

    val expiresAt: OffsetDateTime,

    val usedAt: OffsetDateTime?,

    val revokedAt: OffsetDateTime?,

    val createdAt: OffsetDateTime,
)

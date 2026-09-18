package ru.digitalhustle.certis.features.profile.model

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Profile(

    val id: UUID,

    val name: String,

    val surname: String,

    val dateOfBirth: LocalDate,

    val updatedAt: OffsetDateTime,
)

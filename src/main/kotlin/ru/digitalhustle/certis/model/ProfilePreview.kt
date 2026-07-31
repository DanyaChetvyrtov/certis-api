package ru.digitalhustle.certis.model

import java.time.LocalDate
import java.util.UUID

data class ProfilePreview(

    val id: UUID,

    val name: String,

    val surname: String,

    val dateOfBirth: LocalDate,

    val photoUrl: String?,
)

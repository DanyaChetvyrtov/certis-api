package ru.digitalhustle.certis.model

import java.time.LocalDate
import java.util.UUID

data class NewProfile(

    val id: UUID,

    val name: String,

    val surname: String,

    val dateOfBirth: LocalDate,
)

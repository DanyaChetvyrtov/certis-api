package ru.digitalhustle.certis.model.profile

import java.time.LocalDate
import java.util.UUID

data class UpdateProfileData(

    val id: UUID,

    val name: String,

    val surname: String,

    val dateOfBirth: LocalDate,
)

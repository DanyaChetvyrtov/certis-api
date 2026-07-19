package ru.digitalhustle.certis.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileDto(

    val id: UUID,

    val name: String,

    val surname: String,

    val dateOfBirth: LocalDate,

    val photoUrl: String?,
)

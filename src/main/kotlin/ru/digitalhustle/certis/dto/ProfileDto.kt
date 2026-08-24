package ru.digitalhustle.certis.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.Currency
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileDto(

    val id: UUID,

    val name: String,

    val surname: String,

    val dateOfBirth: LocalDate,

    val preferredCurrency: Currency,

    val photoUrl: String?,
)

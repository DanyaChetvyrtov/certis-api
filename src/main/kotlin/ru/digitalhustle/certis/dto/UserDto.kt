package ru.digitalhustle.certis.dto

import ru.digitalhustle.certis.enums.Currency
import java.util.UUID

data class UserDto(

    val id: UUID,

    val email: String,

    val preferredCurrency: Currency,
)

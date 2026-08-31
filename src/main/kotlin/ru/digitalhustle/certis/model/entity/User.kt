package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.Currency
import java.time.OffsetDateTime
import java.util.UUID

data class User(

    val id: UUID,

    val email: String,

    val passwordHash: String,

    val preferredCurrency: Currency = Currency.USD,

    val lastLogin: OffsetDateTime?,

    val createdAt: OffsetDateTime,
)

package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Account(

    val id: UUID,

    val userId: UUID,

    val name: String,

    val type: AccountType,

    val openingBalance: BigDecimal,

    val currency: Currency,

    val createdAt: OffsetDateTime,

    val closedAt: OffsetDateTime?,
)

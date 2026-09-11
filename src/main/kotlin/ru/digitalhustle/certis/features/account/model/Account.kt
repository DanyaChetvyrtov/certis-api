package ru.digitalhustle.certis.features.account.model

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.account.enums.AccountType
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

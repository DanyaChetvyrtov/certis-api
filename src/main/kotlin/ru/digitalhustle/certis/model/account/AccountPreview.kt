package ru.digitalhustle.certis.model.account

import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class AccountPreview(

    val id: UUID,

    val name: String,

    val type: AccountType,

    val openingBalance: BigDecimal,

    val balance: BigDecimal,

    val currency: Currency,

    val createdAt: OffsetDateTime,

    val closedAt: OffsetDateTime?,
)

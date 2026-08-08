package ru.digitalhustle.certis.model

import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.util.UUID

data class NewAccount(

    val userId: UUID,

    val name: String,

    val type: AccountType,

    val openingBalance: BigDecimal,

    val currency: Currency,
)

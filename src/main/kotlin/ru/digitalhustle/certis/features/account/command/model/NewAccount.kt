package ru.digitalhustle.certis.features.account.command.model

import ru.digitalhustle.certis.features.account.enums.AccountType
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.util.UUID

data class NewAccount(

    val userId: UUID,

    val name: String,

    val type: AccountType,

    val openingBalance: BigDecimal,

    val currency: Currency,
)

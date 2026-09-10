package ru.digitalhustle.certis.features.account.command.model

import ru.digitalhustle.certis.features.account.enums.AccountType
import java.math.BigDecimal
import java.util.UUID

data class UpdateAccountData(

    val id: UUID,

    val userId: UUID,

    val name: String,

    val type: AccountType,

    val openingBalance: BigDecimal,
)

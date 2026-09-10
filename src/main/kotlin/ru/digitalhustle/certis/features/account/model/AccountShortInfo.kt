package ru.digitalhustle.certis.features.account.model

import ru.digitalhustle.certis.features.account.enums.AccountType
import java.util.UUID

data class AccountShortInfo(

    val id: UUID,

    val name: String,

    val type: AccountType,
)

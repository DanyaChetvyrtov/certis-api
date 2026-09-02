package ru.digitalhustle.certis.model.account

import ru.digitalhustle.certis.enums.AccountType
import java.util.UUID

data class AccountShortInfo(

    val id: UUID,

    val name: String,

    val type: AccountType,
)

package ru.digitalhustle.certis.api.dto

import ru.digitalhustle.certis.features.account.enums.AccountType
import java.util.UUID

data class AccountShortInfoDto(

    val id: UUID,

    val name: String,

    val type: AccountType,
)

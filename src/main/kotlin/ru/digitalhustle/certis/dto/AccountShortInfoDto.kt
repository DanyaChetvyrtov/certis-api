package ru.digitalhustle.certis.dto

import ru.digitalhustle.certis.enums.AccountType
import java.util.UUID

data class AccountShortInfoDto(

    val id: UUID,

    val name: String,

    val type: AccountType,
)

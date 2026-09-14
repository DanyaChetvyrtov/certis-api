package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.AccountDto

data class AccountsRs(

    val accounts: List<AccountDto>,
)

package ru.digitalhustle.certis.util.validation

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.service.domain.AccountService
import java.util.UUID

@Component
class AccountValidator(
    private val accountService: AccountService,
) {

    fun validateActiveAccount(accountId: UUID, userId: UUID) {
        val account = accountService.getByIdForShare(accountId, userId)

        if (account.closedAt != null) {
            throw AccountClosedException(ErrorMessages.TRANSACTION_ACCOUNT_CLOSED)
        }
    }
}

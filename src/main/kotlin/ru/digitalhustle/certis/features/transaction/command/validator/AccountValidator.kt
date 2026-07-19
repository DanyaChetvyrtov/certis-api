package ru.digitalhustle.certis.features.transaction.command.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.account.api.AccountCommandAccess
import ru.digitalhustle.certis.features.account.exceptions.AccountClosedException
import ru.digitalhustle.certis.features.transaction.constants.TransactionErrorMessages
import java.util.UUID

@Component
class AccountValidator(
    private val accountService: AccountCommandAccess,
) {

    fun validateActiveAccount(accountId: UUID, userId: UUID) {
        val account = accountService.getByIdForShare(accountId, userId)

        if (account.closedAt != null) {
            throw AccountClosedException(TransactionErrorMessages.TRANSACTION_ACCOUNT_CLOSED)
        }
    }
}

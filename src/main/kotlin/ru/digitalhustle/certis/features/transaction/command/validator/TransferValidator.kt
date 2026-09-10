package ru.digitalhustle.certis.features.transaction.command.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.account.api.AccountSnapshot
import ru.digitalhustle.certis.features.account.exceptions.AccountClosedException
import ru.digitalhustle.certis.features.transaction.command.model.CreateTransferData
import ru.digitalhustle.certis.features.transaction.constants.TransactionErrorMessages
import ru.digitalhustle.certis.features.transaction.exceptions.InvalidTransferException
import ru.digitalhustle.certis.features.transaction.model.Transfer

@Component
class TransferValidator {

    fun validateDifferentAccounts(transfer: CreateTransferData) {
        if (transfer.sourceAccountId == transfer.destinationAccountId) {
            throw InvalidTransferException(TransactionErrorMessages.TRANSFER_SAME_ACCOUNT)
        }
    }

    fun validateActive(account: AccountSnapshot) {
        if (account.closedAt != null) {
            throw AccountClosedException(TransactionErrorMessages.TRANSFER_ACCOUNT_CLOSED)
        }
    }

    fun validateSameCurrency(
        sourceAccount: AccountSnapshot,
        destinationAccount: AccountSnapshot,
    ) {
        if (sourceAccount.currency != destinationAccount.currency) {
            throw InvalidTransferException(TransactionErrorMessages.TRANSFER_CURRENCY_MISMATCH)
        }
    }

    fun validateReversible(transfer: Transfer) {
        if (transfer.reversalOfTransferId != null) {
            throw InvalidTransferException(TransactionErrorMessages.TRANSFER_REVERSAL_OF_REVERSAL)
        }
    }
}

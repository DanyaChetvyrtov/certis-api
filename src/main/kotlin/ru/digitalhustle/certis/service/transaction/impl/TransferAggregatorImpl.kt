package ru.digitalhustle.certis.service.transaction.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.exception.custom.InvalidTransferException
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.transaction.NewTransaction
import ru.digitalhustle.certis.model.transfer.CreateTransferData
import ru.digitalhustle.certis.model.transfer.NewTransfer
import ru.digitalhustle.certis.model.transfer.ReverseTransferData
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.TransactionService
import ru.digitalhustle.certis.service.domain.TransferService
import ru.digitalhustle.certis.service.transaction.TransferAggregator
import java.util.UUID

@Service
class TransferAggregatorImpl(
    private val transferService: TransferService,
    private val accountService: AccountService,
    private val transactionService: TransactionService,
) : TransferAggregator {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Transfer = transferService.getById(id, userId)

    override fun getAllByUserId(userId: UUID): List<Transfer> =
        transferService.getAllByUserId(userId)

    @Transactional
    override fun save(transfer: CreateTransferData): Transfer {
        validateDifferentAccounts(transfer)

        val (sourceAccount, destinationAccount) = getAccounts(
            sourceAccountId = transfer.sourceAccountId,
            destinationAccountId = transfer.destinationAccountId,
            userId = transfer.userId,
        )

        validateActive(sourceAccount)
        validateActive(destinationAccount)
        validateSameCurrency(sourceAccount, destinationAccount)

        return saveWithPostings(
            NewTransfer(
                userId = transfer.userId,
                sourceAccountId = transfer.sourceAccountId,
                destinationAccountId = transfer.destinationAccountId,
                currency = sourceAccount.currency,
                amount = transfer.amount,
                note = transfer.note,
                occurredAt = transfer.occurredAt,
            ),
        )
    }

    @Transactional
    override fun reverse(transfer: ReverseTransferData): Transfer {
        val original = transferService.getByIdForUpdate(transfer.transferId, transfer.userId)

        if (original.reversalOfTransferId != null) {
            throw InvalidTransferException(ErrorMessages.TRANSFER_REVERSAL_OF_REVERSAL)
        }

        transferService.findReversal(original.id, original.userId)?.let { reversal ->
            return reversal
        }

        val (sourceAccount, destinationAccount) = getAccounts(
            sourceAccountId = original.destinationAccountId,
            destinationAccountId = original.sourceAccountId,
            userId = original.userId,
        )

        validateActive(sourceAccount)
        validateActive(destinationAccount)

        return saveWithPostings(
            NewTransfer(
                userId = original.userId,
                sourceAccountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                currency = original.currency,
                amount = original.amount,
                note = transfer.note,
                occurredAt = transfer.occurredAt,
                reversalOfTransferId = original.id,
            ),
        )
    }

    private fun saveWithPostings(transfer: NewTransfer): Transfer {
        val savedTransfer = transferService.save(transfer)

        transactionService.save(createPosting(savedTransfer, TransactionType.EXPENSE))
        transactionService.save(createPosting(savedTransfer, TransactionType.INCOME))

        return savedTransfer
    }

    private fun getAccounts(
        sourceAccountId: UUID,
        destinationAccountId: UUID,
        userId: UUID,
    ): Pair<Account, Account> {
        val accounts = accountService.getAllByIdsForShare(
            ids = listOf(sourceAccountId, destinationAccountId),
            userId = userId,
        ).associateBy(Account::id)

        return accounts.getValue(sourceAccountId) to accounts.getValue(destinationAccountId)
    }

    private fun validateDifferentAccounts(transfer: CreateTransferData) {
        if (transfer.sourceAccountId == transfer.destinationAccountId) {
            throw InvalidTransferException(ErrorMessages.TRANSFER_SAME_ACCOUNT)
        }
    }

    private fun validateActive(account: Account) {
        if (account.closedAt != null) {
            throw AccountClosedException(ErrorMessages.TRANSFER_ACCOUNT_CLOSED)
        }
    }

    private fun validateSameCurrency(
        sourceAccount: Account,
        destinationAccount: Account,
    ) {
        if (sourceAccount.currency != destinationAccount.currency) {
            throw InvalidTransferException(ErrorMessages.TRANSFER_CURRENCY_MISMATCH)
        }
    }

    private fun createPosting(
        transfer: Transfer,
        type: TransactionType,
    ): NewTransaction =
        NewTransaction(
            userId = transfer.userId,
            accountId = when (type) {
                TransactionType.EXPENSE -> transfer.sourceAccountId
                TransactionType.INCOME -> transfer.destinationAccountId
            },
            type = type,
            amount = transfer.amount,
            categoryId = null,
            merchant = null,
            note = transfer.note,
            occurredAt = transfer.occurredAt,
            transferId = transfer.id,
        )
}

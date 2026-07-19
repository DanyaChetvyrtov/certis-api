package ru.digitalhustle.certis.features.transaction.command.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.account.api.AccountCommandAccess
import ru.digitalhustle.certis.features.account.api.AccountSnapshot
import ru.digitalhustle.certis.features.transaction.command.model.CreateTransferData
import ru.digitalhustle.certis.features.transaction.command.model.NewTransaction
import ru.digitalhustle.certis.features.transaction.command.model.NewTransfer
import ru.digitalhustle.certis.features.transaction.command.model.ReverseTransferData
import ru.digitalhustle.certis.features.transaction.command.service.TransactionService
import ru.digitalhustle.certis.features.transaction.command.service.TransferCommandService
import ru.digitalhustle.certis.features.transaction.command.service.TransferService
import ru.digitalhustle.certis.features.transaction.command.validator.TransferValidator
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.model.Transfer
import java.util.UUID

@Service
class TransferCommandServiceImpl(
    private val transferService: TransferService,
    private val accountService: AccountCommandAccess,
    private val transactionService: TransactionService,
    private val transferValidator: TransferValidator,
) : TransferCommandService {

    @Transactional
    override fun save(transfer: CreateTransferData): Transfer {
        transferValidator.validateDifferentAccounts(transfer)

        val (sourceAccount, destinationAccount) = getAccounts(
            sourceAccountId = transfer.sourceAccountId,
            destinationAccountId = transfer.destinationAccountId,
            userId = transfer.userId,
        )

        transferValidator.validateActive(sourceAccount)
        transferValidator.validateActive(destinationAccount)
        transferValidator.validateSameCurrency(sourceAccount, destinationAccount)

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

        transferValidator.validateReversible(original)

        transferService.findReversal(original.id, original.userId)?.let { reversal ->
            return reversal
        }

        val (sourceAccount, destinationAccount) = getAccounts(
            sourceAccountId = original.destinationAccountId,
            destinationAccountId = original.sourceAccountId,
            userId = original.userId,
        )

        transferValidator.validateActive(sourceAccount)
        transferValidator.validateActive(destinationAccount)

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
    ): Pair<AccountSnapshot, AccountSnapshot> {
        val accounts = accountService.getAllByIdsForShare(
            ids = listOf(sourceAccountId, destinationAccountId),
            userId = userId,
        ).associateBy(AccountSnapshot::id)

        return accounts.getValue(sourceAccountId) to accounts.getValue(destinationAccountId)
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

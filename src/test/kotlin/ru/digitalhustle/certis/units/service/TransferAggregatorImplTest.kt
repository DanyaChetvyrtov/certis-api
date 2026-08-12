package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
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
import ru.digitalhustle.certis.service.transaction.impl.TransferAggregatorImpl
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransferAggregatorImplTest {

    private val transferService = mock(TransferService::class.java)
    private val accountService = mock(AccountService::class.java)
    private val transactionService = mock(TransactionService::class.java)
    private val transferAggregator = TransferAggregatorImpl(
        transferService,
        accountService,
        transactionService,
    )

    @Test
    fun `should save transfer and linked debit and credit postings`() {
        // given
        val sourceAccount = createAccount()
        val destinationAccount = createAccount(userId = sourceAccount.userId)
        val createData = createTransferData(sourceAccount, destinationAccount)
        val newTransfer = createNewTransfer(createData, sourceAccount.currency)
        val savedTransfer = createTransfer(newTransfer)

        `when`(
            accountService.getAllByIdsForShare(
                listOf(sourceAccount.id, destinationAccount.id),
                sourceAccount.userId,
            ),
        ).thenReturn(listOf(sourceAccount, destinationAccount))
        `when`(transferService.save(newTransfer)).thenReturn(savedTransfer)

        // when
        val result = transferAggregator.save(createData)

        // then
        assertThat(result).isEqualTo(savedTransfer)
        verify(transactionService).save(createPosting(savedTransfer, TransactionType.EXPENSE))
        verify(transactionService).save(createPosting(savedTransfer, TransactionType.INCOME))
    }

    @Test
    fun `should reject transfer to the same account`() {
        // given
        val account = createAccount()
        val createData = createTransferData(account, account)

        // when, then
        assertThatThrownBy {
            transferAggregator.save(createData)
        }
            .isInstanceOf(InvalidTransferException::class.java)
            .hasMessage(ErrorMessages.TRANSFER_SAME_ACCOUNT)

        verifyNoInteractions(accountService, transferService, transactionService)
    }

    @Test
    fun `should reject transfer involving closed account`() {
        // given
        val sourceAccount = createAccount(closedAt = OffsetDateTime.now())
        val destinationAccount = createAccount(userId = sourceAccount.userId)
        val createData = createTransferData(sourceAccount, destinationAccount)
        `when`(
            accountService.getAllByIdsForShare(
                listOf(sourceAccount.id, destinationAccount.id),
                sourceAccount.userId,
            ),
        ).thenReturn(listOf(sourceAccount, destinationAccount))

        // when, then
        assertThatThrownBy {
            transferAggregator.save(createData)
        }
            .isInstanceOf(AccountClosedException::class.java)
            .hasMessage(ErrorMessages.TRANSFER_ACCOUNT_CLOSED)

        verify(transferService, never()).save(createNewTransfer(createData, sourceAccount.currency))
        verifyNoInteractions(transactionService)
    }

    @Test
    fun `should reject transfer between different currencies`() {
        // given
        val sourceAccount = createAccount(currency = Currency.EUR)
        val destinationAccount = createAccount(userId = sourceAccount.userId, currency = Currency.USD)
        val createData = createTransferData(sourceAccount, destinationAccount)
        `when`(
            accountService.getAllByIdsForShare(
                listOf(sourceAccount.id, destinationAccount.id),
                sourceAccount.userId,
            ),
        ).thenReturn(listOf(sourceAccount, destinationAccount))

        // when, then
        assertThatThrownBy {
            transferAggregator.save(createData)
        }
            .isInstanceOf(InvalidTransferException::class.java)
            .hasMessage(ErrorMessages.TRANSFER_CURRENCY_MISMATCH)

        verifyNoInteractions(transferService, transactionService)
    }

    @Test
    fun `should propagate posting failure for transaction rollback`() {
        // given
        val sourceAccount = createAccount()
        val destinationAccount = createAccount(userId = sourceAccount.userId)
        val createData = createTransferData(sourceAccount, destinationAccount)
        val newTransfer = createNewTransfer(createData, sourceAccount.currency)
        val savedTransfer = createTransfer(newTransfer)
        val incomingPosting = createPosting(savedTransfer, TransactionType.INCOME)

        `when`(
            accountService.getAllByIdsForShare(
                listOf(sourceAccount.id, destinationAccount.id),
                sourceAccount.userId,
            ),
        ).thenReturn(listOf(sourceAccount, destinationAccount))
        `when`(transferService.save(newTransfer)).thenReturn(savedTransfer)
        doThrow(IllegalStateException("posting failed"))
            .`when`(transactionService).save(incomingPosting)

        // when, then
        assertThatThrownBy {
            transferAggregator.save(createData)
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("posting failed")

        verify(transactionService).save(createPosting(savedTransfer, TransactionType.EXPENSE))
        verify(transactionService).save(incomingPosting)
    }

    @Test
    fun `should reverse transfer with linked debit and credit postings`() {
        // given
        val originalSource = createAccount()
        val originalDestination = createAccount(userId = originalSource.userId)
        val originalData = createTransferData(originalSource, originalDestination)
        val original = createTransfer(createNewTransfer(originalData, originalSource.currency))
        val reverseData = createReverseTransferData(original)
        val newReversal = NewTransfer(
            userId = original.userId,
            sourceAccountId = original.destinationAccountId,
            destinationAccountId = original.sourceAccountId,
            currency = original.currency,
            amount = original.amount,
            note = reverseData.note,
            occurredAt = reverseData.occurredAt,
            reversalOfTransferId = original.id,
        )
        val savedReversal = createTransfer(newReversal)

        `when`(transferService.getByIdForUpdate(original.id, original.userId)).thenReturn(original)
        `when`(transferService.findReversal(original.id, original.userId)).thenReturn(null)
        `when`(
            accountService.getAllByIdsForShare(
                listOf(original.destinationAccountId, original.sourceAccountId),
                original.userId,
            ),
        ).thenReturn(listOf(originalDestination, originalSource))
        `when`(transferService.save(newReversal)).thenReturn(savedReversal)

        // when
        val result = transferAggregator.reverse(reverseData)

        // then
        assertThat(result).isEqualTo(savedReversal)
        verify(transactionService).save(createPosting(savedReversal, TransactionType.EXPENSE))
        verify(transactionService).save(createPosting(savedReversal, TransactionType.INCOME))
    }

    @Test
    fun `should return existing reversal without creating duplicate postings`() {
        // given
        val sourceAccount = createAccount()
        val destinationAccount = createAccount(userId = sourceAccount.userId)
        val original = createTransfer(
            createNewTransfer(
                createTransferData(sourceAccount, destinationAccount),
                Currency.EUR,
            ),
        )
        val reversal = original.copy(
            id = UUID.randomUUID(),
            sourceAccountId = original.destinationAccountId,
            destinationAccountId = original.sourceAccountId,
            reversalOfTransferId = original.id,
        )
        val reverseData = createReverseTransferData(original)
        `when`(transferService.getByIdForUpdate(original.id, original.userId)).thenReturn(original)
        `when`(transferService.findReversal(original.id, original.userId)).thenReturn(reversal)

        // when
        val result = transferAggregator.reverse(reverseData)

        // then
        assertThat(result).isEqualTo(reversal)
        verify(transferService).getByIdForUpdate(original.id, original.userId)
        verify(transferService).findReversal(original.id, original.userId)
        verifyNoMoreInteractions(transferService)
        verifyNoInteractions(accountService, transactionService)
    }

    @Test
    fun `should reject reversal of a reversal`() {
        // given
        val sourceAccount = createAccount()
        val destinationAccount = createAccount(userId = sourceAccount.userId)
        val original = createTransfer(
            createNewTransfer(
                createTransferData(sourceAccount, destinationAccount),
                Currency.EUR,
            ),
        ).copy(reversalOfTransferId = UUID.randomUUID())
        val reverseData = createReverseTransferData(original)
        `when`(transferService.getByIdForUpdate(original.id, original.userId)).thenReturn(original)

        // when, then
        assertThatThrownBy {
            transferAggregator.reverse(reverseData)
        }
            .isInstanceOf(InvalidTransferException::class.java)
            .hasMessage(ErrorMessages.TRANSFER_REVERSAL_OF_REVERSAL)

        verify(transferService).getByIdForUpdate(original.id, original.userId)
        verifyNoMoreInteractions(transferService)
        verifyNoInteractions(accountService, transactionService)
    }

    private fun createTransferData(
        sourceAccount: Account,
        destinationAccount: Account,
    ): CreateTransferData =
        CreateTransferData(
            userId = sourceAccount.userId,
            sourceAccountId = sourceAccount.id,
            destinationAccountId = destinationAccount.id,
            amount = BigDecimal("25.50"),
            note = "Move to savings",
            occurredAt = OffsetDateTime.parse("2026-08-16T10:30:00Z"),
        )

    private fun createNewTransfer(
        createData: CreateTransferData,
        currency: Currency,
    ): NewTransfer =
        NewTransfer(
            userId = createData.userId,
            sourceAccountId = createData.sourceAccountId,
            destinationAccountId = createData.destinationAccountId,
            currency = currency,
            amount = createData.amount,
            note = createData.note,
            occurredAt = createData.occurredAt,
        )

    private fun createTransfer(newTransfer: NewTransfer): Transfer =
        Transfer(
            id = UUID.randomUUID(),
            userId = newTransfer.userId,
            sourceAccountId = newTransfer.sourceAccountId,
            destinationAccountId = newTransfer.destinationAccountId,
            currency = newTransfer.currency,
            amount = newTransfer.amount,
            note = newTransfer.note,
            occurredAt = newTransfer.occurredAt,
            createdAt = OffsetDateTime.parse("2026-08-16T12:00:00Z"),
            reversalOfTransferId = newTransfer.reversalOfTransferId,
        )

    private fun createReverseTransferData(transfer: Transfer): ReverseTransferData =
        ReverseTransferData(
            userId = transfer.userId,
            transferId = transfer.id,
            note = "Undo transfer",
            occurredAt = OffsetDateTime.parse("2026-08-16T13:00:00Z"),
        )

    private fun createPosting(
        transfer: Transfer,
        type: TransactionType,
    ): NewTransaction =
        NewTransaction(
            userId = transfer.userId,
            accountId = if (type == TransactionType.EXPENSE) {
                transfer.sourceAccountId
            } else {
                transfer.destinationAccountId
            },
            type = type,
            amount = transfer.amount,
            categoryId = null,
            merchant = null,
            note = transfer.note,
            occurredAt = transfer.occurredAt,
            transferId = transfer.id,
        )

    private fun createAccount(
        userId: UUID = UUID.randomUUID(),
        currency: Currency = Currency.EUR,
        closedAt: OffsetDateTime? = null,
    ): Account =
        Account(
            id = UUID.randomUUID(),
            userId = userId,
            name = "Account",
            type = AccountType.BANK,
            openingBalance = BigDecimal("100.00"),
            currency = currency,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            closedAt = closedAt,
        )
}

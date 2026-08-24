package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.exception.custom.CategoryArchivedException
import ru.digitalhustle.certis.exception.custom.InvalidTransactionException
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.transaction.NewTransaction
import ru.digitalhustle.certis.model.transaction.UpdateTransactionData
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.domain.TransactionService
import ru.digitalhustle.certis.service.transaction.impl.TransactionAggregatorImpl
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransactionAggregatorImplTest {

    private val transactionService = mock(TransactionService::class.java)
    private val accountService = mock(AccountService::class.java)
    private val categoryService = mock(CategoryService::class.java)
    private val transactionAggregator = TransactionAggregatorImpl(
        transactionService,
        accountService,
        categoryService,
    )

    @Test
    fun `should save transaction after locking active account and category`() {
        // given
        val account = createAccount()
        val category = createCategory(userId = account.userId)
        val newTransaction = createNewTransaction(
            userId = account.userId,
            accountId = account.id,
            categoryId = category.id,
        )
        val savedTransaction = createTransaction(
            userId = account.userId,
            accountId = account.id,
            categoryId = category.id,
        )

        `when`(accountService.getByIdForShare(account.id, account.userId))
            .thenReturn(account)
        `when`(categoryService.getByIdForShare(category.id, category.userId))
            .thenReturn(category)
        `when`(transactionService.save(newTransaction))
            .thenReturn(savedTransaction)

        // when
        val result = transactionAggregator.save(newTransaction)

        // then
        assertThat(result).isEqualTo(savedTransaction)
        verify(accountService).getByIdForShare(account.id, account.userId)
        verify(categoryService).getByIdForShare(category.id, category.userId)
    }

    @Test
    fun `should reject transaction for closed account`() {
        // given
        val account = createAccount(closedAt = OffsetDateTime.now())
        val newTransaction = createNewTransaction(
            userId = account.userId,
            accountId = account.id,
        )

        `when`(accountService.getByIdForShare(account.id, account.userId))
            .thenReturn(account)

        // when, then
        assertThatThrownBy {
            transactionAggregator.save(newTransaction)
        }
            .isInstanceOf(AccountClosedException::class.java)
            .hasMessage(ErrorMessages.TRANSACTION_ACCOUNT_CLOSED)

        verify(transactionService, never()).save(newTransaction)
    }

    @Test
    fun `should reject transaction with archived category`() {
        // given
        val account = createAccount()
        val category = createCategory(
            userId = account.userId,
            archivedAt = OffsetDateTime.now(),
        )
        val newTransaction = createNewTransaction(
            userId = account.userId,
            accountId = account.id,
            categoryId = category.id,
        )

        `when`(accountService.getByIdForShare(account.id, account.userId))
            .thenReturn(account)
        `when`(categoryService.getByIdForShare(category.id, category.userId))
            .thenReturn(category)

        // when, then
        assertThatThrownBy {
            transactionAggregator.save(newTransaction)
        }
            .isInstanceOf(CategoryArchivedException::class.java)
            .hasMessage(ErrorMessages.TRANSACTION_CATEGORY_ARCHIVED)
    }

    @Test
    fun `should reject category with different transaction type`() {
        // given
        val account = createAccount()
        val category = createCategory(
            userId = account.userId,
            type = CategoryType.INCOME,
        )
        val newTransaction = createNewTransaction(
            userId = account.userId,
            accountId = account.id,
            categoryId = category.id,
        )

        `when`(accountService.getByIdForShare(account.id, account.userId))
            .thenReturn(account)
        `when`(categoryService.getByIdForShare(category.id, category.userId))
            .thenReturn(category)

        // when, then
        assertThatThrownBy {
            transactionAggregator.save(newTransaction)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(ErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH)
    }

    @Test
    fun `should update transaction on original account with existing archived category`() {
        // given
        val category = createCategory(archivedAt = OffsetDateTime.now())
        val currentTransaction = createTransaction(
            userId = category.userId,
            categoryId = category.id,
        )
        val updateData = createUpdateTransactionData(
            id = currentTransaction.id,
            userId = currentTransaction.userId,
            accountId = currentTransaction.accountId,
            categoryId = category.id,
        )
        val updatedTransaction = currentTransaction.copy(amount = updateData.amount)

        `when`(transactionService.getByIdForUpdate(currentTransaction.id, currentTransaction.userId))
            .thenReturn(currentTransaction)
        `when`(categoryService.getByIdForShare(category.id, category.userId))
            .thenReturn(category)
        `when`(transactionService.update(updateData))
            .thenReturn(updatedTransaction)

        // when
        val result = transactionAggregator.update(updateData)

        // then
        assertThat(result).isEqualTo(updatedTransaction)
        verify(transactionService).getByIdForUpdate(currentTransaction.id, currentTransaction.userId)
        verify(accountService, never())
            .getByIdForShare(currentTransaction.accountId, currentTransaction.userId)
    }

    @Test
    fun `should reject moving transaction to closed account`() {
        // given
        val currentTransaction = createTransaction()
        val closedAccount = createAccount(
            userId = currentTransaction.userId,
            closedAt = OffsetDateTime.now(),
        )
        val updateData = createUpdateTransactionData(
            id = currentTransaction.id,
            userId = currentTransaction.userId,
            accountId = closedAccount.id,
        )

        `when`(transactionService.getByIdForUpdate(currentTransaction.id, currentTransaction.userId))
            .thenReturn(currentTransaction)
        `when`(accountService.getByIdForShare(closedAccount.id, closedAccount.userId))
            .thenReturn(closedAccount)

        // when, then
        assertThatThrownBy {
            transactionAggregator.update(updateData)
        }
            .isInstanceOf(AccountClosedException::class.java)
            .hasMessage(ErrorMessages.TRANSACTION_ACCOUNT_CLOSED)

        verify(transactionService, never()).update(updateData)
    }

    @Test
    fun `should delegate delete`() {
        // given
        val transaction = createTransaction()
        `when`(transactionService.findByIdForUpdate(transaction.id, transaction.userId))
            .thenReturn(transaction)

        // when
        transactionAggregator.delete(transaction.id, transaction.userId)

        // then
        verify(transactionService).findByIdForUpdate(transaction.id, transaction.userId)
        verify(transactionService).delete(transaction.id, transaction.userId)
    }

    @Test
    fun `should reject independent transfer posting update`() {
        // given
        val transaction = createTransaction().copy(transferId = UUID.randomUUID())
        val updateData = createUpdateTransactionData(
            id = transaction.id,
            userId = transaction.userId,
            accountId = transaction.accountId,
        )
        `when`(transactionService.getByIdForUpdate(transaction.id, transaction.userId))
            .thenReturn(transaction)

        // when, then
        assertThatThrownBy {
            transactionAggregator.update(updateData)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(ErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE)

        verify(transactionService, never()).update(updateData)
    }

    @Test
    fun `should reject independent transfer posting deletion`() {
        // given
        val transaction = createTransaction().copy(transferId = UUID.randomUUID())
        `when`(transactionService.findByIdForUpdate(transaction.id, transaction.userId))
            .thenReturn(transaction)

        // when, then
        assertThatThrownBy {
            transactionAggregator.delete(transaction.id, transaction.userId)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(ErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE)

        verify(transactionService, never()).delete(transaction.id, transaction.userId)
    }

    private fun createNewTransaction(
        userId: UUID = UUID.randomUUID(),
        accountId: UUID = UUID.randomUUID(),
        categoryId: UUID? = null,
    ): NewTransaction =
        NewTransaction(
            userId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("42.50"),
            categoryId = categoryId,
            merchant = "Coffee shop",
            note = "Lunch",
            occurredAt = OffsetDateTime.parse("2026-08-07T12:30:00Z"),
        )

    private fun createUpdateTransactionData(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        accountId: UUID = UUID.randomUUID(),
        categoryId: UUID? = null,
    ): UpdateTransactionData =
        UpdateTransactionData(
            id = id,
            userId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("55.00"),
            categoryId = categoryId,
            merchant = "Updated merchant",
            note = "Updated note",
            occurredAt = OffsetDateTime.parse("2026-08-08T12:30:00Z"),
        )

    private fun createAccount(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        closedAt: OffsetDateTime? = null,
    ): Account =
        Account(
            id = id,
            userId = userId,
            name = "Main card",
            type = AccountType.CARD,
            openingBalance = BigDecimal("100.00"),
            currency = Currency.EUR,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            closedAt = closedAt,
        )

    private fun createCategory(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        type: CategoryType = CategoryType.EXPENSE,
        archivedAt: OffsetDateTime? = null,
    ): Category =
        Category(
            id = id,
            userId = userId,
            name = "Category",
            type = type,
            icon = "wallet",
            color = "#10B981",
            archivedAt = archivedAt,
        )

    private fun createTransaction(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        accountId: UUID = UUID.randomUUID(),
        categoryId: UUID? = null,
    ): Transaction =
        Transaction(
            id = id,
            userId = userId,
            accountId = accountId,
            categoryId = categoryId,
            recurringTransactionTemplateId = null,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("42.50"),
            merchant = "Coffee shop",
            note = "Lunch",
            scheduledFor = null,
            occurredAt = OffsetDateTime.parse("2026-08-07T12:30:00Z"),
            createdAt = OffsetDateTime.parse("2026-08-08T18:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-08T18:00:00Z"),
            deletedAt = null,
        )
}

package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.account.api.AccountCommandAccess
import ru.digitalhustle.certis.features.account.api.AccountSnapshot
import ru.digitalhustle.certis.features.account.exceptions.AccountClosedException
import ru.digitalhustle.certis.features.category.api.CategoryCommandAccess
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.exceptions.CategoryArchivedException
import ru.digitalhustle.certis.features.transaction.command.model.AssignTransactionsCategory
import ru.digitalhustle.certis.features.transaction.command.model.NewTransaction
import ru.digitalhustle.certis.features.transaction.command.model.TransactionCategoryAssignment
import ru.digitalhustle.certis.features.transaction.command.model.UpdateTransactionData
import ru.digitalhustle.certis.features.transaction.command.service.TransactionService
import ru.digitalhustle.certis.features.transaction.command.service.impl.TransactionCommandServiceImpl
import ru.digitalhustle.certis.features.transaction.command.validator.AccountValidator
import ru.digitalhustle.certis.features.transaction.command.validator.CategoryValidator
import ru.digitalhustle.certis.features.transaction.command.validator.TransactionValidator
import ru.digitalhustle.certis.features.transaction.constants.TransactionErrorMessages
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.exceptions.InvalidTransactionException
import ru.digitalhustle.certis.features.transaction.model.Transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransactionCommandServiceImplTest {

    private val userId = UUID.randomUUID()
    private val transactionService = mock(TransactionService::class.java)
    private val accountService = mock(AccountCommandAccess::class.java)
    private val categoryService = mock(CategoryCommandAccess::class.java)
    private val transactionCommandService = TransactionCommandServiceImpl(
        transactionService = transactionService,
        categoryService = categoryService,
        accountValidator = AccountValidator(accountService),
        categoryValidator = CategoryValidator(categoryService),
        transactionValidator = TransactionValidator(),
    )

    @Test
    fun `should save transaction after locking active account and category`() {
        // given
        val account = createAccount()
        val category = createCategory()
        val newTransaction = createNewTransaction(
            userId = userId,
            accountId = account.id,
            categoryId = category.id,
        )
        val savedTransaction = createTransaction(
            userId = userId,
            accountId = account.id,
            categoryId = category.id,
        )

        `when`(accountService.getByIdForShare(account.id, userId))
            .thenReturn(account)
        `when`(categoryService.getByIdForShare(category.id, userId))
            .thenReturn(category)
        `when`(transactionService.save(newTransaction))
            .thenReturn(savedTransaction)

        // when
        val result = transactionCommandService.save(newTransaction)

        // then
        assertThat(result).isEqualTo(savedTransaction)
        verify(accountService).getByIdForShare(account.id, userId)
        verify(categoryService).getByIdForShare(category.id, userId)
    }

    @Test
    fun `should reject transaction for closed account`() {
        // given
        val account = createAccount(closedAt = OffsetDateTime.now())
        val newTransaction = createNewTransaction(
            userId = userId,
            accountId = account.id,
        )

        `when`(accountService.getByIdForShare(account.id, userId))
            .thenReturn(account)

        // when, then
        assertThatThrownBy {
            transactionCommandService.save(newTransaction)
        }
            .isInstanceOf(AccountClosedException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_ACCOUNT_CLOSED)

        verify(transactionService, never()).save(newTransaction)
    }

    @Test
    fun `should reject transaction with archived category`() {
        // given
        val account = createAccount()
        val category = createCategory(
            archivedAt = OffsetDateTime.now(),
        )
        val newTransaction = createNewTransaction(
            userId = userId,
            accountId = account.id,
            categoryId = category.id,
        )

        `when`(accountService.getByIdForShare(account.id, userId))
            .thenReturn(account)
        `when`(categoryService.getByIdForShare(category.id, userId))
            .thenReturn(category)

        // when, then
        assertThatThrownBy {
            transactionCommandService.save(newTransaction)
        }
            .isInstanceOf(CategoryArchivedException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_CATEGORY_ARCHIVED)
    }

    @Test
    fun `should reject category with different transaction type`() {
        // given
        val account = createAccount()
        val category = createCategory(
            type = CategoryType.INCOME,
        )
        val newTransaction = createNewTransaction(
            userId = userId,
            accountId = account.id,
            categoryId = category.id,
        )

        `when`(accountService.getByIdForShare(account.id, userId))
            .thenReturn(account)
        `when`(categoryService.getByIdForShare(category.id, userId))
            .thenReturn(category)

        // when, then
        assertThatThrownBy {
            transactionCommandService.save(newTransaction)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH)
    }

    @Test
    fun `should update transaction on original account with existing archived category`() {
        // given
        val category = createCategory(archivedAt = OffsetDateTime.now())
        val currentTransaction = createTransaction(
            userId = userId,
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
        `when`(categoryService.getByIdForShare(category.id, userId))
            .thenReturn(category)
        `when`(transactionService.update(updateData))
            .thenReturn(updatedTransaction)

        // when
        val result = transactionCommandService.update(updateData)

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
            closedAt = OffsetDateTime.now(),
        )
        val updateData = createUpdateTransactionData(
            id = currentTransaction.id,
            userId = currentTransaction.userId,
            accountId = closedAccount.id,
        )

        `when`(transactionService.getByIdForUpdate(currentTransaction.id, currentTransaction.userId))
            .thenReturn(currentTransaction)
        `when`(accountService.getByIdForShare(closedAccount.id, currentTransaction.userId))
            .thenReturn(closedAccount)

        // when, then
        assertThatThrownBy {
            transactionCommandService.update(updateData)
        }
            .isInstanceOf(AccountClosedException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_ACCOUNT_CLOSED)

        verify(transactionService, never()).update(updateData)
    }

    @Test
    fun `should assign different categories to uncategorized transactions`() {
        // given
        val firstCategory = createCategory()
        val secondCategory = createCategory()
        val categories = listOf(firstCategory, secondCategory)
        val transactions = listOf(
            createTransaction(userId = userId),
            createTransaction(userId = userId),
        )
        val assignment = AssignTransactionsCategory(
            userId = userId,
            assignments = transactions.zip(categories).map { (transaction, category) ->
                TransactionCategoryAssignment(transaction.id, category.id)
            },
        )

        `when`(
            categoryService.getAllByIdsForShare(
                categories.mapTo(linkedSetOf(), CategorySnapshot::id),
                assignment.userId,
            ),
        )
            .thenReturn(categories)
        `when`(
            transactionService.getAllByIdsForUpdate(
                assignment.assignments.map(TransactionCategoryAssignment::transactionId),
                assignment.userId,
            ),
        )
            .thenReturn(transactions)

        // when
        transactionCommandService.assignCategories(assignment)

        // then
        verify(transactionService).assignCategories(assignment)
    }

    @Test
    fun `should reject assignment of archived category`() {
        // given
        val category = createCategory(archivedAt = OffsetDateTime.now())
        val assignment = AssignTransactionsCategory(
            userId = userId,
            assignments = listOf(TransactionCategoryAssignment(UUID.randomUUID(), category.id)),
        )

        `when`(categoryService.getAllByIdsForShare(setOf(category.id), userId))
            .thenReturn(listOf(category))

        // when, then
        assertThatThrownBy {
            transactionCommandService.assignCategories(assignment)
        }
            .isInstanceOf(CategoryArchivedException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_CATEGORY_ARCHIVED)

        verify(transactionService, never())
            .getAllByIdsForUpdate(listOf(assignment.assignments.single().transactionId), assignment.userId)
    }

    @Test
    fun `should reject category assignment to categorized transaction`() {
        // given
        val category = createCategory()
        val transaction = createTransaction(
            userId = userId,
            categoryId = UUID.randomUUID(),
        )
        val assignment = AssignTransactionsCategory(
            userId = userId,
            assignments = listOf(TransactionCategoryAssignment(transaction.id, category.id)),
        )

        `when`(categoryService.getAllByIdsForShare(setOf(category.id), userId))
            .thenReturn(listOf(category))
        `when`(transactionService.getAllByIdsForUpdate(listOf(transaction.id), assignment.userId))
            .thenReturn(listOf(transaction))

        // when, then
        assertThatThrownBy {
            transactionCommandService.assignCategories(assignment)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_ALREADY_CATEGORIZED)

        verify(transactionService, never()).assignCategories(assignment)
    }

    @Test
    fun `should reject category assignment with different transaction type`() {
        // given
        val category = createCategory(type = CategoryType.INCOME)
        val transaction = createTransaction(userId = userId)
        val assignment = AssignTransactionsCategory(
            userId = userId,
            assignments = listOf(TransactionCategoryAssignment(transaction.id, category.id)),
        )

        `when`(categoryService.getAllByIdsForShare(setOf(category.id), userId))
            .thenReturn(listOf(category))
        `when`(transactionService.getAllByIdsForUpdate(listOf(transaction.id), assignment.userId))
            .thenReturn(listOf(transaction))

        // when, then
        assertThatThrownBy {
            transactionCommandService.assignCategories(assignment)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH)

        verify(transactionService, never()).assignCategories(assignment)
    }

    @Test
    fun `should reject duplicate transaction assignments before locking resources`() {
        // given
        val transactionId = UUID.randomUUID()
        val assignment = AssignTransactionsCategory(
            userId = UUID.randomUUID(),
            assignments = listOf(
                TransactionCategoryAssignment(transactionId, UUID.randomUUID()),
                TransactionCategoryAssignment(transactionId, UUID.randomUUID()),
            ),
        )

        // when, then
        assertThatThrownBy {
            transactionCommandService.assignCategories(assignment)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSACTION_DUPLICATE_CATEGORY_ASSIGNMENTS)

        verifyNoInteractions(categoryService)
        verify(transactionService, never()).assignCategories(assignment)
    }

    @Test
    fun `should delegate delete`() {
        // given
        val transaction = createTransaction()
        `when`(transactionService.findByIdForUpdate(transaction.id, transaction.userId))
            .thenReturn(transaction)

        // when
        transactionCommandService.delete(transaction.id, transaction.userId)

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
            transactionCommandService.update(updateData)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE)

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
            transactionCommandService.delete(transaction.id, transaction.userId)
        }
            .isInstanceOf(InvalidTransactionException::class.java)
            .hasMessage(TransactionErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE)

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
        closedAt: OffsetDateTime? = null,
    ): AccountSnapshot =
        AccountSnapshot(
            id = id,
            currency = Currency.EUR,
            closedAt = closedAt,
        )

    private fun createCategory(
        id: UUID = UUID.randomUUID(),
        type: CategoryType = CategoryType.EXPENSE,
        archivedAt: OffsetDateTime? = null,
    ): CategorySnapshot =
        CategorySnapshot(
            id = id,
            type = type,
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

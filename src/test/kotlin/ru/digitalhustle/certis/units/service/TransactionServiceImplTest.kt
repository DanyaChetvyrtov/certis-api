package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.transaction.AssignTransactionsCategory
import ru.digitalhustle.certis.model.transaction.NewTransaction
import ru.digitalhustle.certis.model.transaction.TransactionCategoryAssignment
import ru.digitalhustle.certis.model.transaction.TransactionFilter
import ru.digitalhustle.certis.model.transaction.TransactionPage
import ru.digitalhustle.certis.model.transaction.UpdateTransactionData
import ru.digitalhustle.certis.repository.TransactionRepository
import ru.digitalhustle.certis.service.domain.impl.TransactionServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class TransactionServiceImplTest {

    private val transactionRepository = mock(TransactionRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-08T19:00:00Z"), ZoneOffset.UTC)
    private val transactionService = TransactionServiceImpl(transactionRepository, clock)

    private companion object {
        private val AMOUNT = BigDecimal("42.50")
        private val TRANSACTION_DATE = OffsetDateTime.parse("2026-08-07T12:30:00Z")
    }

    @Test
    fun `should get transaction owned by user`() {
        // given
        val transaction = createTransaction()

        `when`(transactionRepository.findByIdAndUserId(transaction.id, transaction.userId))
            .thenReturn(transaction)

        // when
        val result = transactionService.getById(transaction.id, transaction.userId)

        // then
        assertThat(result).isEqualTo(transaction)
    }

    @Test
    fun `should throw not found for transaction owned by another user`() {
        // given
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(transactionRepository.findByIdAndUserId(transactionId, userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            transactionService.getById(transactionId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should lock transaction owned by user for update`() {
        // given
        val transaction = createTransaction()

        `when`(transactionRepository.findByIdAndUserIdForUpdate(transaction.id, transaction.userId))
            .thenReturn(transaction)

        // when
        val result = transactionService.getByIdForUpdate(transaction.id, transaction.userId)

        // then
        assertThat(result).isEqualTo(transaction)
    }

    @Test
    fun `should throw not found when locking missing transaction for update`() {
        // given
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(transactionRepository.findByIdAndUserIdForUpdate(transactionId, userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            transactionService.getByIdForUpdate(transactionId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should return null when optionally locking missing transaction for update`() {
        // given
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        `when`(transactionRepository.findByIdAndUserIdForUpdate(transactionId, userId))
            .thenReturn(null)

        // when
        val result = transactionService.findByIdForUpdate(transactionId, userId)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should get filtered transaction page`() {
        // given
        val userId = UUID.randomUUID()
        val filter = createFilter()
        val page = TransactionPage(
            items = listOf(createTransaction(userId = userId)),
            page = filter.page,
            size = filter.size,
            totalElements = 1,
        )

        `when`(transactionRepository.findAllByUserId(userId, filter))
            .thenReturn(page)

        // when
        val result = transactionService.getAllByUserId(userId, filter)

        // then
        assertThat(result).isEqualTo(page)
    }

    @Test
    fun `should lock all requested transactions`() {
        // given
        val userId = UUID.randomUUID()
        val transactions = listOf(
            createTransaction(userId = userId),
            createTransaction(userId = userId),
        )
        val ids = transactions.map(Transaction::id)

        `when`(transactionRepository.findAllByIdsAndUserIdForUpdate(ids, userId))
            .thenReturn(transactions)

        // when
        val result = transactionService.getAllByIdsForUpdate(ids, userId)

        // then
        assertThat(result).isEqualTo(transactions)
    }

    @Test
    fun `should reject batch when any requested transaction is unavailable`() {
        // given
        val userId = UUID.randomUUID()
        val availableTransaction = createTransaction(userId = userId)
        val ids = listOf(availableTransaction.id, UUID.randomUUID())

        `when`(transactionRepository.findAllByIdsAndUserIdForUpdate(ids, userId))
            .thenReturn(listOf(availableTransaction))

        // when, then
        assertThatThrownBy {
            transactionService.getAllByIdsForUpdate(ids, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should save transaction`() {
        // given
        val newTransaction = createNewTransaction()
        val transactionCaptor = ArgumentCaptor.forClass(Transaction::class.java)

        `when`(transactionRepository.insert(captureTransaction(transactionCaptor)))
            .thenAnswer { transactionCaptor.value }

        // when
        val result = transactionService.save(newTransaction)

        // then
        assertAll(
            { assertThat(result.amount).isEqualByComparingTo(AMOUNT) },
            { assertThat(result.createdAt).isEqualTo(OffsetDateTime.now(clock)) },
            { assertThat(result.updatedAt).isEqualTo(OffsetDateTime.now(clock)) },
            { assertThat(result.recurringTransactionTemplateId).isNull() },
            { assertThat(result.scheduledFor).isNull() },
            { assertThat(result.deletedAt).isNull() },
        )
    }

    @Test
    fun `should save idempotent transaction occurrence from recurring template`() {
        // given
        val template = createRecurringTemplate()
        val scheduledFor = LocalDate.parse("2026-08-08")
        val transactionCaptor = ArgumentCaptor.forClass(Transaction::class.java)

        `when`(transactionRepository.insertIgnoringConflict(captureTransaction(transactionCaptor)))
            .thenAnswer { transactionCaptor.value }

        // when
        val result = checkNotNull(transactionService.saveScheduled(template, scheduledFor))

        // then
        assertAll(
            { assertThat(result.recurringTransactionTemplateId).isEqualTo(template.id) },
            { assertThat(result.scheduledFor).isEqualTo(scheduledFor) },
            { assertThat(result.occurredAt).isEqualTo(OffsetDateTime.parse("2026-08-08T00:00:00Z")) },
            { assertThat(result.amount).isEqualByComparingTo(template.amount) },
            { assertThat(result.createdAt).isEqualTo(OffsetDateTime.now(clock)) },
        )
    }

    @Test
    fun `should return existing recurring occurrence after insert conflict`() {
        // given
        val template = createRecurringTemplate()
        val scheduledFor = LocalDate.parse("2026-08-08")
        val transactionCaptor = ArgumentCaptor.forClass(Transaction::class.java)
        val existingTransaction = createTransaction(
            userId = template.userId,
            accountId = template.accountId,
        ).copy(
            recurringTransactionTemplateId = template.id,
            scheduledFor = scheduledFor,
        )

        `when`(transactionRepository.insertIgnoringConflict(captureTransaction(transactionCaptor)))
            .thenReturn(null)
        `when`(
            transactionRepository.findByRecurringTemplateIdAndScheduledFor(template.id, scheduledFor),
        ).thenReturn(existingTransaction)

        // when
        val result = transactionService.saveScheduled(template, scheduledFor)

        // then
        assertThat(result).isEqualTo(existingTransaction)
    }

    @Test
    fun `should update active transaction`() {
        // given
        val updateData = createUpdateTransactionData()
        val updatedTransaction = createTransaction(
            id = updateData.id,
            userId = updateData.userId,
            accountId = updateData.accountId,
        )

        `when`(transactionRepository.updateActive(updateData, OffsetDateTime.now(clock)))
            .thenReturn(updatedTransaction)

        // when
        val result = transactionService.update(updateData)

        // then
        assertThat(result).isEqualTo(updatedTransaction)
        verify(transactionRepository).updateActive(updateData, OffsetDateTime.now(clock))
    }

    @Test
    fun `should throw not found when updating missing transaction`() {
        // given
        val updateData = createUpdateTransactionData()

        `when`(transactionRepository.updateActive(updateData, OffsetDateTime.now(clock)))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            transactionService.update(updateData)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should assign categories to requested transactions`() {
        // given
        val assignment = AssignTransactionsCategory(
            userId = UUID.randomUUID(),
            assignments = listOf(
                TransactionCategoryAssignment(UUID.randomUUID(), UUID.randomUUID()),
                TransactionCategoryAssignment(UUID.randomUUID(), UUID.randomUUID()),
            ),
        )

        `when`(
            transactionRepository.assignCategories(
                assignment.assignments,
                assignment.userId,
                OffsetDateTime.now(clock),
            ),
        ).thenReturn(assignment.assignments.size)

        // when
        transactionService.assignCategories(assignment)

        // then
        verify(transactionRepository).assignCategories(
            assignment.assignments,
            assignment.userId,
            OffsetDateTime.now(clock),
        )
    }

    @Test
    fun `should fail when category assignment batch is incomplete`() {
        // given
        val assignment = AssignTransactionsCategory(
            userId = UUID.randomUUID(),
            assignments = listOf(
                TransactionCategoryAssignment(UUID.randomUUID(), UUID.randomUUID()),
                TransactionCategoryAssignment(UUID.randomUUID(), UUID.randomUUID()),
            ),
        )

        `when`(
            transactionRepository.assignCategories(
                assignment.assignments,
                assignment.userId,
                OffsetDateTime.now(clock),
            ),
        ).thenReturn(1)

        // when, then
        assertThatThrownBy {
            transactionService.assignCategories(assignment)
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage(ErrorMessages.TRANSACTION_CATEGORY_ASSIGNMENT_FAILED)
    }

    @Test
    fun `should soft delete transaction`() {
        // given
        val transaction = createTransaction()

        `when`(transactionRepository.softDelete(transaction.id, transaction.userId, OffsetDateTime.now(clock)))
            .thenReturn(true)

        // when
        transactionService.delete(transaction.id, transaction.userId)

        // then
        verify(transactionRepository).softDelete(transaction.id, transaction.userId, OffsetDateTime.now(clock))
        verify(transactionRepository, never()).existsIncludingDeletedByIdAndUserId(transaction.id, transaction.userId)
    }

    @Test
    fun `should keep repeated soft delete idempotent`() {
        // given
        val transaction = createTransaction()

        `when`(transactionRepository.softDelete(transaction.id, transaction.userId, OffsetDateTime.now(clock)))
            .thenReturn(false)
        `when`(transactionRepository.existsIncludingDeletedByIdAndUserId(transaction.id, transaction.userId))
            .thenReturn(true)

        // when
        transactionService.delete(transaction.id, transaction.userId)

        // then
        verify(transactionRepository).existsIncludingDeletedByIdAndUserId(transaction.id, transaction.userId)
    }

    @Test
    fun `should throw not found when deleting missing transaction`() {
        // given
        val transactionId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(transactionRepository.softDelete(transactionId, userId, OffsetDateTime.now(clock)))
            .thenReturn(false)
        `when`(transactionRepository.existsIncludingDeletedByIdAndUserId(transactionId, userId))
            .thenReturn(false)

        // when, then
        assertThatThrownBy {
            transactionService.delete(transactionId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    private fun createFilter(): TransactionFilter =
        TransactionFilter(
            accountId = null,
            categoryId = null,
            type = null,
            from = null,
            to = null,
            page = 0,
            size = 20,
        )

    private fun createNewTransaction(
        userId: UUID = UUID.randomUUID(),
        accountId: UUID = UUID.randomUUID(),
        categoryId: UUID? = null,
    ): NewTransaction =
        NewTransaction(
            userId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = AMOUNT,
            categoryId = categoryId,
            merchant = "Coffee shop",
            note = "Lunch",
            occurredAt = TRANSACTION_DATE,
        )

    private fun createUpdateTransactionData(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        accountId: UUID = UUID.randomUUID(),
    ): UpdateTransactionData =
        UpdateTransactionData(
            id = id,
            userId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("55.00"),
            categoryId = null,
            merchant = "Updated merchant",
            note = "Updated note",
            occurredAt = TRANSACTION_DATE.plusDays(1),
        )

    private fun createTransaction(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        accountId: UUID = UUID.randomUUID(),
    ): Transaction =
        Transaction(
            id = id,
            userId = userId,
            accountId = accountId,
            categoryId = null,
            recurringTransactionTemplateId = null,
            type = TransactionType.EXPENSE,
            amount = AMOUNT,
            merchant = "Coffee shop",
            note = "Lunch",
            scheduledFor = null,
            occurredAt = TRANSACTION_DATE,
            createdAt = OffsetDateTime.parse("2026-08-08T18:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-08T18:00:00Z"),
            deletedAt = null,
        )

    private fun createRecurringTemplate(): RecurringTransactionTemplate =
        RecurringTransactionTemplate(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            categoryId = null,
            name = "Subscription",
            type = TransactionType.EXPENSE,
            amount = AMOUNT,
            merchant = "Streaming service",
            note = null,
            status = RecurringTransactionTemplateStatus.ACTIVE,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
            startDate = LocalDate.parse("2026-08-08"),
            endDate = null,
            lastRunDate = null,
            nextRunDate = LocalDate.parse("2026-08-08"),
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
        )

    private fun captureTransaction(captor: ArgumentCaptor<Transaction>): Transaction {
        captor.capture()
        return createTransaction()
    }
}

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
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.NewTransaction
import ru.digitalhustle.certis.model.TransactionFilter
import ru.digitalhustle.certis.model.TransactionPage
import ru.digitalhustle.certis.model.UpdateTransactionData
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.repository.TransactionRepository
import ru.digitalhustle.certis.service.domain.impl.TransactionServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
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
            { assertThat(result.recurringTransactionId).isNull() },
            { assertThat(result.deletedAt).isNull() },
        )
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

        `when`(transactionRepository.updateActive(updateData))
            .thenReturn(updatedTransaction)

        // when
        val result = transactionService.update(updateData)

        // then
        assertThat(result).isEqualTo(updatedTransaction)
    }

    @Test
    fun `should throw not found when updating missing transaction`() {
        // given
        val updateData = createUpdateTransactionData()

        `when`(transactionRepository.updateActive(updateData))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            transactionService.update(updateData)
        }.isInstanceOf(NotFoundException::class.java)
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
            date = TRANSACTION_DATE,
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
            date = TRANSACTION_DATE.plusDays(1),
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
            type = TransactionType.EXPENSE,
            amount = AMOUNT,
            categoryId = null,
            merchant = "Coffee shop",
            note = "Lunch",
            date = TRANSACTION_DATE,
            createdAt = OffsetDateTime.parse("2026-08-08T18:00:00Z"),
            recurringTransactionId = null,
            deletedAt = null,
        )

    private fun captureTransaction(captor: ArgumentCaptor<Transaction>): Transaction {
        captor.capture()
        return createTransaction()
    }
}

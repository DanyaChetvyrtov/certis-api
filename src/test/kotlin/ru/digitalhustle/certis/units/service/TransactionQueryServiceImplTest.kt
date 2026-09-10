package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.features.transaction.query.model.TransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.TransactionPage
import ru.digitalhustle.certis.features.transaction.query.repository.TransactionQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.impl.TransactionQueryServiceImpl
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransactionQueryServiceImplTest {

    private val transactionRepository = mock(TransactionQueryRepository::class.java)
    private val transactionService = TransactionQueryServiceImpl(transactionRepository)

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
}

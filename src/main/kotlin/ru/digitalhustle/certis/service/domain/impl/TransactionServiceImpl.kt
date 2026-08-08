package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.NewTransaction
import ru.digitalhustle.certis.model.TransactionFilter
import ru.digitalhustle.certis.model.TransactionPage
import ru.digitalhustle.certis.model.UpdateTransactionData
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.repository.TransactionRepository
import ru.digitalhustle.certis.service.domain.TransactionService
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TransactionServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) : TransactionService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Transaction =
        transactionRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Transaction")

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Transaction =
        transactionRepository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw NotFoundException.entity("Transaction")

    override fun getAllByUserId(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionPage = transactionRepository.findAllByUserId(userId, filter)

    override fun save(newTransaction: NewTransaction): Transaction {
        val now = OffsetDateTime.now(clock)

        return transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = newTransaction.userId,
                accountId = newTransaction.accountId,
                type = newTransaction.type,
                amount = newTransaction.amount,
                categoryId = newTransaction.categoryId,
                merchant = newTransaction.merchant,
                note = newTransaction.note,
                date = newTransaction.date,
                createdAt = now,
                recurringTransactionId = null,
                deletedAt = null,
            ),
        )
    }

    override fun update(transaction: UpdateTransactionData): Transaction =
        transactionRepository.updateActive(transaction)
            ?: throw NotFoundException.entity("Transaction")

    override fun delete(
        id: UUID,
        userId: UUID,
    ) {
        val deleted = transactionRepository.softDelete(
            id = id,
            userId = userId,
            deletedAt = OffsetDateTime.now(clock),
        )

        if (!deleted && !transactionRepository.existsIncludingDeletedByIdAndUserId(id, userId)) {
            throw NotFoundException.entity("Transaction")
        }
    }
}

package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.transaction.AssignTransactionsCategory
import ru.digitalhustle.certis.model.transaction.NewTransaction
import ru.digitalhustle.certis.model.transaction.TransactionFilter
import ru.digitalhustle.certis.model.transaction.TransactionPage
import ru.digitalhustle.certis.model.transaction.UpdateTransactionData
import ru.digitalhustle.certis.repository.TransactionRepository
import ru.digitalhustle.certis.service.domain.TransactionService
import java.time.Clock
import java.time.LocalDate
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
        findByIdForUpdate(id, userId)
            ?: throw NotFoundException.entity("Transaction")

    override fun findByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Transaction? = transactionRepository.findByIdAndUserIdForUpdate(id, userId)

    override fun getAllByUserId(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionPage = transactionRepository.findAllByUserId(userId, filter)

    override fun getAllByIdsForUpdate(
        ids: Collection<UUID>,
        userId: UUID,
    ): List<Transaction> {
        val transactions = transactionRepository.findAllByIdsAndUserIdForUpdate(ids, userId)

        if (transactions.size != ids.size) {
            throw NotFoundException.entity("Transaction")
        }

        return transactions
    }

    override fun save(newTransaction: NewTransaction): Transaction {
        val now = OffsetDateTime.now(clock)

        return transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = newTransaction.userId,
                accountId = newTransaction.accountId,
                categoryId = newTransaction.categoryId,
                recurringTransactionTemplateId = null,
                type = newTransaction.type,
                amount = newTransaction.amount,
                merchant = newTransaction.merchant,
                note = newTransaction.note,
                scheduledFor = null,
                occurredAt = newTransaction.occurredAt,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                transferId = newTransaction.transferId,
            ),
        )
    }

    override fun saveScheduled(
        template: RecurringTransactionTemplate,
        scheduledFor: LocalDate,
    ): Transaction {
        val now = OffsetDateTime.now(clock)
        val transaction = Transaction(
            id = UUID.randomUUID(),
            userId = template.userId,
            accountId = template.accountId,
            categoryId = template.categoryId,
            recurringTransactionTemplateId = template.id,
            type = template.type,
            amount = template.amount,
            merchant = template.merchant,
            note = template.note,
            scheduledFor = scheduledFor,
            occurredAt = scheduledFor.atStartOfDay(clock.zone).toOffsetDateTime(),
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            transferId = null,
        )

        return transactionRepository.insertIgnoringConflict(transaction)
            ?: transactionRepository.findByRecurringTemplateIdAndScheduledFor(template.id, scheduledFor)
            ?: error("Scheduled transaction insert conflicted without an existing recurring occurrence")
    }

    override fun update(transaction: UpdateTransactionData): Transaction =
        transactionRepository.updateActive(
            transaction = transaction,
            updatedAt = OffsetDateTime.now(clock),
        )
            ?: throw NotFoundException.entity("Transaction")

    override fun assignCategories(
        assignment: AssignTransactionsCategory,
    ) {
        val assignedCount = transactionRepository.assignCategories(
            assignments = assignment.assignments,
            userId = assignment.userId,
            updatedAt = OffsetDateTime.now(clock),
        )

        check(assignedCount == assignment.assignments.size) {
            ErrorMessages.TRANSACTION_CATEGORY_ASSIGNMENT_FAILED
        }
    }

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

package ru.digitalhustle.certis.features.transaction.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.command.model.AssignTransactionsCategory
import ru.digitalhustle.certis.features.transaction.command.model.NewTransaction
import ru.digitalhustle.certis.features.transaction.command.model.UpdateTransactionData
import ru.digitalhustle.certis.features.transaction.command.repository.TransactionRepository
import ru.digitalhustle.certis.features.transaction.command.service.TransactionService
import ru.digitalhustle.certis.features.transaction.constants.TransactionErrorMessages
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.LocalDate
import java.util.UUID

@Service
class TransactionServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val applicationClock: ApplicationClock,
) : TransactionService {

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
        val now = applicationClock.now()

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
        val now = applicationClock.now()
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
            occurredAt = applicationClock.startOfDay(scheduledFor),
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
            updatedAt = applicationClock.now(),
        )
            ?: throw NotFoundException.entity("Transaction")

    override fun assignCategories(
        assignment: AssignTransactionsCategory,
    ) {
        val assignedCount = transactionRepository.assignCategories(
            assignments = assignment.assignments,
            userId = assignment.userId,
            updatedAt = applicationClock.now(),
        )

        check(assignedCount == assignment.assignments.size) {
            TransactionErrorMessages.TRANSACTION_CATEGORY_ASSIGNMENT_FAILED
        }
    }

    override fun delete(
        id: UUID,
        userId: UUID,
    ) {
        val deleted = transactionRepository.softDelete(
            id = id,
            userId = userId,
            deletedAt = applicationClock.now(),
        )

        if (!deleted && !transactionRepository.existsIncludingDeletedByIdAndUserId(id, userId)) {
            throw NotFoundException.entity("Transaction")
        }
    }
}

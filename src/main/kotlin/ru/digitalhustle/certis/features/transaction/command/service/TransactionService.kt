package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.AssignTransactionsCategory
import ru.digitalhustle.certis.features.transaction.command.model.NewTransaction
import ru.digitalhustle.certis.features.transaction.command.model.UpdateTransactionData
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.model.Transaction
import java.time.LocalDate
import java.util.UUID

interface TransactionService {

    fun getByIdForUpdate(id: UUID, userId: UUID): Transaction

    fun findByIdForUpdate(id: UUID, userId: UUID): Transaction?

    fun getAllByIdsForUpdate(ids: Collection<UUID>, userId: UUID): List<Transaction>

    fun save(newTransaction: NewTransaction): Transaction

    fun saveScheduled(
        template: RecurringTransactionTemplate,
        scheduledFor: LocalDate,
    ): Transaction

    fun update(transaction: UpdateTransactionData): Transaction

    fun assignCategories(assignment: AssignTransactionsCategory)

    fun delete(id: UUID, userId: UUID)
}

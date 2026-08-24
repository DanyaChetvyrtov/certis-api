package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.transaction.NewTransaction
import ru.digitalhustle.certis.model.transaction.TransactionFilter
import ru.digitalhustle.certis.model.transaction.TransactionPage
import ru.digitalhustle.certis.model.transaction.UpdateTransactionData
import java.time.LocalDate
import java.util.UUID

interface TransactionService {

    fun getById(id: UUID, userId: UUID): Transaction

    fun getByIdForUpdate(id: UUID, userId: UUID): Transaction

    fun findByIdForUpdate(id: UUID, userId: UUID): Transaction?

    fun getAllByUserId(userId: UUID, filter: TransactionFilter): TransactionPage

    fun save(newTransaction: NewTransaction): Transaction

    fun saveScheduled(
        template: RecurringTransactionTemplate,
        scheduledFor: LocalDate,
    ): Transaction

    fun update(transaction: UpdateTransactionData): Transaction

    fun delete(id: UUID, userId: UUID)
}

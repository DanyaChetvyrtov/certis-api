package ru.digitalhustle.certis.service.transaction

import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.transaction.AssignTransactionsCategory
import ru.digitalhustle.certis.model.transaction.NewTransaction
import ru.digitalhustle.certis.model.transaction.TransactionFilter
import ru.digitalhustle.certis.model.transaction.TransactionPage
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import ru.digitalhustle.certis.model.transaction.UpdateTransactionData
import java.util.UUID

interface TransactionAggregator {

    fun getAllByUserId(userId: UUID, filter: TransactionFilter): TransactionPage

    fun getUncategorizedByUserId(userId: UUID, filter: UncategorizedTransactionFilter): UncategorizedTransactionPage

    fun getById(id: UUID, userId: UUID): Transaction

    fun save(transaction: NewTransaction): Transaction

    fun update(transaction: UpdateTransactionData): Transaction

    fun assignCategories(assignment: AssignTransactionsCategory)

    fun delete(id: UUID, userId: UUID)
}

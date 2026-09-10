package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.AssignTransactionsCategory
import ru.digitalhustle.certis.features.transaction.command.model.NewTransaction
import ru.digitalhustle.certis.features.transaction.command.model.UpdateTransactionData
import ru.digitalhustle.certis.features.transaction.model.Transaction
import java.util.UUID

interface TransactionCommandService {

    fun save(transaction: NewTransaction): Transaction

    fun update(transaction: UpdateTransactionData): Transaction

    fun assignCategories(assignment: AssignTransactionsCategory)

    fun delete(id: UUID, userId: UUID)
}

package ru.digitalhustle.certis.service.aggregation

import ru.digitalhustle.certis.model.NewTransaction
import ru.digitalhustle.certis.model.TransactionFilter
import ru.digitalhustle.certis.model.TransactionPage
import ru.digitalhustle.certis.model.UpdateTransactionData
import ru.digitalhustle.certis.model.entity.Transaction
import java.util.UUID

interface TransactionAggregator {

    fun getById(id: UUID, userId: UUID): Transaction

    fun getAllByUserId(userId: UUID, filter: TransactionFilter): TransactionPage

    fun save(transaction: NewTransaction): Transaction

    fun update(transaction: UpdateTransactionData): Transaction

    fun delete(id: UUID, userId: UUID)
}

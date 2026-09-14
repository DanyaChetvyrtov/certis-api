package ru.digitalhustle.certis.features.transaction.query.service

import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.features.transaction.query.model.TransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.TransactionPage
import java.util.UUID

interface TransactionQueryService {

    fun getById(id: UUID, userId: UUID): Transaction

    fun getAllByUserId(userId: UUID, filter: TransactionFilter): TransactionPage
}

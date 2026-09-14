package ru.digitalhustle.certis.features.transaction.query.service

import ru.digitalhustle.certis.features.transaction.query.model.UncategorizedTransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.UncategorizedTransactionPage
import java.util.UUID

interface UncategorizedTransactionService {

    fun getAllByUserId(
        userId: UUID,
        filter: UncategorizedTransactionFilter,
    ): UncategorizedTransactionPage
}

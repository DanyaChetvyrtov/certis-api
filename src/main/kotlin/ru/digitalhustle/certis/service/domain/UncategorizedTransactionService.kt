package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import java.util.UUID

interface UncategorizedTransactionService {

    fun getAllByUserId(
        userId: UUID,
        filter: UncategorizedTransactionFilter,
    ): UncategorizedTransactionPage
}

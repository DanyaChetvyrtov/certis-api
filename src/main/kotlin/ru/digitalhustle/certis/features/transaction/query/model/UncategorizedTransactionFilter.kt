package ru.digitalhustle.certis.features.transaction.query.model

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import java.time.YearMonth
import java.util.UUID

data class UncategorizedTransactionFilter(

    val month: YearMonth,

    val currency: Currency,

    val type: TransactionType,

    val accountId: UUID?,

    val search: String?,

    val page: Int,

    val size: Int,
)

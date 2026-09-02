package ru.digitalhustle.certis.model.transaction

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
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

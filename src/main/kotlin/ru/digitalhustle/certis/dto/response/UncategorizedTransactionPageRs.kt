package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.dto.UncategorizedTransactionDto
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import java.time.YearMonth

data class UncategorizedTransactionPageRs(

    val month: YearMonth,

    val currency: Currency,

    val type: TransactionType,

    val items: List<UncategorizedTransactionDto>,

    val page: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,
)

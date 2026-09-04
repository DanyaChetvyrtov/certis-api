package ru.digitalhustle.certis.model.transaction

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import java.time.YearMonth

data class UncategorizedTransactionPage(

    val month: YearMonth,

    val currency: Currency,

    val type: TransactionType,

    val items: List<UncategorizedTransaction>,

    val page: Int,

    val size: Int,

    val totalElements: Long,
) {

    val totalPages: Int =
        if (totalElements == 0L) {
            0
        } else {
            ((totalElements - 1) / size + 1).toInt()
        }
}

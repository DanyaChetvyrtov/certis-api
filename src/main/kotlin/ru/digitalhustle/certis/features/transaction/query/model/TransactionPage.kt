package ru.digitalhustle.certis.features.transaction.query.model

import ru.digitalhustle.certis.features.transaction.model.Transaction

data class TransactionPage(

    val items: List<Transaction>,

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

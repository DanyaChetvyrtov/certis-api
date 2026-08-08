package ru.digitalhustle.certis.model

import ru.digitalhustle.certis.model.entity.Transaction

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

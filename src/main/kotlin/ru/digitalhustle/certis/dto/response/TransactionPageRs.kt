package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.dto.TransactionDto

data class TransactionPageRs(

    val items: List<TransactionDto>,

    val page: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,
)

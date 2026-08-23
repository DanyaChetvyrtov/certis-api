package ru.digitalhustle.certis.model.transaction

import ru.digitalhustle.certis.enums.TransactionType
import java.time.OffsetDateTime
import java.util.UUID

data class TransactionFilter(

    val accountId: UUID?,

    val categoryId: UUID?,

    val type: TransactionType?,

    val from: OffsetDateTime?,

    val to: OffsetDateTime?,

    val page: Int,

    val size: Int,
)

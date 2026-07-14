package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.TransactionType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Transaction(

    val id: UUID,

    val userId: UUID,

    val accountId: UUID,

    val type: TransactionType,

    val amount: BigDecimal,

    val categoryId: UUID?,

    val merchant: String?,

    val note: String?,

    val date: OffsetDateTime,

    val createdAt: OffsetDateTime,

    val recurringTransactionId: UUID?,
)

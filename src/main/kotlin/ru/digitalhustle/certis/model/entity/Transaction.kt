package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Transaction(

    val id: UUID,

    val userId: UUID,

    val accountId: UUID,

    val categoryId: UUID?,

    val recurringTransactionTemplateId: UUID?,

    val type: TransactionType,

    val amount: BigDecimal,

    val merchant: String?,

    val note: String?,

    val scheduledFor: LocalDate?,

    val occurredAt: OffsetDateTime,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,

    val deletedAt: OffsetDateTime?,

    val transferId: UUID? = null,
)

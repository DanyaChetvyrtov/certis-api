package ru.digitalhustle.certis.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransactionDto(

    val id: UUID,

    val accountId: UUID,

    val type: TransactionType,

    val amount: BigDecimal,

    val categoryId: UUID?,

    val merchant: String?,

    val note: String?,

    val recurringTransactionTemplateId: UUID?,

    val scheduledFor: LocalDate?,

    val occurredAt: OffsetDateTime,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,

    val transferId: UUID?,
)

package ru.digitalhustle.certis.features.transaction.model

import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class RecurringTransactionTemplate(

    val id: UUID,

    val userId: UUID,

    val accountId: UUID,

    val categoryId: UUID?,

    val name: String,

    val type: TransactionType,

    val amount: BigDecimal,

    val merchant: String?,

    val note: String?,

    val status: RecurringTransactionTemplateStatus,

    val frequency: RecurringTransactionFrequency,

    val intervalCount: Short,

    val startDate: LocalDate,

    val endDate: LocalDate?,

    val lastRunDate: LocalDate?,

    val nextRunDate: LocalDate?,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,
)

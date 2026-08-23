package ru.digitalhustle.certis.model.transaction

import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class UpdateRecurringTransactionTemplateData(

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
)

package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class RecurringTransaction(

    val id: UUID,

    val userId: UUID,

    val accountId: UUID,

    val type: TransactionType,

    val amount: BigDecimal,

    val categoryId: UUID?,

    val frequency: RecurringTransactionFrequency,

    val nextRunDate: LocalDate,

    val isActive: Boolean,
)

package ru.digitalhustle.certis.features.transaction.query.model

import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

data class ForecastRecurringTemplate(
    val id: UUID,
    val name: String,
    val type: TransactionType,
    val amount: BigDecimal,
    val category: ForecastTransactionCategory?,
    val frequency: RecurringTransactionFrequency,
    val intervalCount: Short,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val nextRunDate: LocalDate,
    val updatedAt: OffsetDateTime,
)

data class ForecastActualTransaction(
    val id: UUID,
    val type: TransactionType,
    val amount: BigDecimal,
    val title: String,
    val category: ForecastTransactionCategory?,
    val occurredOn: LocalDate,
    val recurringTemplateId: UUID?,
)

data class ForecastHistoricalCategoryAmount(
    val type: TransactionType,
    val category: ForecastTransactionCategory,
    val month: YearMonth,
    val amount: BigDecimal,
)

data class ForecastTransactionCategory(
    val id: UUID,
    val name: String,
    val icon: String,
    val color: String,
)

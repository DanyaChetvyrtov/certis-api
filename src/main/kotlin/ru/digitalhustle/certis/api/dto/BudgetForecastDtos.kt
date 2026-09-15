package ru.digitalhustle.certis.api.dto

import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionFrequency
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class BudgetForecastHistoryWindowDto(

    val fromMonth: String,

    val toMonth: String,

    val monthsUsed: Int,

    val method: String,
)

data class BudgetForecastSummaryDto(

    val forecastIncome: BigDecimal,

    val recurringExpenses: BigDecimal,

    val flexibleEstimate: BigDecimal,

    val forecastExpenses: BigDecimal,

    val forecastSavings: BigDecimal,

    val includedItemCount: Int,

    val excludedItemCount: Int,
)

data class BudgetForecastItemDto(

    val sourceKey: String,

    val sourceType: BudgetForecastSourceType,

    val operationType: BudgetForecastOperationType,

    val title: String,

    val category: CategoryOptionDto?,

    val expectedDate: LocalDate?,

    val originalAmount: BigDecimal,

    val effectiveAmount: BigDecimal,

    val included: Boolean,

    val defaultConstraintRole: BudgetConstraintRole?,

    val confidence: BudgetForecastConfidence,

    val recurring: BudgetForecastRecurringSourceDto?,

    val history: BudgetForecastHistorySourceDto?,
)

data class BudgetForecastRecurringSourceDto(

    val templateId: UUID,

    val frequency: RecurringTransactionFrequency,
)

data class BudgetForecastHistorySourceDto(

    val monthsUsed: Int,

    val method: String,
)

data class BudgetForecastWarningDto(

    val code: String,

    val parameters: Map<String, Any?> = emptyMap(),
)

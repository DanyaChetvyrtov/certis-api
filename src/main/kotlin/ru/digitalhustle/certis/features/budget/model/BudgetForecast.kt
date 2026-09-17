package ru.digitalhustle.certis.features.budget.model

import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastConfidence
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastSourceType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

data class BudgetForecastPreview(
    val planId: UUID,
    val basedOnPlanVersion: Long,
    val sourceFingerprint: String,
    val generatedAt: OffsetDateTime,
    val historyWindow: BudgetForecastHistoryWindow,
    val summary: BudgetForecastSummary,
    val items: List<BudgetForecastItem>,
    val warnings: List<BudgetForecastWarning>,
)

data class BudgetForecast(
    val id: UUID,
    val userId: UUID,
    val planId: UUID,
    val revision: Int,
    val status: BudgetForecastStatus,
    val sourceFingerprint: String,
    val forecastFingerprint: String,
    val historyWindow: BudgetForecastHistoryWindow,
    val summary: BudgetForecastSummary,
    val items: List<BudgetForecastItem>,
    val planVersion: Long,
    val confirmedAt: OffsetDateTime,
)

data class BudgetForecastHistoryWindow(
    val fromMonth: YearMonth,
    val toMonth: YearMonth,
    val monthsUsed: Int,
    val method: String,
)

data class BudgetForecastSummary(
    val forecastIncome: BigDecimal,
    val recurringIncome: BigDecimal,
    val recurringExpenses: BigDecimal,
    val flexibleEstimate: BigDecimal,
    val forecastExpenses: BigDecimal,
    val forecastSavings: BigDecimal,
    val includedItemCount: Int,
    val excludedItemCount: Int,
)

data class BudgetForecastItem(
    val id: UUID,
    val sourceKey: String,
    val sourceType: BudgetForecastSourceType,
    val operationType: BudgetForecastOperationType,
    val title: String,
    val category: BudgetForecastCategory?,
    val expectedDate: LocalDate?,
    val originalAmount: BigDecimal,
    val effectiveAmount: BigDecimal,
    val included: Boolean,
    val defaultConstraintRole: BudgetConstraintRole?,
    val confidence: BudgetForecastConfidence,
    val recurring: BudgetForecastRecurringSource?,
    val transactionId: UUID?,
    val manualClientId: UUID?,
    val sourceUpdatedAt: OffsetDateTime?,
    val history: BudgetForecastHistorySource?,
    val sourcePayload: Map<String, Any?> = emptyMap(),
)

data class BudgetForecastCategory(
    val id: UUID,
    val type: CategoryType,
    val name: String,
    val icon: String,
    val color: String,
)

data class BudgetForecastRecurringSource(
    val templateId: UUID,
    val frequency: BudgetForecastFrequency,
)

enum class BudgetForecastFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class BudgetForecastHistorySource(
    val monthsUsed: Int,
    val method: String,
)

data class BudgetForecastWarning(
    val code: String,
    val parameters: Map<String, Any?> = emptyMap(),
)

data class BudgetForecastBaselineAllocation(
    val category: BudgetForecastCategory,
    val limitAmount: BigDecimal,
    val fixed: Boolean,
)

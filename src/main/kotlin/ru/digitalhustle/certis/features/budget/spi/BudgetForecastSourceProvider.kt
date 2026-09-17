package ru.digitalhustle.certis.features.budget.spi

import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

interface BudgetForecastSourceProvider {
    fun load(
        userId: UUID,
        currency: Currency,
        targetMonth: YearMonth,
        historyMonths: Int,
    ): BudgetForecastSourceSet
}

data class BudgetForecastSourceSet(
    val recurringOccurrences: List<ProjectedRecurringOccurrence>,
    val actualOperations: List<ActualForecastOperation>,
    val historicalCategories: List<HistoricalCategorySpending>,
    val historyFromMonth: YearMonth,
    val historyToMonth: YearMonth,
    val historyMonthsUsed: Int,
)

data class ProjectedRecurringOccurrence(
    val templateId: UUID,
    val sourceUpdatedAt: OffsetDateTime,
    val operationType: ForecastSourceOperationType,
    val title: String,
    val category: ForecastSourceCategory?,
    val scheduledFor: LocalDate,
    val amount: BigDecimal,
    val frequency: ForecastSourceFrequency,
)

data class ActualForecastOperation(
    val transactionId: UUID,
    val operationType: ForecastSourceOperationType,
    val title: String,
    val category: ForecastSourceCategory?,
    val occurredOn: LocalDate,
    val amount: BigDecimal,
    val recurringTemplateId: UUID?,
)

data class HistoricalCategorySpending(
    val operationType: ForecastSourceOperationType,
    val category: ForecastSourceCategory,
    val monthlyAmounts: Map<YearMonth, BigDecimal>,
)

data class ForecastSourceCategory(
    val id: UUID,
    val name: String,
    val icon: String,
    val color: String,
)

enum class ForecastSourceOperationType {
    INCOME,
    EXPENSE,
}

enum class ForecastSourceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

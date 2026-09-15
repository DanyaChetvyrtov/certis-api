package ru.digitalhustle.certis.features.budget.model

import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetFeasibilityStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningStep
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetPlanStateSnapshot(
    val forecast: BudgetPlanForecastSnapshot?,
    val constraints: BudgetPlanConstraintSnapshot?,
    val optimization: BudgetPlanOptimizationSnapshot?,
) {
    companion object {
        val EMPTY = BudgetPlanStateSnapshot(null, null, null)
    }
}

data class BudgetPlanForecastSnapshot(
    val revision: Int,
    val forecastIncome: BigDecimal,
    val recurringExpenses: BigDecimal,
    val flexibleEstimate: BigDecimal,
    val forecastExpenses: BigDecimal,
    val forecastSavings: BigDecimal,
    val includedItemCount: Int,
    val excludedItemCount: Int,
)

data class BudgetPlanConstraintSnapshot(
    val revision: Int,
    val forecastRevision: Int,
    val status: BudgetFeasibilityStatus,
    val savingsFloorAmount: BigDecimal,
    val requiredAmount: BigDecimal,
    val variableMinimumAmount: BigDecimal,
    val maximumSavingsAmount: BigDecimal,
    val shortfall: BigDecimal,
    val violations: List<BudgetPlanningViolation>,
)

data class BudgetPlanOptimizationSnapshot(
    val id: UUID,
    val status: BudgetOptimizationRunStatus,
    val targetSavingsAmount: BigDecimal,
    val actualSavingsAmount: BigDecimal?,
    val createdAt: OffsetDateTime,
)

data class BudgetPlanningViolation(
    val code: String,
    val shortfall: BigDecimal? = null,
    val requiredAmount: BigDecimal? = null,
    val availableAmount: BigDecimal? = null,
    val parameters: Map<String, Any?> = emptyMap(),
)

data class BudgetPlanView(
    val id: UUID,
    val previousPlanId: UUID?,
    val budgetMonth: LocalDate,
    val currency: Currency,
    val revision: Int,
    val status: BudgetPlanStatus,
    val currentStep: BudgetPlanningStep,
    val version: Long,
    val forecast: BudgetPlanForecastState,
    val constraints: BudgetPlanConstraintState,
    val currentOptimization: BudgetPlanOptimizationState?,
    val capabilities: BudgetPlanCapabilities,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class BudgetPlanForecastState(
    val revision: Int?,
    val status: BudgetForecastStatus,
    val summary: BudgetPlanForecastSummary?,
)

data class BudgetPlanForecastSummary(
    val forecastIncome: BigDecimal,
    val recurringExpenses: BigDecimal,
    val flexibleEstimate: BigDecimal,
    val forecastExpenses: BigDecimal,
    val forecastSavings: BigDecimal,
    val includedItemCount: Int,
    val excludedItemCount: Int,
)

data class BudgetPlanConstraintState(
    val revision: Int?,
    val status: BudgetConstraintStatus,
    val feasibility: BudgetPlanFeasibility?,
)

data class BudgetPlanFeasibility(
    val status: BudgetFeasibilityStatus,
    val forecastIncome: BigDecimal,
    val requiredAmount: BigDecimal,
    val variableMinimumAmount: BigDecimal,
    val savingsFloorAmount: BigDecimal,
    val maximumSavingsAmount: BigDecimal,
    val capacityAtSavingsFloor: BigDecimal,
    val shortfall: BigDecimal,
    val violations: List<BudgetPlanningViolation>,
)

data class BudgetPlanOptimizationState(
    val id: UUID,
    val status: BudgetOptimizationRunStatus,
    val targetSavingsAmount: BigDecimal,
    val actualSavingsAmount: BigDecimal?,
    val createdAt: OffsetDateTime,
)

data class BudgetPlanCapabilities(
    val canEditForecast: Boolean,
    val canEditConstraints: Boolean,
    val canRunOptimization: Boolean,
    val canApply: Boolean,
    val canCancel: Boolean,
)

data class BudgetPlanRevisions(
    val items: List<BudgetPlanRevision>,
)

data class BudgetPlanRevision(
    val id: UUID,
    val revision: Int,
    val status: BudgetPlanStatus,
    val currentStep: BudgetPlanningStep,
    val createdAt: OffsetDateTime,
    val appliedAt: OffsetDateTime?,
)

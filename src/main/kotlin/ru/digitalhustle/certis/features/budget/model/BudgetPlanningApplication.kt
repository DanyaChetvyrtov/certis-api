package ru.digitalhustle.certis.features.budget.model

import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningStep
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetPlanningApplicationResult(
    val plan: BudgetPlanningAppliedPlan,
    val optimization: BudgetPlanningAppliedOptimization,
    val budget: BudgetPlanningAppliedBudget,
)

data class BudgetPlanningAppliedPlan(
    val id: UUID,
    val revision: Int,
    val status: BudgetPlanStatus,
    val currentStep: BudgetPlanningStep,
    val version: Long,
    val appliedAt: OffsetDateTime,
)

data class BudgetPlanningAppliedOptimization(
    val id: UUID,
    val status: BudgetOptimizationRunStatus,
)

data class BudgetPlanningAppliedBudget(
    val id: UUID,
    val budgetMonth: LocalDate,
    val currency: Currency,
    val totalLimit: BigDecimal,
    val sourceOptimizationId: UUID,
    val allocations: List<BudgetPlanningAppliedAllocation>,
)

data class BudgetPlanningAppliedAllocation(
    val categoryId: UUID,
    val limit: BigDecimal,
)

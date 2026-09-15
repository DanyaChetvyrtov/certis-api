package ru.digitalhustle.certis.features.budget.model

import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetConstraintSet(
    val id: UUID?,
    val userId: UUID,
    val planId: UUID,
    val forecastRevisionId: UUID,
    val basedOnForecastRevision: Int,
    val revision: Int?,
    val status: BudgetConstraintStatus,
    val savingsFloorAmount: BigDecimal,
    val categories: List<BudgetCategoryConstraint>,
    val feasibility: BudgetPlanFeasibility,
    val constraintFingerprint: String?,
    val planVersion: Long,
    val createdAt: OffsetDateTime?,
)

data class BudgetCategoryConstraint(
    val id: UUID,
    val category: BudgetForecastCategory,
    val allocationType: BudgetAllocationType,
    val constraintRole: BudgetConstraintRole,
    val requiredAmount: BigDecimal,
    val priority: BudgetPriority?,
    val currentLimitAmount: BigDecimal,
    val fundingLevels: List<BudgetCategoryFundingLevel>,
    val sourceKeys: List<String>,
)

data class BudgetCategoryFundingLevel(
    val id: UUID,
    val level: BudgetFundingLevel,
    val amount: BigDecimal,
    val coverage: BigDecimal,
)

data class BudgetConstraintBaseline(
    val savingsFloorAmount: BigDecimal,
    val allocations: List<BudgetForecastBaselineAllocation>,
)

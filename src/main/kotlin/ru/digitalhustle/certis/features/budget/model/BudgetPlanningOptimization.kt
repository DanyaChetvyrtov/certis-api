package ru.digitalhustle.certis.features.budget.model

import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetPlanningOptimizationRun(
    val id: UUID,
    val userId: UUID,
    val planId: UUID,
    val forecastRevisionId: UUID,
    val constraintRevisionId: UUID,
    val status: BudgetOptimizationRunStatus,
    val algorithmVersion: String,
    val generationIdempotencyKey: String,
    val applyIdempotencyKey: String? = null,
    val inputFingerprint: String,
    val input: BudgetPlanningOptimizationInput,
    val result: BudgetPlanningOptimizationResult?,
    val decisions: List<BudgetPlanningOptimizationDecision>,
    val constraintChecks: List<BudgetPlanningConstraintCheck>,
    val violations: List<BudgetPlanningViolation>,
    val planVersion: Long,
    val createdAt: OffsetDateTime,
    val staleAt: OffsetDateTime? = null,
    val dismissedAt: OffsetDateTime? = null,
    val appliedAt: OffsetDateTime? = null,
)

data class BudgetPlanningOptimizationInput(
    val planVersion: Long,
    val forecastRevision: Int,
    val constraintsRevision: Int,
    val forecastIncome: BigDecimal,
    val requiredAmount: BigDecimal,
    val savingsFloorAmount: BigDecimal,
    val targetSavingsAmount: BigDecimal,
    val maximumSavingsAmount: BigDecimal,
    val flexibleCapacity: BigDecimal,
    val flexibleCategoryCount: Int,
    val candidateOptionCount: Int,
    val forecastFingerprint: String,
    val constraintFingerprint: String,
    val baselineSavingsAmount: BigDecimal,
)

data class BudgetPlanningOptimizationResult(
    val requiredAllocation: BigDecimal,
    val flexibleAllocation: BigDecimal,
    val totalAllocation: BigDecimal,
    val targetSavings: BigDecimal,
    val actualSavings: BigDecimal,
    val additionalSavingsComparedWithCurrent: BigDecimal,
    val coverageScore: BigDecimal,
    val objectiveValue: BigDecimal,
    val unusedCapacity: BigDecimal,
)

data class BudgetPlanningOptimizationDecision(
    val id: UUID,
    val categoryConstraintId: UUID,
    val fundingLevelId: UUID?,
    val category: BudgetForecastCategory,
    val allocationType: BudgetAllocationType,
    val constraintRole: BudgetConstraintRole,
    val priority: BudgetPriority?,
    val currentLimit: BigDecimal,
    val requiredAmount: BigDecimal,
    val selectedLevel: BudgetFundingLevel?,
    val recommendedLimit: BigDecimal,
    val change: BigDecimal,
    val coverage: BigDecimal,
    val optionValue: BigDecimal?,
    val reason: BudgetPlanningOptimizationReason,
)

data class BudgetPlanningOptimizationReason(
    val code: String,
    val parameters: Map<String, Any?> = emptyMap(),
)

data class BudgetPlanningConstraintCheck(
    val code: String,
    val satisfied: Boolean,
    val actual: BigDecimal,
    val required: BigDecimal,
)

data class BudgetPlanningOptimizationDismissal(
    val optimizationId: UUID,
    val status: BudgetOptimizationRunStatus,
    val planVersion: Long,
    val dismissedAt: OffsetDateTime,
)

package ru.digitalhustle.certis.api.dto

import java.math.BigDecimal

data class BudgetOptimizationInputDto(

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
)

data class BudgetOptimizationResultDto(

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

data class BudgetOptimizationDecisionDto(

    val category: CategoryOptionDto,

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

    val reason: BudgetOptimizationReasonDto,
)

data class BudgetOptimizationReasonDto(

    val code: String,

    val parameters: Map<String, Any?> = emptyMap(),
)

data class BudgetConstraintCheckDto(

    val code: String,

    val satisfied: Boolean,

    val actual: BigDecimal,

    val required: BigDecimal,
)

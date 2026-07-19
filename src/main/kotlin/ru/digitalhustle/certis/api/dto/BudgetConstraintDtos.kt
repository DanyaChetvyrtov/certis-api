package ru.digitalhustle.certis.api.dto

import java.math.BigDecimal

data class BudgetCategoryConstraintDto(

    val category: CategoryOptionDto,

    val allocationType: BudgetAllocationType,

    val constraintRole: BudgetConstraintRole,

    val requiredAmount: BigDecimal,

    val priority: BudgetPriority?,

    val fundingLevels: List<BudgetFundingLevelDto>,

    val sourceKeys: List<String>,
)

data class BudgetFundingLevelDto(

    val level: BudgetFundingLevel,

    val amount: BigDecimal,

    val coverage: BigDecimal,
)

data class BudgetFeasibilityDto(

    val status: BudgetFeasibilityStatus,

    val forecastIncome: BigDecimal,

    val requiredAmount: BigDecimal,

    val variableMinimumAmount: BigDecimal,

    val savingsFloorAmount: BigDecimal,

    val maximumSavingsAmount: BigDecimal,

    val capacityAtSavingsFloor: BigDecimal,

    val shortfall: BigDecimal,

    val violations: List<BudgetPlanningViolationDto>,
)

data class BudgetPlanningViolationDto(

    val code: String,

    val shortfall: BigDecimal? = null,

    val requiredAmount: BigDecimal? = null,

    val availableAmount: BigDecimal? = null,

    val parameters: Map<String, Any?> = emptyMap(),
)

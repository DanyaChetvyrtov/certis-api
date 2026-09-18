package ru.digitalhustle.certis.features.budget.command.model

import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import java.math.BigDecimal
import java.util.UUID

data class SaveBudgetConstraintsData(
    val planId: UUID,
    val userId: UUID,
    val expectedVersion: Long,
    val forecastRevision: Int,
    val savingsFloorAmount: BigDecimal,
    val categories: List<BudgetCategoryConstraintData>,
)

data class BudgetCategoryConstraintData(
    val categoryId: UUID,
    val allocationType: BudgetAllocationType,
    val constraintRole: BudgetConstraintRole,
    val requiredAmount: BigDecimal,
    val priority: BudgetPriority?,
    val fundingLevels: List<BudgetFundingLevelData>,
)

data class BudgetFundingLevelData(
    val level: BudgetFundingLevel,
    val amount: BigDecimal,
)

package ru.digitalhustle.certis.api.dto

import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationReason
import java.math.BigDecimal
import java.util.UUID

data class BudgetOptimizationAllocationDto(

    val allocationId: UUID,

    val categoryId: UUID,

    val categoryName: String,

    val categoryIcon: String,

    val categoryColor: String,

    val type: BudgetExpenseType,

    val status: BudgetAllocationStatus,

    val currentLimit: BigDecimal,

    val recommendedLimit: BigDecimal,

    val change: BigDecimal,

    val spent: BigDecimal,

    val reason: BudgetOptimizationReason,
)

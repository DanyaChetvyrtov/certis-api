package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.BudgetOptimizationReason
import java.math.BigDecimal
import java.util.UUID

data class BudgetOptimizationAllocationRs(

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

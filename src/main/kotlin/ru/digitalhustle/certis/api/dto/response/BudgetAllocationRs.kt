package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import java.math.BigDecimal
import java.util.UUID

data class BudgetAllocationRs(

    val id: UUID,

    val categoryId: UUID,

    val categoryName: String,

    val categoryIcon: String,

    val categoryColor: String,

    val type: BudgetExpenseType,

    val limit: BigDecimal,

    val spent: BigDecimal,

    val status: BudgetAllocationStatus,
)

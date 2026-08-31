package ru.digitalhustle.certis.model.budget

import ru.digitalhustle.certis.enums.BudgetExpenseType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class SaveBudgetData(

    val userId: UUID,

    val budgetMonth: LocalDate,

    val plannedIncome: BigDecimal,

    val savingsTarget: BigDecimal,

    val allocations: List<SaveBudgetAllocationData>,
)

data class SaveBudgetAllocationData(

    val categoryId: UUID,

    val expenseType: BudgetExpenseType,

    val limitAmount: BigDecimal,
)

data class ApplyBudgetOptimizationData(

    val userId: UUID,

    val inputSnapshot: BudgetOptimizationInputSnapshot,

    val allocations: List<SaveBudgetAllocationData>,
)

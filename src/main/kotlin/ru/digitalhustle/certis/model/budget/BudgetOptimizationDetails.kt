package ru.digitalhustle.certis.model.budget

import ru.digitalhustle.certis.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.BudgetOptimizationReason
import ru.digitalhustle.certis.enums.BudgetOptimizationStatus
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetOptimizationDetails(

    val id: UUID,

    val budgetId: UUID,

    val budgetMonth: LocalDate,

    val currency: Currency,

    val algorithmVersion: String,

    val status: BudgetOptimizationStatus,

    val savingsBefore: BigDecimal,

    val savingsAfter: BigDecimal,

    val allocations: List<BudgetOptimizationAllocation>,

    val createdAt: OffsetDateTime,

    val appliedAt: OffsetDateTime?,

    val inputSnapshot: BudgetOptimizationInputSnapshot,
)

data class BudgetOptimizationAllocation(

    val allocationId: UUID,

    val categoryId: UUID,

    val categoryName: String,

    val categoryIcon: String,

    val categoryColor: String,

    val expenseType: BudgetExpenseType,

    val status: BudgetAllocationStatus,

    val currentLimit: BigDecimal,

    val recommendedLimit: BigDecimal,

    val spentAmount: BigDecimal,

    val reason: BudgetOptimizationReason,
)

data class BudgetOptimizationInputSnapshot(

    val budgetId: UUID,

    val budgetMonth: LocalDate,

    val plannedIncome: BigDecimal,

    val savingsTarget: BigDecimal,

    val currency: Currency,

    val budgetUpdatedAt: OffsetDateTime,

    val allocations: List<BudgetOptimizationInputAllocation>,
) {

    fun matches(budget: BudgetDetails): Boolean =
        budgetId == budget.id &&
            budgetMonth == budget.budgetMonth &&
            plannedIncome.sameAmountAs(budget.plannedIncome) &&
            savingsTarget.sameAmountAs(budget.savingsTarget) &&
            currency == budget.currency &&
            budgetUpdatedAt.toInstant() == budget.updatedAt.toInstant() &&
            allocations.matches(budget.allocations)

    private fun List<BudgetOptimizationInputAllocation>.matches(
        currentAllocations: List<BudgetAllocationDetails>,
    ): Boolean {
        if (size != currentAllocations.size) {
            return false
        }

        val currentById = currentAllocations.associateBy(BudgetAllocationDetails::id)

        return all { source ->
            currentById[source.allocationId]?.let { current ->
                source.categoryId == current.categoryId &&
                    source.expenseType == current.expenseType &&
                    source.limitAmount.sameAmountAs(current.limitAmount) &&
                    source.spentAmount.sameAmountAs(current.spentAmount)
            } == true
        }
    }

    private fun BigDecimal.sameAmountAs(other: BigDecimal): Boolean = compareTo(other) == 0
}

data class BudgetOptimizationInputAllocation(

    val allocationId: UUID,

    val categoryId: UUID,

    val expenseType: BudgetExpenseType,

    val limitAmount: BigDecimal,

    val spentAmount: BigDecimal,
)

data class BudgetOptimizationResultSnapshot(

    val allocations: List<BudgetOptimizationAllocation>,
)

data class CalculatedBudgetOptimization(

    val inputSnapshot: BudgetOptimizationInputSnapshot,

    val resultSnapshot: BudgetOptimizationResultSnapshot,

    val savingsBefore: BigDecimal,

    val savingsAfter: BigDecimal,
)

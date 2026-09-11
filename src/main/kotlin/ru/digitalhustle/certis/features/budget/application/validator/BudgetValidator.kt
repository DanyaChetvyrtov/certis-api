package ru.digitalhustle.certis.features.budget.application.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.budget.command.model.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetData
import ru.digitalhustle.certis.features.budget.exceptions.BudgetOptimizationConflictException
import ru.digitalhustle.certis.features.budget.exceptions.InvalidBudgetException
import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import ru.digitalhustle.certis.features.category.api.ExpenseCategoryEligibility
import java.math.BigDecimal

@Component
class BudgetValidator(
    private val expenseCategoryEligibility: ExpenseCategoryEligibility,
) {

    fun validateBudget(budget: SaveBudgetData) {
        val categoryIds = budget.allocations.map { allocation -> allocation.categoryId }

        if (categoryIds.distinct().size != categoryIds.size) {
            throw InvalidBudgetException(ErrorMessages.BUDGET_DUPLICATE_CATEGORIES)
        }

        val allocatedAmount = budget.allocations.fold(BigDecimal.ZERO) { total, allocation ->
            total + allocation.limitAmount
        }
        if (allocatedAmount + budget.savingsTarget > budget.plannedIncome) {
            throw InvalidBudgetException(ErrorMessages.BUDGET_ALLOCATIONS_EXCEED_INCOME)
        }
    }

    fun validateCategories(budget: SaveBudgetData) {
        val categoryIds = budget.allocations.map { allocation -> allocation.categoryId }
        if (!expenseCategoryEligibility.areAllActive(budget.userId, categoryIds)) {
            throw InvalidBudgetException(ErrorMessages.BUDGET_CATEGORY_INVALID)
        }
    }

    fun validateOptimization(
        data: ApplyBudgetOptimizationData,
        currentDetails: BudgetDetails,
    ) {
        if (!data.inputSnapshot.matches(currentDetails) || !data.matchesSnapshotAllocations()) {
            throw BudgetOptimizationConflictException(ErrorMessages.BUDGET_OPTIMIZATION_STALE)
        }
    }

    private fun ApplyBudgetOptimizationData.matchesSnapshotAllocations(): Boolean {
        if (allocations.size != inputSnapshot.allocations.size) {
            return false
        }

        val sourceByCategory = inputSnapshot.allocations.associateBy { allocation -> allocation.categoryId }

        return allocations.all { allocation ->
            sourceByCategory[allocation.categoryId]?.expenseType == allocation.expenseType
        }
    }
}

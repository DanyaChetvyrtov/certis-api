package ru.digitalhustle.certis.api.mapper

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.api.dto.BudgetOptimizationAllocationDto
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationRs
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationAllocation
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationDetails
import java.time.YearMonth

@Component
class BudgetOptimizationMapper {

    fun convert(source: BudgetOptimizationDetails): BudgetOptimizationRs =
        BudgetOptimizationRs(
            id = source.id,
            budgetId = source.budgetId,
            month = YearMonth.from(source.budgetMonth).toString(),
            currency = source.currency,
            algorithmVersion = source.algorithmVersion,
            status = source.status,
            savingsBefore = source.savingsBefore,
            savingsAfter = source.savingsAfter,
            additionalSavings = source.savingsAfter - source.savingsBefore,
            allocations = source.allocations.map(::convert),
            createdAt = source.createdAt,
            appliedAt = source.appliedAt,
        )

    private fun convert(source: BudgetOptimizationAllocation): BudgetOptimizationAllocationDto =
        BudgetOptimizationAllocationDto(
            allocationId = source.allocationId,
            categoryId = source.categoryId,
            categoryName = source.categoryName,
            categoryIcon = source.categoryIcon,
            categoryColor = source.categoryColor,
            type = source.expenseType,
            status = source.status,
            currentLimit = source.currentLimit,
            recommendedLimit = source.recommendedLimit,
            change = source.recommendedLimit - source.currentLimit,
            spent = source.spentAmount,
            reason = source.reason,
        )
}

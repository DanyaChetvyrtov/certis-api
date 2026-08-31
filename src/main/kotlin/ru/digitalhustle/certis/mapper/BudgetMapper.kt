package ru.digitalhustle.certis.mapper

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.dto.request.SaveBudgetRq
import ru.digitalhustle.certis.dto.response.BudgetAllocationRs
import ru.digitalhustle.certis.dto.response.BudgetRs
import ru.digitalhustle.certis.model.budget.BudgetAllocationDetails
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.SaveBudgetAllocationData
import ru.digitalhustle.certis.model.budget.SaveBudgetData
import java.time.YearMonth
import java.util.UUID

@Component
class BudgetMapper {

    fun convert(
        source: SaveBudgetRq,
        userId: UUID,
        budgetMonth: YearMonth,
    ): SaveBudgetData =
        SaveBudgetData(
            userId = userId,
            budgetMonth = budgetMonth.atDay(1),
            plannedIncome = source.monthlyIncome,
            savingsTarget = source.savingsTarget,
            allocations = source.allocations.map { allocation ->
                SaveBudgetAllocationData(
                    categoryId = allocation.categoryId,
                    expenseType = allocation.type,
                    limitAmount = allocation.limit,
                )
            },
        )

    fun convert(source: BudgetDetails): BudgetRs =
        BudgetRs(
            id = source.id,
            month = YearMonth.from(source.budgetMonth).toString(),
            currency = source.currency,
            monthlyIncome = source.plannedIncome,
            savingsTarget = source.savingsTarget,
            allocations = source.allocations.map(::convert),
            createdAt = source.createdAt,
            updatedAt = source.updatedAt,
        )

    private fun convert(source: BudgetAllocationDetails): BudgetAllocationRs =
        BudgetAllocationRs(
            id = source.id,
            categoryId = source.categoryId,
            categoryName = source.categoryName,
            categoryIcon = source.categoryIcon,
            categoryColor = source.categoryColor,
            type = source.expenseType,
            limit = source.limitAmount,
            spent = source.spentAmount,
            status = source.status,
        )
}

package ru.digitalhustle.certis.service.budget

import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.BudgetOptimizationDetails
import java.time.LocalDate
import java.util.UUID

interface BudgetOptimizationAggregator {

    fun getLatest(userId: UUID, budgetMonth: LocalDate): BudgetOptimizationDetails

    fun generate(userId: UUID, budgetMonth: LocalDate): BudgetOptimizationDetails

    fun apply(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails

    fun dismiss(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    )
}

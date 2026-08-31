package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.budget.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.SaveBudgetData
import java.time.LocalDate
import java.util.UUID

interface BudgetService {

    fun getByMonth(userId: UUID, budgetMonth: LocalDate): BudgetDetails

    fun getByMonthForUpdate(userId: UUID, budgetMonth: LocalDate): BudgetDetails

    fun save(budget: SaveBudgetData): BudgetDetails

    fun applyOptimization(data: ApplyBudgetOptimizationData): BudgetDetails
}

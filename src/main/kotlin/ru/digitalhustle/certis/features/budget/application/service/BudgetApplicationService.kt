package ru.digitalhustle.certis.features.budget.application.service

import ru.digitalhustle.certis.features.budget.command.model.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetData
import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import java.time.LocalDate
import java.util.UUID

interface BudgetApplicationService {

    fun getByMonthForUpdate(userId: UUID, budgetMonth: LocalDate): BudgetDetails

    fun save(budget: SaveBudgetData): BudgetDetails

    fun applyOptimization(data: ApplyBudgetOptimizationData): BudgetDetails
}

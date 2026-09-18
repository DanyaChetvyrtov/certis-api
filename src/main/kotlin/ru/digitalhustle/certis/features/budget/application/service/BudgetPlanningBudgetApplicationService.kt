package ru.digitalhustle.certis.features.budget.application.service

import ru.digitalhustle.certis.features.budget.command.model.ApplyBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningApplicationResult

interface BudgetPlanningBudgetApplicationService {
    fun apply(data: ApplyBudgetPlanningOptimizationData): BudgetPlanningApplicationResult
}

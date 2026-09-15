package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.model.BudgetPlan

interface BudgetPlanCommandService {
    fun create(data: CreateBudgetPlanData): BudgetPlan
}

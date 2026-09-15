package ru.digitalhustle.certis.features.budget.application.service

import ru.digitalhustle.certis.features.budget.command.model.CancelBudgetPlanData
import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.model.BudgetPlanView

interface BudgetPlanningApplicationService {
    fun create(data: CreateBudgetPlanData): BudgetPlanView

    fun cancel(data: CancelBudgetPlanData): BudgetPlanView
}

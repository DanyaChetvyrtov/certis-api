package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import java.util.UUID

interface BudgetPlanCommandService {
    fun getByIdForUpdate(id: UUID, userId: UUID): BudgetPlan

    fun create(data: CreateBudgetPlanData): BudgetPlan

    fun incrementVersion(plan: BudgetPlan): BudgetPlan
}

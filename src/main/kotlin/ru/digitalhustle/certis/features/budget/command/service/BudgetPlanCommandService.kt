package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import java.time.OffsetDateTime
import java.util.UUID

interface BudgetPlanCommandService {
    fun getByIdForUpdate(id: UUID, userId: UUID): BudgetPlan

    fun create(data: CreateBudgetPlanData): BudgetPlan

    fun incrementVersion(plan: BudgetPlan): BudgetPlan

    fun apply(plan: BudgetPlan, budgetId: UUID, appliedAt: OffsetDateTime): BudgetPlan

    fun cancel(plan: BudgetPlan, cancelledAt: OffsetDateTime): BudgetPlan

    fun supersedeCurrentApplied(plan: BudgetPlan, supersededAt: OffsetDateTime)
}

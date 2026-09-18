package ru.digitalhustle.certis.features.budget.application.service

import ru.digitalhustle.certis.features.budget.command.model.DismissBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.command.model.GenerateBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationDismissal
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import java.util.UUID

interface BudgetPlanningOptimizationApplicationService {
    fun getLatest(planId: UUID, userId: UUID): BudgetPlanningOptimizationRun

    fun generate(data: GenerateBudgetPlanningOptimizationData): BudgetPlanningOptimizationRun

    fun dismiss(data: DismissBudgetPlanningOptimizationData): BudgetPlanningOptimizationDismissal
}

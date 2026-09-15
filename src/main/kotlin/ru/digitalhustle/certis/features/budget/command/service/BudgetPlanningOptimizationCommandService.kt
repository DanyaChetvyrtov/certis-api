package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import java.time.OffsetDateTime

interface BudgetPlanningOptimizationCommandService {
    fun save(run: BudgetPlanningOptimizationRun, staleAt: OffsetDateTime)

    fun markStale(run: BudgetPlanningOptimizationRun, staleAt: OffsetDateTime): BudgetPlanningOptimizationRun

    fun dismiss(run: BudgetPlanningOptimizationRun, dismissedAt: OffsetDateTime)

    fun apply(
        run: BudgetPlanningOptimizationRun,
        idempotencyKey: String,
        appliedAt: OffsetDateTime,
    ): BudgetPlanningOptimizationRun
}

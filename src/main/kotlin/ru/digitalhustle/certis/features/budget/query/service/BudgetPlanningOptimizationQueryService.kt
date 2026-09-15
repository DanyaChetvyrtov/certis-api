package ru.digitalhustle.certis.features.budget.query.service

import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import java.util.UUID

interface BudgetPlanningOptimizationQueryService {
    fun getLatest(planId: UUID, userId: UUID): BudgetPlanningOptimizationRun

    fun getById(planId: UUID, optimizationId: UUID, userId: UUID): BudgetPlanningOptimizationRun

    fun findByGenerationKey(userId: UUID, idempotencyKey: String): BudgetPlanningOptimizationRun?

    fun findByApplyKey(userId: UUID, idempotencyKey: String): BudgetPlanningOptimizationRun?
}

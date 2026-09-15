package ru.digitalhustle.certis.features.budget.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningOptimizationNotFoundException
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import ru.digitalhustle.certis.features.budget.query.repository.BudgetPlanningOptimizationQueryRepository
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanningOptimizationQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BudgetPlanningOptimizationQueryServiceImpl(
    private val planQueryService: BudgetPlanQueryService,
    private val repository: BudgetPlanningOptimizationQueryRepository,
) : BudgetPlanningOptimizationQueryService {

    override fun getLatest(planId: UUID, userId: UUID): BudgetPlanningOptimizationRun {
        planQueryService.getEntityById(planId, userId)
        return repository.findLatest(planId, userId)
            ?: throw BudgetPlanningOptimizationNotFoundException(mapOf("planId" to planId))
    }

    override fun getById(planId: UUID, optimizationId: UUID, userId: UUID): BudgetPlanningOptimizationRun {
        planQueryService.getEntityById(planId, userId)
        return repository.findById(planId, optimizationId, userId)
            ?: throw BudgetPlanningOptimizationNotFoundException(
                mapOf("planId" to planId, "optimizationId" to optimizationId),
            )
    }

    override fun findByGenerationKey(userId: UUID, idempotencyKey: String): BudgetPlanningOptimizationRun? =
        repository.findByGenerationKey(userId, idempotencyKey)

    override fun findByApplyKey(userId: UUID, idempotencyKey: String): BudgetPlanningOptimizationRun? =
        repository.findByApplyKey(userId, idempotencyKey)
}

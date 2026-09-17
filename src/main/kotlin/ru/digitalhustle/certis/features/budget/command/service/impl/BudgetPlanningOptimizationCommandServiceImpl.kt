package ru.digitalhustle.certis.features.budget.command.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.command.repository.BudgetOptimizationDecisionRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetOptimizationRunRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanningOptimizationCommandService
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import java.time.OffsetDateTime

@Service
class BudgetPlanningOptimizationCommandServiceImpl(
    private val runRepository: BudgetOptimizationRunRepository,
    private val decisionRepository: BudgetOptimizationDecisionRepository,
) : BudgetPlanningOptimizationCommandService {

    override fun save(run: BudgetPlanningOptimizationRun, staleAt: OffsetDateTime) {
        runRepository.markGeneratedStale(run.planId, run.userId, staleAt)
        runRepository.insert(run)
        decisionRepository.insertAll(run)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun markStale(
        run: BudgetPlanningOptimizationRun,
        staleAt: OffsetDateTime,
    ): BudgetPlanningOptimizationRun {
        if (!runRepository.markStale(run.id, run.planId, run.userId, staleAt)) {
            throw stateConflict(run)
        }
        return run.copy(status = BudgetOptimizationRunStatus.STALE, staleAt = staleAt)
    }

    override fun dismiss(run: BudgetPlanningOptimizationRun, dismissedAt: OffsetDateTime) {
        if (!runRepository.dismiss(run.id, run.planId, run.userId, dismissedAt)) {
            throw stateConflict(run)
        }
    }

    override fun apply(
        run: BudgetPlanningOptimizationRun,
        idempotencyKey: String,
        appliedAt: OffsetDateTime,
    ): BudgetPlanningOptimizationRun {
        if (!runRepository.apply(run.id, run.planId, run.userId, idempotencyKey, appliedAt)) {
            throw stateConflict(run)
        }
        return run.copy(
            status = BudgetOptimizationRunStatus.APPLIED,
            applyIdempotencyKey = idempotencyKey,
            appliedAt = appliedAt,
        )
    }

    private fun stateConflict(run: BudgetPlanningOptimizationRun): BudgetPlanningConflictException =
        BudgetPlanningConflictException(
            message = "Budget optimization state changed; refresh the plan",
            code = BudgetPlanningErrorCode.OPTIMIZATION_STALE,
            details = mapOf("planId" to run.planId, "optimizationId" to run.id),
        )
}

package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningOptimizationApplicationService
import ru.digitalhustle.certis.features.budget.application.validator.BudgetForecastValidator
import ru.digitalhustle.certis.features.budget.application.validator.BudgetPlanningOptimizationValidator
import ru.digitalhustle.certis.features.budget.command.model.DismissBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.command.model.GenerateBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanningOptimizationCommandService
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationDismissal
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import ru.digitalhustle.certis.features.budget.query.service.BudgetConstraintQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetForecastQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanningOptimizationQueryService
import ru.digitalhustle.certis.features.budget.util.BudgetPlanningOptimizationCalculator
import ru.digitalhustle.certis.features.budget.util.BudgetPlanningOptimizationFreshnessChecker
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class BudgetPlanningOptimizationApplicationServiceImpl(
    private val planCommandService: BudgetPlanCommandService,
    private val forecastQueryService: BudgetForecastQueryService,
    private val constraintQueryService: BudgetConstraintQueryService,
    private val optimizationQueryService: BudgetPlanningOptimizationQueryService,
    private val optimizationCommandService: BudgetPlanningOptimizationCommandService,
    private val calculator: BudgetPlanningOptimizationCalculator,
    private val freshnessChecker: BudgetPlanningOptimizationFreshnessChecker,
    private val forecastValidator: BudgetForecastValidator,
    private val optimizationValidator: BudgetPlanningOptimizationValidator,
    private val applicationClock: ApplicationClock,
) : BudgetPlanningOptimizationApplicationService {

    @Transactional
    override fun getLatest(planId: UUID, userId: UUID): BudgetPlanningOptimizationRun {
        val run = optimizationQueryService.getLatest(planId, userId)
        if (run.status != BudgetOptimizationRunStatus.GENERATED) return run
        val forecast = forecastQueryService.getCurrent(planId, userId)
        val constraints = constraintQueryService.findLatest(planId, userId)
        return if (freshnessChecker.isCurrent(run, forecast, constraints)) {
            run
        } else {
            optimizationCommandService.markStale(run, applicationClock.now())
        }
    }

    @Transactional
    override fun generate(data: GenerateBudgetPlanningOptimizationData): BudgetPlanningOptimizationRun {
        val normalized = data.copy(idempotencyKey = data.idempotencyKey.trim())
        val plan = planCommandService.getByIdForUpdate(normalized.planId, normalized.userId)
        optimizationQueryService.findByGenerationKey(normalized.userId, normalized.idempotencyKey)
            ?.let { existing ->
                optimizationValidator.validateIdempotentReplay(existing, normalized)
                return existing
            }
        forecastValidator.validatePlan(plan, normalized.expectedVersion)
        val forecast = forecastQueryService.getCurrent(plan.id, plan.userId)
        val constraints = optimizationValidator.validateInputs(
            normalized,
            forecast,
            constraintQueryService.findLatest(plan.id, plan.userId),
        )
        val now = applicationClock.now()
        val nextPlan = planCommandService.incrementVersion(plan)
        val run = calculator.calculate(
            plan = plan,
            resultPlanVersion = nextPlan.version,
            forecast = forecast,
            constraints = constraints,
            targetSavingsAmount = normalized.targetSavingsAmount,
            idempotencyKey = normalized.idempotencyKey,
            createdAt = now,
        )
        optimizationCommandService.save(run, now)
        return run
    }

    @Transactional
    override fun dismiss(data: DismissBudgetPlanningOptimizationData): BudgetPlanningOptimizationDismissal {
        val plan = planCommandService.getByIdForUpdate(data.planId, data.userId)
        forecastValidator.validatePlan(plan, data.expectedVersion)
        val run = optimizationQueryService.getById(data.planId, data.optimizationId, data.userId)
        optimizationValidator.validateDismissible(run)
        val dismissedAt = applicationClock.now()
        val nextPlan = planCommandService.incrementVersion(plan)
        optimizationCommandService.dismiss(run, dismissedAt)
        return BudgetPlanningOptimizationDismissal(
            optimizationId = run.id,
            status = BudgetOptimizationRunStatus.DISMISSED,
            planVersion = nextPlan.version,
            dismissedAt = dismissedAt,
        )
    }
}

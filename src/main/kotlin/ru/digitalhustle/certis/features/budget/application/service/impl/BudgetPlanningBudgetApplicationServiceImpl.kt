package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningBudgetApplicationService
import ru.digitalhustle.certis.features.budget.application.validator.BudgetForecastValidator
import ru.digitalhustle.certis.features.budget.command.model.ApplyBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.command.service.BudgetAllocationService
import ru.digitalhustle.certis.features.budget.command.service.BudgetCommandStore
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanningOptimizationCommandService
import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningStep
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.model.Budget
import ru.digitalhustle.certis.features.budget.model.BudgetCategory
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningApplicationResult
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedAllocation
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedBudget
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedOptimization
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import ru.digitalhustle.certis.features.budget.query.service.BudgetConstraintQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetForecastQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanningOptimizationQueryService
import ru.digitalhustle.certis.features.budget.util.BudgetPlanningOptimizationFreshnessChecker
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BudgetPlanningBudgetApplicationServiceImpl(
    private val planCommandService: BudgetPlanCommandService,
    private val forecastQueryService: BudgetForecastQueryService,
    private val constraintQueryService: BudgetConstraintQueryService,
    private val optimizationQueryService: BudgetPlanningOptimizationQueryService,
    private val optimizationCommandService: BudgetPlanningOptimizationCommandService,
    private val budgetStore: BudgetCommandStore,
    private val budgetAllocationService: BudgetAllocationService,
    private val freshnessChecker: BudgetPlanningOptimizationFreshnessChecker,
    private val forecastValidator: BudgetForecastValidator,
    private val applicationClock: ApplicationClock,
) : BudgetPlanningBudgetApplicationService {

    @Transactional
    override fun apply(data: ApplyBudgetPlanningOptimizationData): BudgetPlanningApplicationResult {
        val normalized = data.copy(idempotencyKey = data.idempotencyKey.trim())
        val plan = planCommandService.getByIdForUpdate(normalized.planId, normalized.userId)
        optimizationQueryService.findByApplyKey(normalized.userId, normalized.idempotencyKey)
            ?.let { existing -> return replay(plan, existing, normalized) }
        validateApplicablePlan(plan, normalized.expectedVersion)
        val run = optimizationQueryService.getById(plan.id, normalized.optimizationId, plan.userId)
        validateApplicableRun(run)
        validateFreshness(run)

        val appliedAt = applicationClock.now()
        val appliedRun = applyRun(run, normalized.idempotencyKey, appliedAt)
        val budget = saveBudget(plan, appliedRun, appliedAt)
        planCommandService.supersedeCurrentApplied(plan, appliedAt)
        val appliedPlan = planCommandService.apply(plan, budget.id, appliedAt)
        return result(appliedPlan, appliedRun, budget.id)
    }

    private fun replay(
        plan: BudgetPlan,
        run: BudgetPlanningOptimizationRun,
        data: ApplyBudgetPlanningOptimizationData,
    ): BudgetPlanningApplicationResult {
        val sameCommand = run.planId == data.planId && run.id == data.optimizationId
        if (!sameCommand || run.status != BudgetOptimizationRunStatus.APPLIED) {
            throw idempotencyKeyReused(data)
        }
        val budgetId = plan.appliedBudgetId
            ?: throw invalidState(plan, "Applied budget is missing from the budget plan")
        return result(plan, run, budgetId)
    }

    private fun validateApplicablePlan(plan: BudgetPlan, expectedVersion: Long) {
        if (plan.status in setOf(BudgetPlanStatus.APPLIED, BudgetPlanStatus.SUPERSEDED)) {
            throw BudgetPlanningConflictException(
                message = "Budget plan was already applied",
                code = BudgetPlanningErrorCode.BUDGET_PLAN_ALREADY_APPLIED,
                details = mapOf("planId" to plan.id, "status" to plan.status.name),
            )
        }
        forecastValidator.validatePlan(plan, expectedVersion)
    }

    private fun validateApplicableRun(run: BudgetPlanningOptimizationRun) {
        if (run.status == BudgetOptimizationRunStatus.GENERATED) return
        val code = if (run.status == BudgetOptimizationRunStatus.STALE) {
            BudgetPlanningErrorCode.OPTIMIZATION_STALE
        } else {
            BudgetPlanningErrorCode.INVALID_BUDGET_PLAN_STATE
        }
        throw BudgetPlanningConflictException(
            message = "Only a current generated optimization can be applied",
            code = code,
            details = mapOf("optimizationId" to run.id, "status" to run.status.name),
        )
    }

    private fun validateFreshness(run: BudgetPlanningOptimizationRun) {
        val forecast = forecastQueryService.getCurrent(run.planId, run.userId)
        val constraints = constraintQueryService.findLatest(run.planId, run.userId)
        if (freshnessChecker.isCurrent(run, forecast, constraints)) return
        optimizationCommandService.markStale(run, applicationClock.now())
        throw BudgetPlanningConflictException(
            message = "Budget optimization inputs changed; generate a new optimization",
            code = BudgetPlanningErrorCode.OPTIMIZATION_STALE,
            details = mapOf("planId" to run.planId, "optimizationId" to run.id),
        )
    }

    private fun applyRun(
        run: BudgetPlanningOptimizationRun,
        idempotencyKey: String,
        appliedAt: OffsetDateTime,
    ): BudgetPlanningOptimizationRun =
        try {
            optimizationCommandService.apply(run, idempotencyKey, appliedAt)
        } catch (exception: DuplicateKeyException) {
            throw BudgetPlanningConflictException(
                message = "Idempotency key is already used for another budget application",
                code = BudgetPlanningErrorCode.IDEMPOTENCY_KEY_REUSED,
                details = mapOf("idempotencyKey" to idempotencyKey),
                cause = exception,
            )
        }

    private fun saveBudget(
        plan: BudgetPlan,
        run: BudgetPlanningOptimizationRun,
        appliedAt: OffsetDateTime,
    ): Budget {
        val existing = budgetStore.findByUserIdAndMonthAndCurrencyForUpdate(
            plan.userId,
            plan.budgetMonth,
            plan.currency,
        )
        existing?.let { budgetAllocationService.deleteAllocations(it.id) }
        val budget = existing?.copy(
            plannedIncome = run.input.forecastIncome,
            savingsTarget = run.input.targetSavingsAmount,
            updatedAt = appliedAt,
            sourceOptimizationId = run.id,
        ) ?: Budget(
            id = UUID.randomUUID(),
            userId = plan.userId,
            budgetMonth = plan.budgetMonth,
            plannedIncome = run.input.forecastIncome,
            savingsTarget = run.input.targetSavingsAmount,
            currency = plan.currency,
            createdAt = appliedAt,
            updatedAt = appliedAt,
            sourceOptimizationId = run.id,
        )
        val saved = if (existing == null) budgetStore.insert(budget) else budgetStore.update(budget)
        budgetAllocationService.insertAllocations(
            run.decisions.map { decision ->
                BudgetCategory(
                    id = UUID.randomUUID(),
                    userId = plan.userId,
                    budgetId = saved.id,
                    categoryId = decision.category.id,
                    categoryType = decision.category.type,
                    limitAmount = decision.recommendedLimit,
                    expenseType = BudgetExpenseType.valueOf(decision.allocationType.name),
                )
            },
        )
        return saved
    }

    private fun result(
        plan: BudgetPlan,
        run: BudgetPlanningOptimizationRun,
        budgetId: UUID,
    ): BudgetPlanningApplicationResult =
        BudgetPlanningApplicationResult(
            plan = BudgetPlanningAppliedPlan(
                id = plan.id,
                revision = plan.revision,
                status = plan.status,
                currentStep = BudgetPlanningStep.APPLIED,
                version = plan.version,
                appliedAt = requireNotNull(plan.appliedAt),
            ),
            optimization = BudgetPlanningAppliedOptimization(run.id, run.status),
            budget = BudgetPlanningAppliedBudget(
                id = budgetId,
                budgetMonth = plan.budgetMonth,
                currency = plan.currency,
                totalLimit = requireNotNull(run.result).totalAllocation,
                sourceOptimizationId = run.id,
                allocations = run.decisions.map { decision ->
                    BudgetPlanningAppliedAllocation(decision.category.id, decision.recommendedLimit)
                },
            ),
        )

    private fun idempotencyKeyReused(data: ApplyBudgetPlanningOptimizationData) =
        BudgetPlanningConflictException(
            message = "Idempotency key is already used for another budget application",
            code = BudgetPlanningErrorCode.IDEMPOTENCY_KEY_REUSED,
            details = mapOf("idempotencyKey" to data.idempotencyKey),
        )

    private fun invalidState(plan: BudgetPlan, message: String) =
        BudgetPlanningConflictException(
            message = message,
            code = BudgetPlanningErrorCode.INVALID_BUDGET_PLAN_STATE,
            details = mapOf("planId" to plan.id, "status" to plan.status.name),
        )
}

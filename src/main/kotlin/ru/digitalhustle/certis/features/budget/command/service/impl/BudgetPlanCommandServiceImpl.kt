package ru.digitalhustle.certis.features.budget.command.service.impl

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.command.repository.BudgetPlanRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

@Service
class BudgetPlanCommandServiceImpl(
    private val repository: BudgetPlanRepository,
    private val budgetRepository: BudgetRepository,
    private val applicationClock: ApplicationClock,
) : BudgetPlanCommandService {

    override fun getByIdForUpdate(id: UUID, userId: UUID): BudgetPlan =
        repository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanNotFoundException(
                details = mapOf("planId" to id),
            )

    override fun create(data: CreateBudgetPlanData): BudgetPlan {
        val normalizedData = data.copy(idempotencyKey = data.idempotencyKey.trim())
        repository.findByUserIdAndIdempotencyKey(normalizedData.userId, normalizedData.idempotencyKey)
            ?.let { existing -> return validateIdempotentReplay(existing, normalizedData) }

        repository.findActiveDraft(normalizedData.userId, normalizedData.budgetMonth, normalizedData.currency)
            ?.let { existing -> throw activePlanExists(existing) }

        val previousPlan = repository.findLatestByScopeForUpdate(
            normalizedData.userId,
            normalizedData.budgetMonth,
            normalizedData.currency,
        )
        val now = applicationClock.now()
        val plan = BudgetPlan(
            id = UUID.randomUUID(),
            userId = normalizedData.userId,
            previousPlanId = previousPlan?.id,
            baselineBudgetId = budgetRepository.findIdByUserIdAndMonthAndCurrency(
                normalizedData.userId,
                normalizedData.budgetMonth,
                normalizedData.currency,
            ),
            appliedBudgetId = null,
            budgetMonth = normalizedData.budgetMonth,
            currency = normalizedData.currency,
            revision = previousPlan?.revision?.plus(1) ?: 1,
            version = 0,
            status = BudgetPlanStatus.DRAFT,
            idempotencyKey = normalizedData.idempotencyKey,
            createdAt = now,
            updatedAt = now,
            appliedAt = null,
            supersededAt = null,
            cancelledAt = null,
        )

        return try {
            validateIdempotentReplay(repository.insertOrFindByIdempotencyKey(plan), normalizedData)
        } catch (exception: DuplicateKeyException) {
            throw BudgetPlanningConflictException(
                message = "An active budget plan already exists for the requested month and currency",
                code = BudgetPlanningErrorCode.ACTIVE_BUDGET_PLAN_EXISTS,
                details = scopeDetails(normalizedData),
                cause = exception,
            )
        }
    }

    override fun incrementVersion(plan: BudgetPlan): BudgetPlan =
        repository.updateVersion(plan.copy(updatedAt = applicationClock.now()))
            ?: throw versionConflict(plan)

    override fun apply(plan: BudgetPlan, budgetId: UUID, appliedAt: OffsetDateTime): BudgetPlan =
        repository.apply(plan, budgetId, appliedAt) ?: throw versionConflict(plan)

    override fun cancel(plan: BudgetPlan, cancelledAt: OffsetDateTime): BudgetPlan =
        repository.cancel(plan, cancelledAt) ?: throw versionConflict(plan)

    override fun supersedeCurrentApplied(plan: BudgetPlan, supersededAt: OffsetDateTime) {
        repository.supersedeCurrentApplied(plan, supersededAt)
    }

    private fun validateIdempotentReplay(
        existing: BudgetPlan,
        data: CreateBudgetPlanData,
    ): BudgetPlan {
        if (existing.budgetMonth != data.budgetMonth || existing.currency != data.currency) {
            throw BudgetPlanningConflictException(
                message = "Idempotency key is already used for another budget plan",
                code = BudgetPlanningErrorCode.IDEMPOTENCY_KEY_REUSED,
                details = mapOf("planId" to existing.id),
            )
        }

        return existing
    }

    private fun activePlanExists(plan: BudgetPlan): BudgetPlanningConflictException =
        BudgetPlanningConflictException(
            message = "An active budget plan already exists for the requested month and currency",
            code = BudgetPlanningErrorCode.ACTIVE_BUDGET_PLAN_EXISTS,
            details = mapOf("planId" to plan.id),
        )

    private fun versionConflict(plan: BudgetPlan): BudgetPlanningConflictException =
        BudgetPlanningConflictException(
            message = "Budget plan was changed by another request",
            code = BudgetPlanningErrorCode.PLANNING_VERSION_CONFLICT,
            details = mapOf("planId" to plan.id, "expectedVersion" to plan.version),
        )

    private fun scopeDetails(data: CreateBudgetPlanData): Map<String, Any> =
        mapOf(
            "month" to YearMonth.from(data.budgetMonth).toString(),
            "currency" to data.currency.name,
        )
}

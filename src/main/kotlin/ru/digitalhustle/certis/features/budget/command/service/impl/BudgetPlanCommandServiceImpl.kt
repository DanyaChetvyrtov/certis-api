package ru.digitalhustle.certis.features.budget.command.service.impl

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.command.repository.BudgetPlanRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.YearMonth
import java.util.UUID

@Service
class BudgetPlanCommandServiceImpl(
    private val repository: BudgetPlanRepository,
    private val applicationClock: ApplicationClock,
) : BudgetPlanCommandService {

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
            baselineBudgetId = repository.findBaselineBudgetId(
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

    private fun scopeDetails(data: CreateBudgetPlanData): Map<String, Any> =
        mapOf(
            "month" to YearMonth.from(data.budgetMonth).toString(),
            "currency" to data.currency.name,
        )
}

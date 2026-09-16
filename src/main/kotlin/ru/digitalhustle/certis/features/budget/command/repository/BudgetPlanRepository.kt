package ru.digitalhustle.certis.features.budget.command.repository

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.util.UUID

@Repository
class BudgetPlanRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserIdForUpdate(id: UUID, userId: UUID): BudgetPlan? =
        dsl.selectFrom(Tables.BUDGET_PLANS)
            .where(
                Tables.BUDGET_PLANS.ID.eq(id)
                    .and(Tables.BUDGET_PLANS.USER_ID.eq(userId)),
            )
            .forUpdate()
            .fetchOne(::toEntity)

    fun findByUserIdAndIdempotencyKey(
        userId: UUID,
        idempotencyKey: String,
    ): BudgetPlan? =
        dsl.selectFrom(Tables.BUDGET_PLANS)
            .where(
                Tables.BUDGET_PLANS.USER_ID.eq(userId)
                    .and(Tables.BUDGET_PLANS.IDEMPOTENCY_KEY.eq(idempotencyKey)),
            )
            .fetchOne(::toEntity)

    fun findActiveDraft(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): BudgetPlan? =
        selectByScope(userId, budgetMonth, currency)
            .and(Tables.BUDGET_PLANS.STATUS.eq(BudgetPlanStatus.DRAFT.name))
            .fetchOne(::toEntity)

    fun findLatestByScopeForUpdate(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): BudgetPlan? =
        dsl.selectFrom(Tables.BUDGET_PLANS)
            .where(scopeCondition(userId, budgetMonth, currency))
            .orderBy(Tables.BUDGET_PLANS.REVISION.desc())
            .limit(1)
            .forUpdate()
            .fetchOne(::toEntity)

    fun insertOrFindByIdempotencyKey(plan: BudgetPlan): BudgetPlan =
        dsl.insertInto(Tables.BUDGET_PLANS)
            .set(Tables.BUDGET_PLANS.ID, plan.id)
            .set(Tables.BUDGET_PLANS.USER_ID, plan.userId)
            .set(Tables.BUDGET_PLANS.PREVIOUS_PLAN_ID, plan.previousPlanId)
            .set(Tables.BUDGET_PLANS.BASELINE_BUDGET_ID, plan.baselineBudgetId)
            .set(Tables.BUDGET_PLANS.APPLIED_BUDGET_ID, plan.appliedBudgetId)
            .set(Tables.BUDGET_PLANS.BUDGET_MONTH, plan.budgetMonth)
            .set(Tables.BUDGET_PLANS.CURRENCY, plan.currency.name)
            .set(Tables.BUDGET_PLANS.REVISION, plan.revision)
            .set(Tables.BUDGET_PLANS.VERSION, plan.version)
            .set(Tables.BUDGET_PLANS.STATUS, plan.status.name)
            .set(Tables.BUDGET_PLANS.IDEMPOTENCY_KEY, plan.idempotencyKey)
            .set(Tables.BUDGET_PLANS.CREATED_AT, plan.createdAt)
            .set(Tables.BUDGET_PLANS.UPDATED_AT, plan.updatedAt)
            .set(Tables.BUDGET_PLANS.APPLIED_AT, plan.appliedAt)
            .set(Tables.BUDGET_PLANS.SUPERSEDED_AT, plan.supersededAt)
            .set(Tables.BUDGET_PLANS.CANCELLED_AT, plan.cancelledAt)
            .onConflict(Tables.BUDGET_PLANS.USER_ID, Tables.BUDGET_PLANS.IDEMPOTENCY_KEY)
            .doNothing()
            .returning()
            .fetchOne(::toEntity)
            ?: requireNotNull(findByUserIdAndIdempotencyKey(plan.userId, plan.idempotencyKey))

    fun updateVersion(plan: BudgetPlan): BudgetPlan? =
        dsl.update(Tables.BUDGET_PLANS)
            .set(Tables.BUDGET_PLANS.VERSION, plan.version + 1)
            .set(Tables.BUDGET_PLANS.UPDATED_AT, plan.updatedAt)
            .where(
                Tables.BUDGET_PLANS.ID.eq(plan.id)
                    .and(Tables.BUDGET_PLANS.USER_ID.eq(plan.userId))
                    .and(Tables.BUDGET_PLANS.VERSION.eq(plan.version)),
            )
            .returning()
            .fetchOne(::toEntity)

    private fun selectByScope(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ) =
        dsl.selectFrom(Tables.BUDGET_PLANS)
            .where(scopeCondition(userId, budgetMonth, currency))

    private fun scopeCondition(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ) =
        Tables.BUDGET_PLANS.USER_ID.eq(userId)
            .and(Tables.BUDGET_PLANS.BUDGET_MONTH.eq(budgetMonth))
            .and(Tables.BUDGET_PLANS.CURRENCY.eq(currency.name))

    private fun toEntity(record: Record): BudgetPlan =
        BudgetPlan(
            id = requireNotNull(record[Tables.BUDGET_PLANS.ID]),
            userId = requireNotNull(record[Tables.BUDGET_PLANS.USER_ID]),
            previousPlanId = record[Tables.BUDGET_PLANS.PREVIOUS_PLAN_ID],
            baselineBudgetId = record[Tables.BUDGET_PLANS.BASELINE_BUDGET_ID],
            appliedBudgetId = record[Tables.BUDGET_PLANS.APPLIED_BUDGET_ID],
            budgetMonth = requireNotNull(record[Tables.BUDGET_PLANS.BUDGET_MONTH]),
            currency = Currency.valueOf(requireNotNull(record[Tables.BUDGET_PLANS.CURRENCY])),
            revision = requireNotNull(record[Tables.BUDGET_PLANS.REVISION]),
            version = requireNotNull(record[Tables.BUDGET_PLANS.VERSION]),
            status = BudgetPlanStatus.valueOf(requireNotNull(record[Tables.BUDGET_PLANS.STATUS])),
            idempotencyKey = requireNotNull(record[Tables.BUDGET_PLANS.IDEMPOTENCY_KEY]),
            createdAt = requireNotNull(record[Tables.BUDGET_PLANS.CREATED_AT]),
            updatedAt = requireNotNull(record[Tables.BUDGET_PLANS.UPDATED_AT]),
            appliedAt = record[Tables.BUDGET_PLANS.APPLIED_AT],
            supersededAt = record[Tables.BUDGET_PLANS.SUPERSEDED_AT],
            cancelledAt = record[Tables.BUDGET_PLANS.CANCELLED_AT],
        )
}

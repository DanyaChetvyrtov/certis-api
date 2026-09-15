package ru.digitalhustle.certis.features.budget.query.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetFeasibilityStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanConstraintSnapshot
import ru.digitalhustle.certis.features.budget.model.BudgetPlanForecastSnapshot
import ru.digitalhustle.certis.features.budget.model.BudgetPlanOptimizationSnapshot
import ru.digitalhustle.certis.features.budget.model.BudgetPlanStateSnapshot
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.util.UUID

@Repository
class BudgetPlanQueryRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): BudgetPlan? =
        dsl.selectFrom(Tables.BUDGET_PLANS)
            .where(
                Tables.BUDGET_PLANS.ID.eq(id)
                    .and(Tables.BUDGET_PLANS.USER_ID.eq(userId)),
            )
            .fetchOne(::toPlan)

    fun findCurrentByUserIdAndScope(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): BudgetPlan? =
        dsl.selectFrom(Tables.BUDGET_PLANS)
            .where(
                scopeCondition(userId, budgetMonth, currency)
                    .and(
                        Tables.BUDGET_PLANS.STATUS.`in`(
                            BudgetPlanStatus.DRAFT.name,
                            BudgetPlanStatus.APPLIED.name,
                        ),
                    ),
            )
            .orderBy(
                Tables.BUDGET_PLANS.STATUS.eq(BudgetPlanStatus.DRAFT.name).desc(),
                Tables.BUDGET_PLANS.REVISION.desc(),
            )
            .limit(1)
            .fetchOne(::toPlan)

    fun findAllByUserIdAndScope(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): List<BudgetPlan> =
        dsl.selectFrom(Tables.BUDGET_PLANS)
            .where(scopeCondition(userId, budgetMonth, currency))
            .orderBy(Tables.BUDGET_PLANS.REVISION.desc())
            .fetch(::toPlan)

    fun findStatesByPlanIds(planIds: Collection<UUID>): Map<UUID, BudgetPlanStateSnapshot> {
        if (planIds.isEmpty()) {
            return emptyMap()
        }

        val forecasts = findLatestForecasts(planIds)
        val constraints = findLatestConstraints(planIds)
        val optimizations = findLatestOptimizations(planIds)

        return planIds.associateWith { planId ->
            BudgetPlanStateSnapshot(
                forecast = forecasts[planId],
                constraints = constraints[planId],
                optimization = optimizations[planId],
            )
        }
    }

    private fun findLatestForecasts(planIds: Collection<UUID>): Map<UUID, BudgetPlanForecastSnapshot> {
        val forecast = Tables.BUDGET_FORECAST_REVISIONS
        val newerForecast = Tables.BUDGET_FORECAST_REVISIONS.`as`("newer_forecast")
        val item = Tables.BUDGET_FORECAST_ITEMS
        val includedItemCount = DSL.count().filterWhere(item.INCLUDED.eq(true)).`as`("included_item_count")
        val excludedItemCount = DSL.count().filterWhere(item.INCLUDED.eq(false)).`as`("excluded_item_count")

        return dsl.select(*forecast.fields(), includedItemCount, excludedItemCount)
            .from(forecast)
            .leftJoin(item)
            .on(
                item.FORECAST_REVISION_ID.eq(forecast.ID)
                    .and(item.USER_ID.eq(forecast.USER_ID)),
            )
            .where(
                forecast.PLAN_ID.`in`(planIds)
                    .and(
                        DSL.notExists(
                            dsl.selectOne()
                                .from(newerForecast)
                                .where(
                                    newerForecast.PLAN_ID.eq(forecast.PLAN_ID)
                                        .and(newerForecast.REVISION.gt(forecast.REVISION)),
                                ),
                        ),
                    ),
            )
            .groupBy(*forecast.fields())
            .fetch { record ->
                requireNotNull(record[forecast.PLAN_ID]) to BudgetPlanForecastSnapshot(
                    revision = requireNotNull(record[forecast.REVISION]),
                    forecastIncome = requireNotNull(record[forecast.FORECAST_INCOME]),
                    recurringExpenses = requireNotNull(record[forecast.RECURRING_EXPENSES]),
                    flexibleEstimate = requireNotNull(record[forecast.FLEXIBLE_ESTIMATE]),
                    forecastExpenses = requireNotNull(record[forecast.FORECAST_EXPENSES]),
                    forecastSavings = requireNotNull(record[forecast.FORECAST_SAVINGS]),
                    includedItemCount = record[includedItemCount] ?: 0,
                    excludedItemCount = record[excludedItemCount] ?: 0,
                )
            }
            .toMap()
    }

    private fun findLatestConstraints(planIds: Collection<UUID>): Map<UUID, BudgetPlanConstraintSnapshot> {
        val constraints = Tables.BUDGET_CONSTRAINT_REVISIONS
        val newerConstraints = Tables.BUDGET_CONSTRAINT_REVISIONS.`as`("newer_constraints")

        return dsl.selectFrom(constraints)
            .where(
                constraints.PLAN_ID.`in`(planIds)
                    .and(
                        DSL.notExists(
                            dsl.selectOne()
                                .from(newerConstraints)
                                .where(
                                    newerConstraints.PLAN_ID.eq(constraints.PLAN_ID)
                                        .and(newerConstraints.REVISION.gt(constraints.REVISION)),
                                ),
                        ),
                    ),
            )
            .fetch { record ->
                requireNotNull(record[constraints.PLAN_ID]) to BudgetPlanConstraintSnapshot(
                    revision = requireNotNull(record[constraints.REVISION]),
                    status = BudgetFeasibilityStatus.valueOf(
                        requireNotNull(record[constraints.FEASIBILITY_STATUS]),
                    ),
                    savingsFloorAmount = requireNotNull(record[constraints.SAVINGS_FLOOR_AMOUNT]),
                    requiredAmount = requireNotNull(record[constraints.REQUIRED_AMOUNT]),
                    variableMinimumAmount = requireNotNull(record[constraints.VARIABLE_MINIMUM_AMOUNT]),
                    maximumSavingsAmount = requireNotNull(record[constraints.MAXIMUM_SAVINGS_AMOUNT]),
                    shortfall = requireNotNull(record[constraints.SHORTFALL_AMOUNT]),
                    violations = objectMapper.readValue(
                        requireNotNull(record[constraints.VIOLATIONS]).data(),
                        object : TypeReference<List<BudgetPlanningViolation>>() {},
                    ),
                )
            }
            .toMap()
    }

    private fun findLatestOptimizations(planIds: Collection<UUID>): Map<UUID, BudgetPlanOptimizationSnapshot> {
        val optimization = Tables.BUDGET_OPTIMIZATION_RUNS
        val newerOptimization = Tables.BUDGET_OPTIMIZATION_RUNS.`as`("newer_optimization")

        return dsl.selectFrom(optimization)
            .where(
                optimization.PLAN_ID.`in`(planIds)
                    .and(
                        DSL.notExists(
                            dsl.selectOne()
                                .from(newerOptimization)
                                .where(
                                    newerOptimization.PLAN_ID.eq(optimization.PLAN_ID)
                                        .and(
                                            newerOptimization.CREATED_AT.gt(optimization.CREATED_AT)
                                                .or(
                                                    newerOptimization.CREATED_AT.eq(optimization.CREATED_AT)
                                                        .and(newerOptimization.ID.gt(optimization.ID)),
                                                ),
                                        ),
                                ),
                        ),
                    ),
            )
            .fetch { record ->
                requireNotNull(record[optimization.PLAN_ID]) to BudgetPlanOptimizationSnapshot(
                    id = requireNotNull(record[optimization.ID]),
                    status = BudgetOptimizationRunStatus.valueOf(
                        requireNotNull(record[optimization.STATUS]),
                    ),
                    targetSavingsAmount = requireNotNull(record[optimization.TARGET_SAVINGS_AMOUNT]),
                    actualSavingsAmount = record[optimization.ACTUAL_SAVINGS_AMOUNT],
                    createdAt = requireNotNull(record[optimization.CREATED_AT]),
                )
            }
            .toMap()
    }

    private fun scopeCondition(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ) =
        Tables.BUDGET_PLANS.USER_ID.eq(userId)
            .and(Tables.BUDGET_PLANS.BUDGET_MONTH.eq(budgetMonth))
            .and(Tables.BUDGET_PLANS.CURRENCY.eq(currency.name))

    private fun toPlan(record: Record): BudgetPlan =
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

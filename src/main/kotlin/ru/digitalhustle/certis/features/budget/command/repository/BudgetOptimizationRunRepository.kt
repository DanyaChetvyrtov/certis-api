package ru.digitalhustle.certis.features.budget.command.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BudgetOptimizationRunRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun insert(run: BudgetPlanningOptimizationRun) {
        val input = run.input
        val result = run.result
        dsl.insertInto(Tables.BUDGET_OPTIMIZATION_RUNS)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.ID, run.id)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID, run.userId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID, run.planId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.FORECAST_REVISION_ID, run.forecastRevisionId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.CONSTRAINT_REVISION_ID, run.constraintRevisionId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.ALGORITHM_VERSION, run.algorithmVersion)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.OBJECTIVE_CODE, OBJECTIVE_CODE)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS, run.status.name)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.GENERATION_IDEMPOTENCY_KEY, run.generationIdempotencyKey)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.APPLY_IDEMPOTENCY_KEY, run.applyIdempotencyKey)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.INPUT_FINGERPRINT, run.inputFingerprint)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.FORECAST_INCOME_AMOUNT, input.forecastIncome)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.TARGET_SAVINGS_AMOUNT, input.targetSavingsAmount)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.REQUIRED_AMOUNT, input.requiredAmount)
            .set(
                Tables.BUDGET_OPTIMIZATION_RUNS.VARIABLE_MINIMUM_AMOUNT,
                input.forecastIncome - input.requiredAmount - input.maximumSavingsAmount,
            )
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.VARIABLE_CAPACITY_AMOUNT, input.flexibleCapacity)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.MAXIMUM_SAVINGS_AMOUNT, input.maximumSavingsAmount)
            .set(
                Tables.BUDGET_OPTIMIZATION_RUNS.BASELINE_SAVINGS_AMOUNT,
                input.baselineSavingsAmount,
            )
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.SELECTED_VARIABLE_AMOUNT, result?.flexibleAllocation)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.TOTAL_ALLOCATION_AMOUNT, result?.totalAllocation)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.ACTUAL_SAVINGS_AMOUNT, result?.actualSavings)
            .set(
                Tables.BUDGET_OPTIMIZATION_RUNS.ADDITIONAL_SAVINGS_AMOUNT,
                result?.additionalSavingsComparedWithCurrent,
            )
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.UNUSED_CAPACITY_AMOUNT, result?.unusedCapacity)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.WEIGHTED_COVERAGE_SCORE, result?.coverageScore)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.OBJECTIVE_VALUE, result?.objectiveValue)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.INPUT_SNAPSHOT, json(input))
            .set(
                Tables.BUDGET_OPTIMIZATION_RUNS.RESULT_SNAPSHOT,
                json(mapOf("constraintChecks" to run.constraintChecks)),
            )
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.VIOLATIONS, json(run.violations))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.CREATED_AT, run.createdAt)
            .execute()
    }

    fun markGeneratedStale(planId: UUID, userId: UUID, staleAt: OffsetDateTime) {
        dsl.update(Tables.BUDGET_OPTIMIZATION_RUNS)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS, BudgetOptimizationRunStatus.STALE.name)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STALE_AT, staleAt)
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID.eq(planId)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS.eq(BudgetOptimizationRunStatus.GENERATED.name)),
            )
            .execute()
    }

    fun markStale(id: UUID, planId: UUID, userId: UUID, staleAt: OffsetDateTime): Boolean =
        dsl.update(Tables.BUDGET_OPTIMIZATION_RUNS)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS, BudgetOptimizationRunStatus.STALE.name)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STALE_AT, staleAt)
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.ID.eq(id)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID.eq(planId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS.eq(BudgetOptimizationRunStatus.GENERATED.name)),
            )
            .execute() == 1

    fun dismiss(id: UUID, planId: UUID, userId: UUID, dismissedAt: OffsetDateTime): Boolean =
        dsl.update(Tables.BUDGET_OPTIMIZATION_RUNS)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS, BudgetOptimizationRunStatus.DISMISSED.name)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.DISMISSED_AT, dismissedAt)
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.ID.eq(id)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID.eq(planId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS.eq(BudgetOptimizationRunStatus.GENERATED.name)),
            )
            .execute() == 1

    fun apply(
        id: UUID,
        planId: UUID,
        userId: UUID,
        idempotencyKey: String,
        appliedAt: OffsetDateTime,
    ): Boolean =
        dsl.update(Tables.BUDGET_OPTIMIZATION_RUNS)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS, BudgetOptimizationRunStatus.APPLIED.name)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.APPLY_IDEMPOTENCY_KEY, idempotencyKey)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.APPLIED_AT, appliedAt)
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.ID.eq(id)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID.eq(planId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS.eq(BudgetOptimizationRunStatus.GENERATED.name)),
            )
            .execute() == 1

    private fun json(value: Any): JSONB = JSONB.valueOf(objectMapper.writeValueAsString(value))

    companion object {
        private const val OBJECTIVE_CODE = "MAXIMIZE_WEIGHTED_COVERAGE"
    }
}

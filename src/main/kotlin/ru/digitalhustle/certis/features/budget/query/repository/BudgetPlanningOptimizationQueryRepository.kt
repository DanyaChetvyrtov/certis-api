package ru.digitalhustle.certis.features.budget.query.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningConstraintCheck
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationDecision
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationInput
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationReason
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationResult
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.util.UUID

@Repository
class BudgetPlanningOptimizationQueryRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun findLatest(planId: UUID, userId: UUID): BudgetPlanningOptimizationRun? =
        baseSelect()
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID.eq(planId)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId)),
            )
            .orderBy(
                Tables.BUDGET_OPTIMIZATION_RUNS.CREATED_AT.desc(),
                Tables.BUDGET_OPTIMIZATION_RUNS.ID.desc(),
            )
            .limit(1)
            .fetchOne(::toRun)

    fun findById(planId: UUID, optimizationId: UUID, userId: UUID): BudgetPlanningOptimizationRun? =
        baseSelect()
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.ID.eq(optimizationId)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID.eq(planId))
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId)),
            )
            .fetchOne(::toRun)

    fun findByGenerationKey(userId: UUID, idempotencyKey: String): BudgetPlanningOptimizationRun? =
        baseSelect()
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.GENERATION_IDEMPOTENCY_KEY.eq(idempotencyKey)),
            )
            .fetchOne(::toRun)

    fun findByApplyKey(userId: UUID, idempotencyKey: String): BudgetPlanningOptimizationRun? =
        baseSelect()
            .where(
                Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID.eq(userId)
                    .and(Tables.BUDGET_OPTIMIZATION_RUNS.APPLY_IDEMPOTENCY_KEY.eq(idempotencyKey)),
            )
            .fetchOne(::toRun)

    private fun baseSelect() =
        dsl.select(*Tables.BUDGET_OPTIMIZATION_RUNS.fields(), Tables.BUDGET_PLANS.VERSION)
            .from(Tables.BUDGET_OPTIMIZATION_RUNS)
            .join(Tables.BUDGET_PLANS)
            .on(
                Tables.BUDGET_PLANS.ID.eq(Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID)
                    .and(Tables.BUDGET_PLANS.USER_ID.eq(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID)),
            )

    private fun toRun(record: Record): BudgetPlanningOptimizationRun {
        val runId = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.ID])
        val userId = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID])
        val status = BudgetOptimizationRunStatus.valueOf(
            requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.STATUS]),
        )
        return BudgetPlanningOptimizationRun(
            id = runId,
            userId = userId,
            planId = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID]),
            forecastRevisionId = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.FORECAST_REVISION_ID]),
            constraintRevisionId = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.CONSTRAINT_REVISION_ID]),
            status = status,
            algorithmVersion = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.ALGORITHM_VERSION]),
            generationIdempotencyKey = requireNotNull(
                record[Tables.BUDGET_OPTIMIZATION_RUNS.GENERATION_IDEMPOTENCY_KEY],
            ),
            applyIdempotencyKey = record[Tables.BUDGET_OPTIMIZATION_RUNS.APPLY_IDEMPOTENCY_KEY],
            inputFingerprint = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.INPUT_FINGERPRINT]),
            input = readInput(record),
            result = if (status == BudgetOptimizationRunStatus.INFEASIBLE) null else readResult(record),
            decisions = findDecisions(runId, userId),
            constraintChecks = readChecks(record),
            violations = readViolations(record),
            planVersion = requireNotNull(record[Tables.BUDGET_PLANS.VERSION]),
            createdAt = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.CREATED_AT]),
            staleAt = record[Tables.BUDGET_OPTIMIZATION_RUNS.STALE_AT],
            dismissedAt = record[Tables.BUDGET_OPTIMIZATION_RUNS.DISMISSED_AT],
            appliedAt = record[Tables.BUDGET_OPTIMIZATION_RUNS.APPLIED_AT],
        )
    }

    private fun readInput(record: Record): BudgetPlanningOptimizationInput =
        objectMapper.readValue(
            requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.INPUT_SNAPSHOT]).data(),
            BudgetPlanningOptimizationInput::class.java,
        )

    private fun readResult(record: Record): BudgetPlanningOptimizationResult =
        BudgetPlanningOptimizationResult(
            requiredAllocation = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.REQUIRED_AMOUNT]),
            flexibleAllocation = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.SELECTED_VARIABLE_AMOUNT]),
            totalAllocation = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.TOTAL_ALLOCATION_AMOUNT]),
            targetSavings = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.TARGET_SAVINGS_AMOUNT]),
            actualSavings = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.ACTUAL_SAVINGS_AMOUNT]),
            additionalSavingsComparedWithCurrent = requireNotNull(
                record[Tables.BUDGET_OPTIMIZATION_RUNS.ADDITIONAL_SAVINGS_AMOUNT],
            ),
            coverageScore = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.WEIGHTED_COVERAGE_SCORE]),
            objectiveValue = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.OBJECTIVE_VALUE]),
            unusedCapacity = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.UNUSED_CAPACITY_AMOUNT]),
        )

    private fun readChecks(record: Record): List<BudgetPlanningConstraintCheck> {
        val snapshot = objectMapper.readTree(
            requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.RESULT_SNAPSHOT]).data(),
        )
        return objectMapper.convertValue(
            snapshot.path("constraintChecks"),
            object : TypeReference<List<BudgetPlanningConstraintCheck>>() {},
        )
    }

    private fun readViolations(record: Record): List<BudgetPlanningViolation> =
        objectMapper.readValue(
            requireNotNull(record[Tables.BUDGET_OPTIMIZATION_RUNS.VIOLATIONS]).data(),
            object : TypeReference<List<BudgetPlanningViolation>>() {},
        )

    private fun findDecisions(runId: UUID, userId: UUID): List<BudgetPlanningOptimizationDecision> =
        dsl.select(
            *Tables.BUDGET_OPTIMIZATION_DECISIONS.fields(),
            Tables.BUDGET_CATEGORY_CONSTRAINTS.CATEGORY_TYPE,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.BUDGET_OPTIMIZATION_DECISIONS)
            .join(Tables.BUDGET_CATEGORY_CONSTRAINTS)
            .on(
                Tables.BUDGET_CATEGORY_CONSTRAINTS.ID.eq(
                    Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_CONSTRAINT_ID,
                ),
            )
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.BUDGET_OPTIMIZATION_DECISIONS.USER_ID)),
            )
            .where(
                Tables.BUDGET_OPTIMIZATION_DECISIONS.OPTIMIZATION_RUN_ID.eq(runId)
                    .and(Tables.BUDGET_OPTIMIZATION_DECISIONS.USER_ID.eq(userId)),
            )
            .orderBy(Tables.CATEGORIES.NAME.asc())
            .fetch(::toDecision)

    private fun toDecision(record: Record): BudgetPlanningOptimizationDecision =
        BudgetPlanningOptimizationDecision(
            id = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.ID]),
            categoryConstraintId = requireNotNull(
                record[Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_CONSTRAINT_ID],
            ),
            fundingLevelId = record[Tables.BUDGET_OPTIMIZATION_DECISIONS.FUNDING_LEVEL_ID],
            category = BudgetForecastCategory(
                id = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_ID]),
                type = CategoryType.valueOf(
                    requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.CATEGORY_TYPE]),
                ),
                name = requireNotNull(record[Tables.CATEGORIES.NAME]),
                icon = requireNotNull(record[Tables.CATEGORIES.ICON]),
                color = requireNotNull(record[Tables.CATEGORIES.COLOR]),
            ),
            allocationType = BudgetAllocationType.valueOf(
                requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.ALLOCATION_TYPE]),
            ),
            constraintRole = BudgetConstraintRole.valueOf(
                requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.CONSTRAINT_ROLE]),
            ),
            priority = record[Tables.BUDGET_OPTIMIZATION_DECISIONS.PRIORITY]?.let(BudgetPriority::valueOf),
            currentLimit = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.CURRENT_LIMIT_AMOUNT]),
            requiredAmount = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.REQUIRED_AMOUNT]),
            selectedLevel = record[Tables.BUDGET_OPTIMIZATION_DECISIONS.SELECTED_LEVEL]
                ?.let(BudgetFundingLevel::valueOf),
            recommendedLimit = requireNotNull(
                record[Tables.BUDGET_OPTIMIZATION_DECISIONS.RECOMMENDED_LIMIT_AMOUNT],
            ),
            change = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.CHANGE_AMOUNT]),
            coverage = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.COVERAGE]),
            optionValue = record[Tables.BUDGET_OPTIMIZATION_DECISIONS.OPTION_VALUE],
            reason = BudgetPlanningOptimizationReason(
                code = requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.REASON_CODE]),
                parameters = objectMapper.readValue(
                    requireNotNull(record[Tables.BUDGET_OPTIMIZATION_DECISIONS.REASON_PARAMETERS]).data(),
                    object : TypeReference<Map<String, Any?>>() {},
                ),
            ),
        )
}

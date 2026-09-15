package ru.digitalhustle.certis.integrations

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jooq.JSONB
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessException
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BudgetPlanningSchemaTest : AbstractIntegrationTest() {

    private companion object {
        private const val SHA_256 = "sha256:0000000000000000000000000000000000000000000000000000000000000000"
        private val BUDGET_MONTH: LocalDate = LocalDate.parse("2026-10-01")
    }

    @Test
    fun `should install the complete budget planning schema`() {
        val expectedTables = setOf(
            "budget_plans",
            "budget_forecast_revisions",
            "budget_forecast_items",
            "budget_constraint_revisions",
            "budget_category_constraints",
            "budget_constraint_funding_levels",
            "budget_optimization_runs",
            "budget_optimization_decisions",
        )

        val actualTables = dsl.fetch(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'keeper'
            """.trimIndent(),
        ).map { record -> requireNotNull(record.get("table_name", String::class.java)) }.toSet()

        assertThat(actualTables).containsAll(expectedTables)
    }

    @Test
    fun `should scope budgets and active plans by month and currency`() {
        val user = userFixture.createInDb()
        val now = OffsetDateTime.now()

        insertBudget(user.id, Currency.RUB, now)
        insertBudget(user.id, Currency.USD, now)

        assertThat(
            dsl.fetchCount(
                Tables.BUDGETS,
                Tables.BUDGETS.USER_ID.eq(user.id)
                    .and(Tables.BUDGETS.BUDGET_MONTH.eq(BUDGET_MONTH)),
            ),
        ).isEqualTo(2)

        val firstPlanId = UUID.randomUUID()
        insertDraftPlan(firstPlanId, user.id, Currency.RUB, 1, "create-plan-1", now)

        assertThatThrownBy {
            insertDraftPlan(UUID.randomUUID(), user.id, Currency.RUB, 2, "create-plan-2", now)
        }.isInstanceOf(DataAccessException::class.java)

        dsl.update(Tables.BUDGET_PLANS)
            .set(Tables.BUDGET_PLANS.STATUS, "CANCELLED")
            .set(Tables.BUDGET_PLANS.CANCELLED_AT, now)
            .set(Tables.BUDGET_PLANS.UPDATED_AT, now)
            .set(Tables.BUDGET_PLANS.VERSION, Tables.BUDGET_PLANS.VERSION.plus(1))
            .where(
                Tables.BUDGET_PLANS.ID.eq(firstPlanId)
                    .and(Tables.BUDGET_PLANS.USER_ID.eq(user.id)),
            )
            .execute()

        insertDraftPlan(UUID.randomUUID(), user.id, Currency.RUB, 2, "create-plan-2", now)
        insertDraftPlan(UUID.randomUUID(), user.id, Currency.USD, 1, "create-plan-usd", now)
    }

    @Test
    fun `should persist an auditable mckp result and enforce canonical option coverage`() {
        val user = userFixture.createInDb()
        val now = OffsetDateTime.now()
        val planId = UUID.randomUUID()
        val forecastId = UUID.randomUUID()
        val constraintRevisionId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val categoryConstraintId = UUID.randomUUID()

        insertDraftPlan(planId, user.id, Currency.RUB, 1, "create-mckp-plan", now)
        insertExpenseCategory(categoryId, user.id)
        insertForecast(forecastId, planId, user.id, now)
        insertConstraints(constraintRevisionId, forecastId, planId, user.id, now)
        insertCategoryConstraint(categoryConstraintId, constraintRevisionId, categoryId, user.id)

        assertNonCanonicalCoverageRejected(categoryConstraintId, user.id)
        val selectedFundingLevelId = insertCanonicalFundingLevels(categoryConstraintId, user.id)

        val optimizationRunId = UUID.randomUUID()
        insertOptimizationRun(
            optimizationRunId,
            planId,
            forecastId,
            constraintRevisionId,
            user.id,
            now,
        )
        insertOptimizationDecision(
            optimizationRunId,
            constraintRevisionId,
            categoryConstraintId,
            categoryId,
            selectedFundingLevelId,
            user.id,
        )

        assertSecondGeneratedRunRejected(planId, forecastId, constraintRevisionId, user.id, now)
    }

    private fun assertNonCanonicalCoverageRejected(
        categoryConstraintId: UUID,
        userId: UUID,
    ) {
        assertThatThrownBy {
            insertFundingLevel(
                UUID.randomUUID(),
                categoryConstraintId,
                userId,
                "MINIMUM",
                "15000.00",
                "0.700000",
            )
        }.isInstanceOf(DataAccessException::class.java)
    }

    private fun insertCanonicalFundingLevels(
        categoryConstraintId: UUID,
        userId: UUID,
    ): UUID {
        insertFundingLevel(
            UUID.randomUUID(),
            categoryConstraintId,
            userId,
            "MINIMUM",
            "15000.00",
            "0.600000",
        )
        val selectedFundingLevelId = UUID.randomUUID()
        insertFundingLevel(
            selectedFundingLevelId,
            categoryConstraintId,
            userId,
            "BALANCED",
            "54700.00",
            "0.850000",
        )
        insertFundingLevel(
            UUID.randomUUID(),
            categoryConstraintId,
            userId,
            "COMFORTABLE",
            "75000.00",
            "1.000000",
        )
        return selectedFundingLevelId
    }

    private fun assertSecondGeneratedRunRejected(
        planId: UUID,
        forecastId: UUID,
        constraintRevisionId: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ) {
        assertThatThrownBy {
            insertOptimizationRun(
                UUID.randomUUID(),
                planId,
                forecastId,
                constraintRevisionId,
                userId,
                now,
            )
        }.isInstanceOf(DataAccessException::class.java)
    }

    private fun insertBudget(
        userId: UUID,
        currency: Currency,
        now: OffsetDateTime,
    ) {
        dsl.insertInto(Tables.BUDGETS)
            .set(Tables.BUDGETS.ID, UUID.randomUUID())
            .set(Tables.BUDGETS.USER_ID, userId)
            .set(Tables.BUDGETS.BUDGET_MONTH, BUDGET_MONTH)
            .set(Tables.BUDGETS.PLANNED_INCOME, BigDecimal("185000.00"))
            .set(Tables.BUDGETS.SAVINGS_TARGET, BigDecimal("30000.00"))
            .set(Tables.BUDGETS.CURRENCY, currency.name)
            .set(Tables.BUDGETS.CREATED_AT, now)
            .set(Tables.BUDGETS.UPDATED_AT, now)
            .execute()
    }

    private fun insertDraftPlan(
        id: UUID,
        userId: UUID,
        currency: Currency,
        revision: Int,
        idempotencyKey: String,
        now: OffsetDateTime,
    ) {
        dsl.insertInto(Tables.BUDGET_PLANS)
            .set(Tables.BUDGET_PLANS.ID, id)
            .set(Tables.BUDGET_PLANS.USER_ID, userId)
            .set(Tables.BUDGET_PLANS.BUDGET_MONTH, BUDGET_MONTH)
            .set(Tables.BUDGET_PLANS.CURRENCY, currency.name)
            .set(Tables.BUDGET_PLANS.REVISION, revision)
            .set(Tables.BUDGET_PLANS.VERSION, 0L)
            .set(Tables.BUDGET_PLANS.STATUS, "DRAFT")
            .set(Tables.BUDGET_PLANS.IDEMPOTENCY_KEY, idempotencyKey)
            .set(Tables.BUDGET_PLANS.CREATED_AT, now)
            .set(Tables.BUDGET_PLANS.UPDATED_AT, now)
            .execute()
    }

    private fun insertExpenseCategory(
        id: UUID,
        userId: UUID,
    ) {
        dsl.insertInto(Tables.CATEGORIES)
            .set(Tables.CATEGORIES.ID, id)
            .set(Tables.CATEGORIES.USER_ID, userId)
            .set(Tables.CATEGORIES.NAME, "Groceries")
            .set(Tables.CATEGORIES.TYPE, "EXPENSE")
            .set(Tables.CATEGORIES.ICON, "cart")
            .set(Tables.CATEGORIES.COLOR, "#10B981")
            .execute()
    }

    private fun insertForecast(
        id: UUID,
        planId: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ) {
        dsl.insertInto(Tables.BUDGET_FORECAST_REVISIONS)
            .set(Tables.BUDGET_FORECAST_REVISIONS.ID, id)
            .set(Tables.BUDGET_FORECAST_REVISIONS.USER_ID, userId)
            .set(Tables.BUDGET_FORECAST_REVISIONS.PLAN_ID, planId)
            .set(Tables.BUDGET_FORECAST_REVISIONS.REVISION, 1)
            .set(Tables.BUDGET_FORECAST_REVISIONS.SOURCE_FINGERPRINT, SHA_256)
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_FINGERPRINT, SHA_256)
            .set(Tables.BUDGET_FORECAST_REVISIONS.HISTORY_MONTHS_USED, 6.toShort())
            .set(Tables.BUDGET_FORECAST_REVISIONS.HISTORY_METHOD, "MEDIAN_6M")
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_INCOME, BigDecimal("185000.00"))
            .set(Tables.BUDGET_FORECAST_REVISIONS.RECURRING_INCOME, BigDecimal("185000.00"))
            .set(Tables.BUDGET_FORECAST_REVISIONS.RECURRING_EXPENSES, BigDecimal("82000.00"))
            .set(Tables.BUDGET_FORECAST_REVISIONS.FLEXIBLE_ESTIMATE, BigDecimal("56200.00"))
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_EXPENSES, BigDecimal("138200.00"))
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_SAVINGS, BigDecimal("46800.00"))
            .set(Tables.BUDGET_FORECAST_REVISIONS.SOURCE_SNAPSHOT, JSONB.jsonb("{}"))
            .set(Tables.BUDGET_FORECAST_REVISIONS.CONFIRMED_AT, now)
            .execute()
    }

    private fun insertConstraints(
        id: UUID,
        forecastId: UUID,
        planId: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ) {
        dsl.insertInto(Tables.BUDGET_CONSTRAINT_REVISIONS)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.ID, id)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.USER_ID, userId)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.PLAN_ID, planId)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.FORECAST_REVISION_ID, forecastId)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.REVISION, 1)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.CONSTRAINT_FINGERPRINT, SHA_256)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.TARGET_SAVINGS_AMOUNT, BigDecimal("30500.00"))
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.FIXED_REQUIRED_AMOUNT, BigDecimal("82000.00"))
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.VARIABLE_MINIMUM_AMOUNT, BigDecimal("15000.00"))
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.MAXIMUM_SAVINGS_AMOUNT, BigDecimal("88000.00"))
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.FEASIBILITY_STATUS, "FEASIBLE")
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.SHORTFALL_AMOUNT, BigDecimal.ZERO)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.VIOLATIONS, JSONB.jsonb("[]"))
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.CREATED_AT, now)
            .execute()
    }

    private fun insertCategoryConstraint(
        id: UUID,
        constraintRevisionId: UUID,
        categoryId: UUID,
        userId: UUID,
    ) {
        dsl.insertInto(Tables.BUDGET_CATEGORY_CONSTRAINTS)
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.ID, id)
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.USER_ID, userId)
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CONSTRAINT_REVISION_ID, constraintRevisionId)
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CATEGORY_ID, categoryId)
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.ALLOCATION_TYPE, "VARIABLE")
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CONSTRAINT_ROLE, "FLEXIBLE")
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.PRIORITY, "HIGH")
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CURRENT_LIMIT_AMOUNT, BigDecimal("56200.00"))
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.MINIMUM_AMOUNT, BigDecimal("15000.00"))
            .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.SOURCE_KEYS, JSONB.jsonb("[]"))
            .execute()
    }

    private fun insertFundingLevel(
        id: UUID,
        categoryConstraintId: UUID,
        userId: UUID,
        level: String,
        amount: String,
        coverage: String,
    ) {
        dsl.insertInto(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS)
            .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.ID, id)
            .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.USER_ID, userId)
            .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.CATEGORY_CONSTRAINT_ID, categoryConstraintId)
            .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.LEVEL, level)
            .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.AMOUNT, BigDecimal(amount))
            .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.COVERAGE, BigDecimal(coverage))
            .execute()
    }

    private fun insertOptimizationRun(
        id: UUID,
        planId: UUID,
        forecastId: UUID,
        constraintRevisionId: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ) {
        dsl.insertInto(Tables.BUDGET_OPTIMIZATION_RUNS)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.ID, id)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.USER_ID, userId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.PLAN_ID, planId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.FORECAST_REVISION_ID, forecastId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.CONSTRAINT_REVISION_ID, constraintRevisionId)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.ALGORITHM_VERSION, "mckp-v1")
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.OBJECTIVE_CODE, "MAXIMIZE_WEIGHTED_COVERAGE")
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS, "GENERATED")
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.GENERATION_IDEMPOTENCY_KEY, "generate-$id")
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.INPUT_FINGERPRINT, SHA_256)
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.FORECAST_INCOME_AMOUNT, BigDecimal("185000.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.TARGET_SAVINGS_AMOUNT, BigDecimal("30500.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.FIXED_REQUIRED_AMOUNT, BigDecimal("82000.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.VARIABLE_MINIMUM_AMOUNT, BigDecimal("15000.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.VARIABLE_CAPACITY_AMOUNT, BigDecimal("72500.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.MAXIMUM_SAVINGS_AMOUNT, BigDecimal("88000.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.BASELINE_SAVINGS_AMOUNT, BigDecimal("43000.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.SELECTED_VARIABLE_AMOUNT, BigDecimal("54700.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.TOTAL_ALLOCATION_AMOUNT, BigDecimal("136700.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.ACTUAL_SAVINGS_AMOUNT, BigDecimal("48300.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.ADDITIONAL_SAVINGS_AMOUNT, BigDecimal("5300.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.UNUSED_CAPACITY_AMOUNT, BigDecimal("17800.00"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.WEIGHTED_COVERAGE_SCORE, BigDecimal("0.85000000"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.OBJECTIVE_VALUE, BigDecimal("2.55000000"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.INPUT_SNAPSHOT, JSONB.jsonb("{}"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.RESULT_SNAPSHOT, JSONB.jsonb("{}"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.VIOLATIONS, JSONB.jsonb("[]"))
            .set(Tables.BUDGET_OPTIMIZATION_RUNS.CREATED_AT, now)
            .execute()
    }

    private fun insertOptimizationDecision(
        runId: UUID,
        constraintRevisionId: UUID,
        categoryConstraintId: UUID,
        categoryId: UUID,
        fundingLevelId: UUID,
        userId: UUID,
    ) {
        dsl.insertInto(Tables.BUDGET_OPTIMIZATION_DECISIONS)
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.ID, UUID.randomUUID())
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.USER_ID, userId)
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.OPTIMIZATION_RUN_ID, runId)
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CONSTRAINT_REVISION_ID, constraintRevisionId)
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_CONSTRAINT_ID, categoryConstraintId)
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_ID, categoryId)
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.FUNDING_LEVEL_ID, fundingLevelId)
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.ALLOCATION_TYPE, "VARIABLE")
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CONSTRAINT_ROLE, "FLEXIBLE")
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.PRIORITY, "HIGH")
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.SELECTED_LEVEL, "BALANCED")
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CURRENT_LIMIT_AMOUNT, BigDecimal("56200.00"))
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.MINIMUM_AMOUNT, BigDecimal("15000.00"))
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.RECOMMENDED_LIMIT_AMOUNT, BigDecimal("54700.00"))
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CHANGE_AMOUNT, BigDecimal("-1500.00"))
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.COVERAGE, BigDecimal("0.850000"))
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.OPTION_VALUE, BigDecimal("2.55000000"))
            .set(
                Tables.BUDGET_OPTIMIZATION_DECISIONS.REASON_CODE,
                "SELECTED_HIGHEST_VALUE_WITHIN_CAPACITY",
            )
            .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.REASON_PARAMETERS, JSONB.jsonb("{}"))
            .execute()
    }
}

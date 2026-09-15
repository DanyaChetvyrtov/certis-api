package ru.digitalhustle.certis.integrations

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Test
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
            dsl.fetchOne(
                """
                SELECT count(*) AS count
                FROM keeper.budgets
                WHERE user_id = ? AND budget_month = ?
                """.trimIndent(),
                user.id,
                BUDGET_MONTH,
            )?.get("count", Long::class.java),
        ).isEqualTo(2L)

        val firstPlanId = UUID.randomUUID()
        insertDraftPlan(firstPlanId, user.id, Currency.RUB, 1, "create-plan-1", now)

        assertThatThrownBy {
            insertDraftPlan(UUID.randomUUID(), user.id, Currency.RUB, 2, "create-plan-2", now)
        }.isInstanceOf(DataAccessException::class.java)

        dsl.execute(
            """
            UPDATE keeper.budget_plans
            SET status = 'CANCELLED',
                cancelled_at = ?,
                updated_at = ?,
                version = version + 1
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
            now,
            now,
            firstPlanId,
            user.id,
        )

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

        assertThatThrownBy {
            insertFundingLevel(
                UUID.randomUUID(),
                categoryConstraintId,
                user.id,
                "MINIMUM",
                "15000.00",
                "0.700000",
            )
        }.isInstanceOf(DataAccessException::class.java)

        insertFundingLevel(
            UUID.randomUUID(),
            categoryConstraintId,
            user.id,
            "MINIMUM",
            "15000.00",
            "0.600000",
        )
        val selectedFundingLevelId = UUID.randomUUID()
        insertFundingLevel(
            selectedFundingLevelId,
            categoryConstraintId,
            user.id,
            "BALANCED",
            "54700.00",
            "0.850000",
        )
        insertFundingLevel(
            UUID.randomUUID(),
            categoryConstraintId,
            user.id,
            "COMFORTABLE",
            "75000.00",
            "1.000000",
        )

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

        assertThatThrownBy {
            insertOptimizationRun(
                UUID.randomUUID(),
                planId,
                forecastId,
                constraintRevisionId,
                user.id,
                now,
            )
        }.isInstanceOf(DataAccessException::class.java)
    }

    private fun insertBudget(
        userId: UUID,
        currency: Currency,
        now: OffsetDateTime,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budgets (
                id, user_id, budget_month, planned_income, savings_target,
                currency, created_at, updated_at
            )
            VALUES (?, ?, ?, 185000.00, 30000.00, ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            userId,
            BUDGET_MONTH,
            currency.name,
            now,
            now,
        )
    }

    private fun insertDraftPlan(
        id: UUID,
        userId: UUID,
        currency: Currency,
        revision: Int,
        idempotencyKey: String,
        now: OffsetDateTime,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budget_plans (
                id, user_id, budget_month, currency, revision, version,
                status, idempotency_key, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, 0, 'DRAFT', ?, ?, ?)
            """.trimIndent(),
            id,
            userId,
            BUDGET_MONTH,
            currency.name,
            revision,
            idempotencyKey,
            now,
            now,
        )
    }

    private fun insertExpenseCategory(
        id: UUID,
        userId: UUID,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.categories (id, user_id, name, type, icon, color)
            VALUES (?, ?, 'Groceries', 'EXPENSE', 'cart', '#10B981')
            """.trimIndent(),
            id,
            userId,
        )
    }

    private fun insertForecast(
        id: UUID,
        planId: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budget_forecast_revisions (
                id, user_id, plan_id, revision, source_fingerprint,
                forecast_fingerprint, history_months_used, history_method,
                forecast_income, recurring_income, recurring_expenses,
                flexible_estimate, forecast_expenses, forecast_savings,
                source_snapshot, confirmed_at
            )
            VALUES (
                ?, ?, ?, 1, ?, ?, 6, 'MEDIAN_6M',
                185000.00, 185000.00, 82000.00,
                56200.00, 138200.00, 46800.00,
                '{}'::jsonb, ?
            )
            """.trimIndent(),
            id,
            userId,
            planId,
            SHA_256,
            SHA_256,
            now,
        )
    }

    private fun insertConstraints(
        id: UUID,
        forecastId: UUID,
        planId: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budget_constraint_revisions (
                id, user_id, plan_id, forecast_revision_id, revision,
                constraint_fingerprint, target_savings_amount,
                fixed_required_amount, variable_minimum_amount,
                maximum_savings_amount, feasibility_status,
                shortfall_amount, violations, created_at
            )
            VALUES (
                ?, ?, ?, ?, 1, ?, 30500.00,
                82000.00, 15000.00, 88000.00,
                'FEASIBLE', 0, '[]'::jsonb, ?
            )
            """.trimIndent(),
            id,
            userId,
            planId,
            forecastId,
            SHA_256,
            now,
        )
    }

    private fun insertCategoryConstraint(
        id: UUID,
        constraintRevisionId: UUID,
        categoryId: UUID,
        userId: UUID,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budget_category_constraints (
                id, user_id, constraint_revision_id, category_id,
                allocation_type, constraint_role, priority,
                current_limit_amount, minimum_amount, source_keys
            )
            VALUES (
                ?, ?, ?, ?, 'VARIABLE', 'FLEXIBLE', 'HIGH',
                56200.00, 15000.00, '[]'::jsonb
            )
            """.trimIndent(),
            id,
            userId,
            constraintRevisionId,
            categoryId,
        )
    }

    private fun insertFundingLevel(
        id: UUID,
        categoryConstraintId: UUID,
        userId: UUID,
        level: String,
        amount: String,
        coverage: String,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budget_constraint_funding_levels (
                id, user_id, category_constraint_id, level, amount, coverage
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id,
            userId,
            categoryConstraintId,
            level,
            BigDecimal(amount),
            BigDecimal(coverage),
        )
    }

    private fun insertOptimizationRun(
        id: UUID,
        planId: UUID,
        forecastId: UUID,
        constraintRevisionId: UUID,
        userId: UUID,
        now: OffsetDateTime,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budget_optimization_runs (
                id, user_id, plan_id, forecast_revision_id,
                constraint_revision_id, algorithm_version, objective_code,
                status, generation_idempotency_key, input_fingerprint,
                forecast_income_amount, target_savings_amount,
                fixed_required_amount, variable_minimum_amount,
                variable_capacity_amount, maximum_savings_amount,
                baseline_savings_amount, selected_variable_amount,
                total_allocation_amount, actual_savings_amount,
                additional_savings_amount, unused_capacity_amount,
                weighted_coverage_score, objective_value,
                input_snapshot, result_snapshot, violations, created_at
            )
            VALUES (
                ?, ?, ?, ?, ?, 'mckp-v1', 'MAXIMIZE_WEIGHTED_COVERAGE',
                'GENERATED', ?, ?,
                185000.00, 30500.00, 82000.00, 15000.00,
                72500.00, 88000.00, 43000.00, 54700.00,
                136700.00, 48300.00, 5300.00, 17800.00,
                0.85000000, 2.55000000,
                '{}'::jsonb, '{}'::jsonb, '[]'::jsonb, ?
            )
            """.trimIndent(),
            id,
            userId,
            planId,
            forecastId,
            constraintRevisionId,
            "generate-$id",
            SHA_256,
            now,
        )
    }

    private fun insertOptimizationDecision(
        runId: UUID,
        constraintRevisionId: UUID,
        categoryConstraintId: UUID,
        categoryId: UUID,
        fundingLevelId: UUID,
        userId: UUID,
    ) {
        dsl.execute(
            """
            INSERT INTO keeper.budget_optimization_decisions (
                id, user_id, optimization_run_id, constraint_revision_id,
                category_constraint_id, category_id, funding_level_id,
                allocation_type, constraint_role, priority, selected_level,
                current_limit_amount, minimum_amount, recommended_limit_amount,
                change_amount, coverage, option_value, reason_code, reason_parameters
            )
            VALUES (
                ?, ?, ?, ?, ?, ?, ?,
                'VARIABLE', 'FLEXIBLE', 'HIGH', 'BALANCED',
                56200.00, 15000.00, 54700.00,
                -1500.00, 0.850000, 2.55000000,
                'SELECTED_HIGHEST_VALUE_WITHIN_CAPACITY', '{}'::jsonb
            )
            """.trimIndent(),
            UUID.randomUUID(),
            userId,
            runId,
            constraintRevisionId,
            categoryConstraintId,
            categoryId,
            fundingLevelId,
        )
    }
}

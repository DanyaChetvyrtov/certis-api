package ru.digitalhustle.certis.features.budget.command.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import java.util.UUID

@Repository
class BudgetConstraintRevisionRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun nextRevision(planId: UUID): Int =
        requireNotNull(
            dsl.select(DSL.coalesce(DSL.max(Tables.BUDGET_CONSTRAINT_REVISIONS.REVISION), 0).plus(1))
                .from(Tables.BUDGET_CONSTRAINT_REVISIONS)
                .where(Tables.BUDGET_CONSTRAINT_REVISIONS.PLAN_ID.eq(planId))
                .fetchSingle(0, Int::class.java),
        )

    fun insert(constraints: BudgetConstraintSet) {
        val feasibility = constraints.feasibility
        dsl.insertInto(Tables.BUDGET_CONSTRAINT_REVISIONS)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.ID, requireNotNull(constraints.id))
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.USER_ID, constraints.userId)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.PLAN_ID, constraints.planId)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.FORECAST_REVISION_ID, constraints.forecastRevisionId)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.REVISION, requireNotNull(constraints.revision))
            .set(
                Tables.BUDGET_CONSTRAINT_REVISIONS.CONSTRAINT_FINGERPRINT,
                requireNotNull(constraints.constraintFingerprint),
            )
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.SAVINGS_FLOOR_AMOUNT, constraints.savingsFloorAmount)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.REQUIRED_AMOUNT, feasibility.requiredAmount)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.VARIABLE_MINIMUM_AMOUNT, feasibility.variableMinimumAmount)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.MAXIMUM_SAVINGS_AMOUNT, feasibility.maximumSavingsAmount)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.FEASIBILITY_STATUS, feasibility.status.name)
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.SHORTFALL_AMOUNT, feasibility.shortfall)
            .set(
                Tables.BUDGET_CONSTRAINT_REVISIONS.VIOLATIONS,
                JSONB.valueOf(objectMapper.writeValueAsString(feasibility.violations)),
            )
            .set(Tables.BUDGET_CONSTRAINT_REVISIONS.CREATED_AT, requireNotNull(constraints.createdAt))
            .execute()
    }
}

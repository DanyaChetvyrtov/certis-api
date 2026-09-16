package ru.digitalhustle.certis.features.budget.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BudgetOptimizationRunRepository(
    private val dsl: DSLContext,
) {

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
}

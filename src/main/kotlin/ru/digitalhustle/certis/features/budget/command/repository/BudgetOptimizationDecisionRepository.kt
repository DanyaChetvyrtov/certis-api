package ru.digitalhustle.certis.features.budget.command.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun

@Repository
class BudgetOptimizationDecisionRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun insertAll(run: BudgetPlanningOptimizationRun) {
        run.decisions.forEach { decision ->
            dsl.insertInto(Tables.BUDGET_OPTIMIZATION_DECISIONS)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.ID, decision.id)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.USER_ID, run.userId)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.OPTIMIZATION_RUN_ID, run.id)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CONSTRAINT_REVISION_ID, run.constraintRevisionId)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_CONSTRAINT_ID, decision.categoryConstraintId)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CATEGORY_ID, decision.category.id)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.FUNDING_LEVEL_ID, decision.fundingLevelId)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.ALLOCATION_TYPE, decision.allocationType.name)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CONSTRAINT_ROLE, decision.constraintRole.name)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.PRIORITY, decision.priority?.name)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.SELECTED_LEVEL, decision.selectedLevel?.name)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CURRENT_LIMIT_AMOUNT, decision.currentLimit)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.REQUIRED_AMOUNT, decision.requiredAmount)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.RECOMMENDED_LIMIT_AMOUNT, decision.recommendedLimit)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.CHANGE_AMOUNT, decision.change)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.COVERAGE, decision.coverage)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.OPTION_VALUE, decision.optionValue)
                .set(Tables.BUDGET_OPTIMIZATION_DECISIONS.REASON_CODE, decision.reason.code)
                .set(
                    Tables.BUDGET_OPTIMIZATION_DECISIONS.REASON_PARAMETERS,
                    JSONB.valueOf(objectMapper.writeValueAsString(decision.reason.parameters)),
                )
                .execute()
        }
    }
}

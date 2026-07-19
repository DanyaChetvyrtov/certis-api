package ru.digitalhustle.certis.features.budget.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import java.util.UUID

@Repository
class BudgetConstraintFundingLevelRepository(
    private val dsl: DSLContext,
) {

    fun insertAll(userId: UUID, categories: List<BudgetCategoryConstraint>) {
        categories.forEach { category ->
            category.fundingLevels.forEach { level ->
                dsl.insertInto(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS)
                    .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.ID, level.id)
                    .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.USER_ID, userId)
                    .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.CATEGORY_CONSTRAINT_ID, category.id)
                    .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.LEVEL, level.level.name)
                    .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.AMOUNT, level.amount)
                    .set(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.COVERAGE, level.coverage)
                    .execute()
            }
        }
    }
}

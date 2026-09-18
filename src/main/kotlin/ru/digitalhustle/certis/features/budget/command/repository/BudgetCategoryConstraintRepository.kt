package ru.digitalhustle.certis.features.budget.command.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import java.util.UUID

@Repository
class BudgetCategoryConstraintRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun insertAll(
        constraintRevisionId: UUID,
        userId: UUID,
        categories: List<BudgetCategoryConstraint>,
    ) {
        categories.forEach { category ->
            dsl.insertInto(Tables.BUDGET_CATEGORY_CONSTRAINTS)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.ID, category.id)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.USER_ID, userId)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CONSTRAINT_REVISION_ID, constraintRevisionId)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CATEGORY_ID, category.category.id)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CATEGORY_TYPE, category.category.type.name)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.ALLOCATION_TYPE, category.allocationType.name)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CONSTRAINT_ROLE, category.constraintRole.name)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.PRIORITY, category.priority?.name)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.CURRENT_LIMIT_AMOUNT, category.currentLimitAmount)
                .set(Tables.BUDGET_CATEGORY_CONSTRAINTS.REQUIRED_AMOUNT, category.requiredAmount)
                .set(
                    Tables.BUDGET_CATEGORY_CONSTRAINTS.SOURCE_KEYS,
                    JSONB.valueOf(objectMapper.writeValueAsString(category.sourceKeys)),
                )
                .execute()
        }
    }
}

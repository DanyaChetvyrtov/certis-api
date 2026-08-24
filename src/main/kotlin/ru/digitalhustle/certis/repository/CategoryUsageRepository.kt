package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class CategoryUsageRepository(
    private val dsl: DSLContext,
) {

    fun existsInSchedulableRecurringTemplate(
        categoryId: UUID,
        userId: UUID,
    ): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(Tables.RECURRING_TRANSACTION_TEMPLATES)
                .where(
                    Tables.RECURRING_TRANSACTION_TEMPLATES.CATEGORY_ID.eq(categoryId)
                        .and(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(userId))
                        .and(Tables.RECURRING_TRANSACTION_TEMPLATES.STATUS.`in`("ACTIVE", "PAUSED")),
                ),
        )

    fun existsInNonEndedBudget(
        categoryId: UUID,
        userId: UUID,
        currentDate: LocalDate,
    ): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(Tables.BUDGET_CATEGORIES)
                .join(Tables.BUDGETS)
                .on(
                    Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(Tables.BUDGETS.ID)
                        .and(Tables.BUDGET_CATEGORIES.USER_ID.eq(Tables.BUDGETS.USER_ID)),
                )
                .where(
                    Tables.BUDGET_CATEGORIES.CATEGORY_ID.eq(categoryId)
                        .and(Tables.BUDGET_CATEGORIES.USER_ID.eq(userId))
                        .and(Tables.BUDGETS.PERIOD_END.ge(currentDate)),
                ),
        )
}

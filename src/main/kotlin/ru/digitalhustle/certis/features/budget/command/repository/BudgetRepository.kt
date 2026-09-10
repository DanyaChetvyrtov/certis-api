package ru.digitalhustle.certis.features.budget.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.Budget
import java.time.LocalDate
import java.util.UUID

@Repository
class BudgetRepository(
    private val dsl: DSLContext,
) {

    fun findByUserIdAndMonthForUpdate(
        userId: UUID,
        budgetMonth: LocalDate,
    ): Budget? =
        dsl.selectFrom(Tables.BUDGETS)
            .where(
                Tables.BUDGETS.USER_ID.eq(userId)
                    .and(Tables.BUDGETS.BUDGET_MONTH.eq(budgetMonth)),
            )
            .forUpdate()
            .fetchOneInto(Budget::class.java)

    fun insert(budget: Budget): Budget =
        dsl.insertInto(Tables.BUDGETS)
            .set(dsl.newRecord(Tables.BUDGETS, budget))
            .returning()
            .fetchSingleInto(Budget::class.java)

    fun update(budget: Budget): Budget =
        dsl.update(Tables.BUDGETS)
            .set(Tables.BUDGETS.PLANNED_INCOME, budget.plannedIncome)
            .set(Tables.BUDGETS.SAVINGS_TARGET, budget.savingsTarget)
            .set(Tables.BUDGETS.CURRENCY, budget.currency.name)
            .set(Tables.BUDGETS.UPDATED_AT, budget.updatedAt)
            .where(
                Tables.BUDGETS.ID.eq(budget.id)
                    .and(Tables.BUDGETS.USER_ID.eq(budget.userId)),
            )
            .returning()
            .fetchSingleInto(Budget::class.java)
}

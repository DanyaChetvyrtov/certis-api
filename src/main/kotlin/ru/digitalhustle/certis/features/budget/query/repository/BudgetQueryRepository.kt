package ru.digitalhustle.certis.features.budget.query.repository

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record8
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import ru.digitalhustle.certis.features.budget.model.Budget
import ru.digitalhustle.certis.features.budget.model.BudgetAllocationDetails
import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BudgetQueryRepository(
    private val dsl: DSLContext,
) {

    fun findDetailsByUserIdAndMonth(
        userId: UUID,
        budgetMonth: LocalDate,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): BudgetDetails? {
        val budget = findByUserIdAndMonth(userId, budgetMonth) ?: return null

        return BudgetDetails(
            id = budget.id,
            budgetMonth = budget.budgetMonth,
            plannedIncome = budget.plannedIncome,
            savingsTarget = budget.savingsTarget,
            currency = budget.currency,
            allocations = findAllocationDetails(budget, monthStart, nextMonthStart),
            createdAt = budget.createdAt,
            updatedAt = budget.updatedAt,
        )
    }

    fun findByUserIdAndMonth(
        userId: UUID,
        budgetMonth: LocalDate,
    ): Budget? =
        dsl.selectFrom(Tables.BUDGETS)
            .where(
                Tables.BUDGETS.USER_ID.eq(userId)
                    .and(Tables.BUDGETS.BUDGET_MONTH.eq(budgetMonth)),
            )
            .fetchOneInto(Budget::class.java)

    private fun findAllocationDetails(
        budget: Budget,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): List<BudgetAllocationDetails> {
        val spentAmount = createSpentAmountField(budget)

        return dsl.select(
            Tables.BUDGET_CATEGORIES.ID,
            Tables.BUDGET_CATEGORIES.CATEGORY_ID,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
            Tables.BUDGET_CATEGORIES.EXPENSE_TYPE,
            Tables.BUDGET_CATEGORIES.LIMIT_AMOUNT,
            spentAmount,
        )
            .from(Tables.BUDGET_CATEGORIES)
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.BUDGET_CATEGORIES.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.BUDGET_CATEGORIES.USER_ID)),
            )
            .leftJoin(Tables.TRANSACTIONS)
            .on(
                Tables.TRANSACTIONS.USER_ID.eq(Tables.BUDGET_CATEGORIES.USER_ID)
                    .and(Tables.TRANSACTIONS.CATEGORY_ID.eq(Tables.BUDGET_CATEGORIES.CATEGORY_ID))
                    .and(Tables.TRANSACTIONS.TYPE.eq("EXPENSE"))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(monthStart))
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(nextMonthStart)),
            )
            .leftJoin(Tables.ACCOUNTS)
            .on(
                Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
                    .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID)),
            )
            .where(
                Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(budget.id)
                    .and(Tables.BUDGET_CATEGORIES.USER_ID.eq(budget.userId)),
            )
            .groupBy(
                Tables.BUDGET_CATEGORIES.ID,
                Tables.BUDGET_CATEGORIES.CATEGORY_ID,
                Tables.CATEGORIES.NAME,
                Tables.CATEGORIES.ICON,
                Tables.CATEGORIES.COLOR,
                Tables.BUDGET_CATEGORIES.EXPENSE_TYPE,
                Tables.BUDGET_CATEGORIES.LIMIT_AMOUNT,
            )
            .orderBy(
                Tables.BUDGET_CATEGORIES.EXPENSE_TYPE.asc(),
                Tables.CATEGORIES.NAME.asc(),
            )
            .fetch { record -> record.toAllocationDetails(spentAmount) }
    }

    private fun createSpentAmountField(budget: Budget): Field<BigDecimal> {
        val amountInBudgetCurrency = DSL
            .`when`(
                Tables.ACCOUNTS.CURRENCY.eq(budget.currency.name),
                Tables.TRANSACTIONS.AMOUNT,
            )
            .otherwise(BigDecimal.ZERO)

        return DSL.coalesce(DSL.sum(amountInBudgetCurrency), BigDecimal.ZERO)
    }

    private fun Record8<UUID, UUID, String, String, String, String, BigDecimal, BigDecimal>.toAllocationDetails(
        spentAmount: Field<BigDecimal>,
    ): BudgetAllocationDetails =
        BudgetAllocationDetails.create(
            id = this[Tables.BUDGET_CATEGORIES.ID],
            categoryId = this[Tables.BUDGET_CATEGORIES.CATEGORY_ID],
            categoryName = this[Tables.CATEGORIES.NAME],
            categoryIcon = this[Tables.CATEGORIES.ICON],
            categoryColor = this[Tables.CATEGORIES.COLOR],
            expenseType = BudgetExpenseType.valueOf(this[Tables.BUDGET_CATEGORIES.EXPENSE_TYPE]),
            limitAmount = this[Tables.BUDGET_CATEGORIES.LIMIT_AMOUNT],
            spentAmount = this[spentAmount] ?: BigDecimal.ZERO,
        )
}

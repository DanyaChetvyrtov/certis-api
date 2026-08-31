package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record8
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.budget.BudgetAllocationDetails
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.entity.Budget
import ru.digitalhustle.certis.model.entity.BudgetCategory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BudgetRepository(
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

    fun countActiveExpenseCategories(
        userId: UUID,
        categoryIds: Collection<UUID>,
    ): Int {
        if (categoryIds.isEmpty()) {
            return 0
        }

        return dsl.fetchCount(
            Tables.CATEGORIES,
            Tables.CATEGORIES.USER_ID.eq(userId)
                .and(Tables.CATEGORIES.ID.`in`(categoryIds))
                .and(Tables.CATEGORIES.TYPE.eq(CategoryType.EXPENSE.name))
                .and(Tables.CATEGORIES.ARCHIVED_AT.isNull()),
        )
    }

    fun findPreferredCurrencyByUserIdForUpdate(userId: UUID): Currency? =
        dsl.select(Tables.USERS.PREFERRED_CURRENCY)
            .from(Tables.USERS)
            .where(Tables.USERS.ID.eq(userId))
            .forUpdate()
            .fetchOne(Tables.USERS.PREFERRED_CURRENCY)
            ?.let(Currency::valueOf)

    fun insert(budget: Budget): Budget =
        dsl.insertInto(Tables.BUDGETS)
            .set(dsl.newRecord(Tables.BUDGETS, budget))
            .returning()
            .fetchSingleInto(Budget::class.java)

    fun insertAllocations(allocations: Collection<BudgetCategory>) {
        if (allocations.isEmpty()) {
            return
        }

        dsl.batchInsert(
            allocations.map { allocation ->
                dsl.newRecord(Tables.BUDGET_CATEGORIES, allocation)
            },
        ).execute()
    }

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

    fun deleteAllocations(budgetId: UUID) {
        dsl.deleteFrom(Tables.BUDGET_CATEGORIES)
            .where(Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(budgetId))
            .execute()
    }

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
                    .and(Tables.TRANSACTIONS.TYPE.eq(TransactionType.EXPENSE.name))
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

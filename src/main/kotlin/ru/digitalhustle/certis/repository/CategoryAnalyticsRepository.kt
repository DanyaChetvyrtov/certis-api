package ru.digitalhustle.certis.repository

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Records
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.constants.MoneyConstants
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.model.category.TopCategoryAnalytics
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class CategoryAnalyticsRepository(
    private val dsl: DSLContext,
) {

    fun findByUserId(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): CategoryAnalytics {
        val summary = fetchSummary(userId, filter, monthStart, nextMonthStart)
        val topCategories = fetchTopCategories(userId, filter, monthStart, nextMonthStart)

        return CategoryAnalytics(
            month = filter.month,
            currency = filter.currency,
            type = filter.type,
            totalTransactionCount = summary.totalTransactionCount,
            categorizedTransactionCount = summary.categorizedTransactionCount,
            uncategorizedTransactionCount = summary.uncategorizedTransactionCount,
            totalSum = summary.totalSum,
            categorizedSum = summary.categorizedSum,
            uncategorizedSum = summary.uncategorizedSum,
            coveragePercentage = summary.coveragePercentage,
            topExpenseCategories = topCategories,
        )
    }

    private fun fetchSummary(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): CategoryAnalyticsSummary {
        val totalTransactionCount = DSL.count(Tables.TRANSACTIONS.ID)
        val categorizedTransactionCount = DSL.count(Tables.TRANSACTIONS.ID)
            .filterWhere(Tables.TRANSACTIONS.CATEGORY_ID.isNotNull)
        val uncategorizedTransactionCount = DSL.count(Tables.TRANSACTIONS.ID)
            .filterWhere(Tables.TRANSACTIONS.CATEGORY_ID.isNull)
        val totalSum = DSL.coalesce(DSL.sum(Tables.TRANSACTIONS.AMOUNT), BigDecimal.ZERO)
        val categorizedSum = DSL.coalesce(
            DSL.sum(Tables.TRANSACTIONS.AMOUNT)
                .filterWhere(Tables.TRANSACTIONS.CATEGORY_ID.isNotNull),
            BigDecimal.ZERO,
        )
        val uncategorizedSum = DSL.coalesce(
            DSL.sum(Tables.TRANSACTIONS.AMOUNT)
                .filterWhere(Tables.TRANSACTIONS.CATEGORY_ID.isNull),
            BigDecimal.ZERO,
        )
        val coveragePercentage = categorizedSum
            .multiply(MoneyConstants.PERCENTAGE_MULTIPLIER)
            .divide(DSL.nullif(totalSum, BigDecimal.ZERO))

        return dsl
            .select(
                totalTransactionCount,
                categorizedTransactionCount,
                uncategorizedTransactionCount,
                scaledMoney(totalSum),
                scaledMoney(categorizedSum),
                scaledMoney(uncategorizedSum),
                scaledPercentage(coveragePercentage),
            )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .where(transactionCondition(userId, filter, monthStart, nextMonthStart))
            .fetchSingle(Records.mapping(::CategoryAnalyticsSummary))
    }

    private fun fetchTopCategories(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): List<TopCategoryAnalytics> {
        val totalSum = totalSumField(userId, filter, monthStart, nextMonthStart)
        val categoryAmount = DSL.sum(Tables.TRANSACTIONS.AMOUNT)
        val sharePercentage = categoryAmount
            .multiply(MoneyConstants.PERCENTAGE_MULTIPLIER)
            .divide(totalSum)

        return dsl
            .select(
                Tables.CATEGORIES.ID,
                Tables.CATEGORIES.NAME,
                Tables.CATEGORIES.COLOR,
                scaledMoney(categoryAmount),
                scaledPercentage(sharePercentage),
            )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.TRANSACTIONS.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.TRANSACTIONS.USER_ID)),
            )
            .where(transactionCondition(userId, filter, monthStart, nextMonthStart))
            .groupBy(
                Tables.CATEGORIES.ID,
                Tables.CATEGORIES.NAME,
                Tables.CATEGORIES.COLOR,
            )
            .orderBy(
                categoryAmount.desc(),
                DSL.lower(Tables.CATEGORIES.NAME).asc(),
                Tables.CATEGORIES.ID.asc(),
            )
            .limit(filter.topLimit)
            .fetch(Records.mapping(::TopCategoryAnalytics))
    }

    private fun totalSumField(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): Field<BigDecimal> =
        dsl.select(DSL.coalesce(DSL.sum(Tables.TRANSACTIONS.AMOUNT), BigDecimal.ZERO))
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .where(transactionCondition(userId, filter, monthStart, nextMonthStart))
            .asField()

    private fun accountJoinCondition(): Condition =
        Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
            .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID))

    private fun transactionCondition(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): Condition =
        Tables.TRANSACTIONS.USER_ID.eq(userId)
            .and(Tables.TRANSACTIONS.TYPE.eq(filter.type.name))
            .and(Tables.TRANSACTIONS.TRANSFER_ID.isNull())
            .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
            .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(monthStart))
            .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(nextMonthStart))
            .and(Tables.ACCOUNTS.CURRENCY.eq(filter.currency.name))

    private fun scaledMoney(field: Field<BigDecimal>): Field<BigDecimal> =
        DSL.round(field, MoneyConstants.MONEY_SCALE)
            .cast(SQLDataType.NUMERIC(MoneyConstants.MONEY_PRECISION, MoneyConstants.MONEY_SCALE))

    private fun scaledPercentage(field: Field<BigDecimal>): Field<BigDecimal> =
        DSL.round(field, MoneyConstants.PERCENTAGE_SCALE)
            .cast(SQLDataType.NUMERIC(MoneyConstants.PERCENTAGE_PRECISION, MoneyConstants.PERCENTAGE_SCALE))

    private data class CategoryAnalyticsSummary(
        val totalTransactionCount: Int,
        val categorizedTransactionCount: Int,
        val uncategorizedTransactionCount: Int,
        val totalSum: BigDecimal,
        val categorizedSum: BigDecimal,
        val uncategorizedSum: BigDecimal,
        val coveragePercentage: BigDecimal?,
    )
}

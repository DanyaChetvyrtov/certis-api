package ru.digitalhustle.certis.features.category.query.repository

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Records
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingBucket
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingCategory
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTimeFilter
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingQueryResult
import ru.digitalhustle.certis.shared.constants.MoneyConstants
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@Repository
class CategorySpendingOverTimeQueryRepository(
    private val dsl: DSLContext,
) {

    fun findByUserId(
        userId: UUID,
        filter: CategorySpendingOverTimeFilter,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
        timeZone: ZoneId,
    ): CategorySpendingQueryResult {
        val topCategories = fetchTopCategories(userId, filter, from, toExclusive)
        val buckets = fetchBuckets(
            userId = userId,
            filter = filter,
            from = from,
            toExclusive = toExclusive,
            timeZone = timeZone,
            topCategoryIds = topCategories.map(CategorySpendingCategory::categoryId),
        )

        return CategorySpendingQueryResult(topCategories, buckets)
    }

    private fun fetchTopCategories(
        userId: UUID,
        filter: CategorySpendingOverTimeFilter,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
    ): List<CategorySpendingCategory> {
        val categoryAmount = DSL.sum(Tables.TRANSACTIONS.AMOUNT)

        return dsl
            .select(
                Tables.CATEGORIES.ID,
                Tables.CATEGORIES.NAME,
                Tables.CATEGORIES.COLOR,
            )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.TRANSACTIONS.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.TRANSACTIONS.USER_ID)),
            )
            .where(transactionCondition(userId, filter, from, toExclusive))
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
            .fetch(Records.mapping(::CategorySpendingCategory))
    }

    private fun fetchBuckets(
        userId: UUID,
        filter: CategorySpendingOverTimeFilter,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
        timeZone: ZoneId,
        topCategoryIds: List<UUID>,
    ): List<CategorySpendingBucket> {
        val bucketMonth = bucketMonth(timeZone)
        val categoryId = groupedCategoryId(topCategoryIds)
        val amount = DSL.sum(Tables.TRANSACTIONS.AMOUNT)

        return dsl
            .select(categoryId, bucketMonth, scaledMoney(amount))
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .where(transactionCondition(userId, filter, from, toExclusive))
            .groupBy(categoryId, bucketMonth)
            .orderBy(bucketMonth.asc(), categoryId.asc().nullsLast())
            .fetch(Records.mapping(::CategorySpendingBucketSummary))
            .map { summary ->
                CategorySpendingBucket(
                    categoryId = summary.categoryId,
                    bucketMonth = YearMonth.from(summary.bucketMonth),
                    amount = summary.amount,
                )
            }
    }

    private fun groupedCategoryId(topCategoryIds: List<UUID>): Field<UUID> =
        if (topCategoryIds.isEmpty()) {
            DSL.castNull(SQLDataType.UUID)
        } else {
            val topCategoryCondition = topCategoryIds
                .map { categoryId -> Tables.TRANSACTIONS.CATEGORY_ID.eq(DSL.inline(categoryId)) }
                .reduce { condition, next -> condition.or(next) }

            DSL.`when`(
                topCategoryCondition,
                Tables.TRANSACTIONS.CATEGORY_ID,
            ).otherwise(DSL.castNull(SQLDataType.UUID))
        }

    private fun bucketMonth(timeZone: ZoneId): Field<LocalDate> =
        DSL.field(
            "date_trunc('month', {0} at time zone {1})::date",
            SQLDataType.LOCALDATE,
            Tables.TRANSACTIONS.OCCURRED_AT,
            DSL.inline(timeZone.id),
        )

    private fun accountJoinCondition(): Condition =
        Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
            .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID))

    private fun transactionCondition(
        userId: UUID,
        filter: CategorySpendingOverTimeFilter,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
    ): Condition =
        Tables.TRANSACTIONS.USER_ID.eq(userId)
            .and(Tables.TRANSACTIONS.TYPE.eq(filter.type.name))
            .and(Tables.TRANSACTIONS.TRANSFER_ID.isNull())
            .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
            .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(from))
            .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(toExclusive))
            .and(Tables.ACCOUNTS.CURRENCY.eq(filter.currency.name))

    private fun scaledMoney(field: Field<BigDecimal>): Field<BigDecimal> =
        DSL.round(field, MoneyConstants.MONEY_SCALE)
            .cast(SQLDataType.NUMERIC(MoneyConstants.MONEY_PRECISION, MoneyConstants.MONEY_SCALE))

    private data class CategorySpendingBucketSummary(
        val categoryId: UUID?,
        val bucketMonth: LocalDate,
        val amount: BigDecimal,
    )
}

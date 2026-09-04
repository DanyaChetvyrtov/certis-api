package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Records
import org.jooq.SortField
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.constants.MoneyConstants
import ru.digitalhustle.certis.enums.CategoryCardSort
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.category.CategoryCard
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class CategoryCardRepository(
    private val dsl: DSLContext,
) {

    fun findByUserId(
        userId: UUID,
        filter: CategoryCardFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): CategoryCards {
        val categoryCards = fetchCategoryCards(
            userId = userId,
            filter = filter,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        )

        val totalElements = dsl.fetchCount(
            Tables.CATEGORIES,
            Tables.CATEGORIES.USER_ID.eq(userId),
        ).toLong()

        return CategoryCards(
            month = filter.month,
            currency = filter.currency,
            categories = categoryCards,
            page = filter.page,
            size = filter.size,
            totalElements = totalElements,
        )
    }

    private fun fetchCategoryCards(
        userId: UUID,
        filter: CategoryCardFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): List<CategoryCard> {
        val statisticFields = categoryStatisticFields()

        return dsl
            .select(
                Tables.CATEGORIES.ID,
                Tables.CATEGORIES.NAME,
                Tables.CATEGORIES.TYPE.convertFrom(CategoryType::valueOf),
                Tables.CATEGORIES.ICON,
                Tables.CATEGORIES.COLOR,
                Tables.CATEGORIES.ARCHIVED_AT,
                statisticFields.transactionCount,
                statisticFields.scaledAmount,
                statisticFields.scaledSharePercentage,
            )
            .from(Tables.CATEGORIES)
            .leftJoin(Tables.TRANSACTIONS)
            .on(
                Tables.TRANSACTIONS.CATEGORY_ID.eq(Tables.CATEGORIES.ID)
                    .and(Tables.TRANSACTIONS.USER_ID.eq(Tables.CATEGORIES.USER_ID))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(monthStart))
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(nextMonthStart)),
            )
            .leftJoin(Tables.ACCOUNTS)
            .on(
                Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
                    .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID))
                    .and(Tables.ACCOUNTS.CURRENCY.eq(filter.currency.name)),
            )
            .where(Tables.CATEGORIES.USER_ID.eq(userId))
            .groupBy(
                Tables.CATEGORIES.ID,
                Tables.CATEGORIES.NAME,
                Tables.CATEGORIES.TYPE,
                Tables.CATEGORIES.ICON,
                Tables.CATEGORIES.COLOR,
                Tables.CATEGORIES.ARCHIVED_AT,
            )
            .orderBy(
                categoryOrder(
                    sort = filter.sort,
                    categoryId = Tables.CATEGORIES.ID,
                    categoryName = Tables.CATEGORIES.NAME,
                    monthlyAmount = statisticFields.amount,
                ),
            )
            .limit(filter.size)
            .offset(filter.page * filter.size)
            .fetch(Records.mapping(::CategoryCard))
    }

    private fun categoryStatisticFields(): CategoryStatisticFields {
        val monthlyTransactionCount = DSL
            .count(Tables.TRANSACTIONS.ID)
            .filterWhere(Tables.ACCOUNTS.ID.isNotNull)
        val amountInRequestedCurrency = DSL
            .`when`(Tables.ACCOUNTS.ID.isNotNull, Tables.TRANSACTIONS.AMOUNT)
            .otherwise(BigDecimal.ZERO)

        val monthlyAmount = DSL.sum(amountInRequestedCurrency)
        val totalAmountByType = DSL
            .sum(monthlyAmount)
            .over(DSL.partitionBy(Tables.CATEGORIES.TYPE))

        val monthlySharePercentage = DSL
            .`when`(totalAmountByType.eq(BigDecimal.ZERO), BigDecimal.ZERO)
            .otherwise(
                monthlyAmount
                    .multiply(MoneyConstants.PERCENTAGE_MULTIPLIER)
                    .divide(totalAmountByType),
            )

        val scaledMonthlyAmount = DSL
            .round(monthlyAmount, MoneyConstants.MONEY_SCALE)
            .cast(SQLDataType.NUMERIC(MoneyConstants.MONEY_PRECISION, MoneyConstants.MONEY_SCALE))

        val scaledMonthlySharePercentage = DSL
            .round(monthlySharePercentage, MoneyConstants.PERCENTAGE_SCALE)
            .cast(SQLDataType.NUMERIC(MoneyConstants.PERCENTAGE_PRECISION, MoneyConstants.PERCENTAGE_SCALE))

        return CategoryStatisticFields(
            transactionCount = monthlyTransactionCount,
            amount = monthlyAmount,
            scaledAmount = scaledMonthlyAmount,
            scaledSharePercentage = scaledMonthlySharePercentage,
        )
    }

    private fun categoryOrder(
        sort: CategoryCardSort,
        categoryId: Field<UUID>,
        categoryName: Field<String>,
        monthlyAmount: Field<BigDecimal>,
    ): List<SortField<*>> {
        val normalizedName = DSL.lower(categoryName)

        return when (sort) {
            CategoryCardSort.NAME -> listOf(normalizedName.asc(), categoryId.asc())
            CategoryCardSort.AMOUNT_DESC -> listOf(monthlyAmount.desc(), normalizedName.asc(), categoryId.asc())
            CategoryCardSort.AMOUNT_ASC -> listOf(monthlyAmount.asc(), normalizedName.asc(), categoryId.asc())
        }
    }

    private data class CategoryStatisticFields(
        val transactionCount: Field<Int>,
        val amount: Field<BigDecimal>,
        val scaledAmount: Field<BigDecimal>,
        val scaledSharePercentage: Field<BigDecimal>,
    )
}

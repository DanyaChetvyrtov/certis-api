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
import ru.digitalhustle.certis.enums.CashFlowGranularity
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.transaction.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.CashFlowPoint
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionTotal
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class TransactionAnalyticsRepository(
    private val dsl: DSLContext,
) {

    fun findMonthlyByUserId(
        userId: UUID,
        filter: MonthlyTransactionAnalyticsFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): MonthlyTransactionAnalytics {
        val incomeCondition = Tables.TRANSACTIONS.TYPE.eq(TransactionType.INCOME.name)
        val expenseCondition = Tables.TRANSACTIONS.TYPE.eq(TransactionType.EXPENSE.name)
        val incomeTransactionCount = DSL.count(Tables.TRANSACTIONS.ID).filterWhere(incomeCondition)
        val expenseTransactionCount = DSL.count(Tables.TRANSACTIONS.ID).filterWhere(expenseCondition)
        val incomeAmount = sumAmount(incomeCondition)
        val expenseAmount = sumAmount(expenseCondition)
        val netCashFlow = incomeAmount.minus(expenseAmount)

        val summary = dsl
            .select(
                incomeTransactionCount,
                scaledMoney(incomeAmount),
                expenseTransactionCount,
                scaledMoney(expenseAmount),
                scaledMoney(netCashFlow),
            )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .where(transactionCondition(userId, filter, monthStart, nextMonthStart))
            .fetchSingle(Records.mapping(::MonthlyTransactionAnalyticsSummary))

        return MonthlyTransactionAnalytics(
            month = filter.month,
            currency = filter.currency,
            income = MonthlyTransactionTotal(
                transactionCount = summary.incomeTransactionCount,
                amount = summary.incomeAmount,
            ),
            expenses = MonthlyTransactionTotal(
                transactionCount = summary.expenseTransactionCount,
                amount = summary.expenseAmount,
            ),
            netCashFlow = summary.netCashFlow,
        )
    }

    fun findCashFlowPointsByUserId(
        userId: UUID,
        filter: CashFlowAnalyticsFilter,
        granularity: CashFlowGranularity,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
    ): List<CashFlowPoint> {
        val bucketStart = bucketStart(granularity, filter)
        val incomeCondition = Tables.TRANSACTIONS.TYPE.eq(TransactionType.INCOME.name)
        val expenseCondition = Tables.TRANSACTIONS.TYPE.eq(TransactionType.EXPENSE.name)
        val incomeAmount = sumAmount(incomeCondition)
        val expenseAmount = sumAmount(expenseCondition)

        return dsl
            .select(
                bucketStart,
                scaledMoney(incomeAmount),
                scaledMoney(expenseAmount),
                scaledMoney(incomeAmount.minus(expenseAmount)),
            )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .where(transactionCondition(userId, filter.currency, from, toExclusive))
            .groupBy(bucketStart)
            .orderBy(bucketStart.asc())
            .fetch(Records.mapping(::CashFlowPointSummary))
            .map { summary ->
                CashFlowPoint(
                    bucketStart = summary.bucketStart.atZone(filter.timeZone).toOffsetDateTime(),
                    income = summary.income,
                    expenses = summary.expenses,
                    netCashFlow = summary.netCashFlow,
                )
            }
    }

    private fun sumAmount(typeCondition: Condition): Field<BigDecimal> =
        DSL.coalesce(
            DSL.sum(Tables.TRANSACTIONS.AMOUNT).filterWhere(typeCondition),
            BigDecimal.ZERO,
        )

    private fun accountJoinCondition(): Condition =
        Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
            .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID))

    private fun transactionCondition(
        userId: UUID,
        filter: MonthlyTransactionAnalyticsFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): Condition =
        transactionCondition(userId, filter.currency, monthStart, nextMonthStart)

    private fun transactionCondition(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
    ): Condition =
        Tables.TRANSACTIONS.USER_ID.eq(userId)
            .and(Tables.TRANSACTIONS.TRANSFER_ID.isNull())
            .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
            .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(from))
            .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(toExclusive))
            .and(Tables.ACCOUNTS.CURRENCY.eq(currency.name))

    private fun bucketStart(
        granularity: CashFlowGranularity,
        filter: CashFlowAnalyticsFilter,
    ): Field<LocalDateTime> =
        DSL.field(
            "date_trunc({0}, {1} at time zone {2})",
            SQLDataType.LOCALDATETIME,
            DSL.inline(granularity.databaseValue),
            Tables.TRANSACTIONS.OCCURRED_AT,
            DSL.inline(filter.timeZone.id),
        )

    private fun scaledMoney(field: Field<BigDecimal>): Field<BigDecimal> =
        DSL.round(field, MoneyConstants.MONEY_SCALE)
            .cast(SQLDataType.NUMERIC(MoneyConstants.MONEY_PRECISION, MoneyConstants.MONEY_SCALE))

    private data class MonthlyTransactionAnalyticsSummary(
        val incomeTransactionCount: Int,
        val incomeAmount: BigDecimal,
        val expenseTransactionCount: Int,
        val expenseAmount: BigDecimal,
        val netCashFlow: BigDecimal,
    )

    private data class CashFlowPointSummary(
        val bucketStart: LocalDateTime,
        val income: BigDecimal,
        val expenses: BigDecimal,
        val netCashFlow: BigDecimal,
    )
}

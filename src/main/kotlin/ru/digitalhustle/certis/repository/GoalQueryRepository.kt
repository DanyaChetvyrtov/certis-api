package ru.digitalhustle.certis.repository

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SortField
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalContributionPlanType
import ru.digitalhustle.certis.enums.GoalSort
import ru.digitalhustle.certis.enums.GoalStatus
import ru.digitalhustle.certis.enums.GoalTransactionType
import ru.digitalhustle.certis.model.entity.Goal
import ru.digitalhustle.certis.model.goal.GoalFilter
import ru.digitalhustle.certis.model.goal.GoalStatusCounts
import ru.digitalhustle.certis.model.goal.GoalWithBalance
import java.math.BigDecimal
import java.util.UUID

@Repository
class GoalQueryRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): GoalWithBalance? =
        fetchGoals(
            condition = Tables.GOALS.ID.eq(id).and(Tables.GOALS.USER_ID.eq(userId)),
            orderBy = emptyList(),
        ).singleOrNull()

    fun findPageByUserId(
        userId: UUID,
        currency: Currency,
        filter: GoalFilter,
    ): List<GoalWithBalance> {
        val condition = Tables.GOALS.USER_ID.eq(userId)
            .and(Tables.GOALS.CURRENCY.eq(currency.name))
            .and(Tables.GOALS.STATUS.eq(filter.status.name))
        val savedAmount = savedAmountField()

        return fetchGoals(
            condition = condition,
            orderBy = orderBy(filter.sort, savedAmount),
            limit = filter.size,
            offset = filter.page * filter.size,
            savedAmount = savedAmount,
        )
    }

    fun findAllActiveByUserIdAndCurrency(
        userId: UUID,
        currency: Currency,
    ): List<GoalWithBalance> =
        fetchGoals(
            condition = Tables.GOALS.USER_ID.eq(userId)
                .and(Tables.GOALS.CURRENCY.eq(currency.name))
                .and(Tables.GOALS.STATUS.eq(GoalStatus.ACTIVE.name)),
            orderBy = listOf(Tables.GOALS.DEADLINE.asc().nullsLast(), Tables.GOALS.ID.asc()),
        )

    fun findAllByUserIdAndCurrency(
        userId: UUID,
        currency: Currency,
    ): List<GoalWithBalance> =
        fetchGoals(
            condition = Tables.GOALS.USER_ID.eq(userId)
                .and(Tables.GOALS.CURRENCY.eq(currency.name)),
            orderBy = listOf(Tables.GOALS.CREATED_AT.asc(), Tables.GOALS.ID.asc()),
        )

    fun countByUserIdAndCurrencyAndStatus(
        userId: UUID,
        currency: Currency,
        status: GoalStatus,
    ): Long =
        dsl.selectCount()
            .from(Tables.GOALS)
            .where(
                Tables.GOALS.USER_ID.eq(userId)
                    .and(Tables.GOALS.CURRENCY.eq(currency.name))
                    .and(Tables.GOALS.STATUS.eq(status.name)),
            )
            .fetchOne(0, Long::class.java) ?: 0L

    fun statusCounts(
        userId: UUID,
        currency: Currency,
    ): GoalStatusCounts =
        GoalStatusCounts(
            active = countByUserIdAndCurrencyAndStatus(userId, currency, GoalStatus.ACTIVE),
            completed = countByUserIdAndCurrencyAndStatus(userId, currency, GoalStatus.ACHIEVED),
        )

    private fun fetchGoals(
        condition: Condition,
        orderBy: List<SortField<*>>,
        limit: Int? = null,
        offset: Int? = null,
        savedAmount: Field<BigDecimal> = savedAmountField(),
    ): List<GoalWithBalance> =
        dsl
            .select(*Tables.GOALS.fields(), savedAmount)
            .from(Tables.GOALS)
            .leftJoin(Tables.GOAL_TRANSACTIONS)
            .on(
                Tables.GOAL_TRANSACTIONS.GOAL_ID.eq(Tables.GOALS.ID)
                    .and(Tables.GOAL_TRANSACTIONS.USER_ID.eq(Tables.GOALS.USER_ID)),
            )
            .where(condition)
            .groupBy(*Tables.GOALS.fields())
            .orderBy(orderBy)
            .limit(limit ?: Int.MAX_VALUE)
            .offset(offset ?: 0)
            .fetch { record ->
                GoalWithBalance(
                    goal = mapGoal(record),
                    savedAmount = record[savedAmount] ?: BigDecimal.ZERO,
                )
            }

    private fun savedAmountField(): Field<BigDecimal> {
        val signedAmount = DSL
            .`when`(
                Tables.GOAL_TRANSACTIONS.TYPE.eq(GoalTransactionType.CONTRIBUTION.name),
                Tables.GOAL_TRANSACTIONS.AMOUNT,
            )
            .`when`(
                Tables.GOAL_TRANSACTIONS.TYPE.eq(GoalTransactionType.REFUND.name),
                Tables.GOAL_TRANSACTIONS.AMOUNT.neg(),
            )
            .otherwise(BigDecimal.ZERO)

        return DSL.coalesce(DSL.sum(signedAmount), BigDecimal.ZERO).`as`(SAVED_AMOUNT_ALIAS)
    }

    private fun orderBy(
        sort: GoalSort,
        savedAmount: Field<BigDecimal>,
    ): List<SortField<*>> {
        val progress = savedAmount.div(Tables.GOALS.TARGET_AMOUNT)

        return when (sort) {
            GoalSort.TARGET_MONTH_ASC -> listOf(Tables.GOALS.DEADLINE.asc().nullsLast(), Tables.GOALS.ID.asc())
            GoalSort.TARGET_MONTH_DESC -> listOf(Tables.GOALS.DEADLINE.desc().nullsLast(), Tables.GOALS.ID.asc())
            GoalSort.PROGRESS_ASC -> listOf(progress.asc(), Tables.GOALS.ID.asc())
            GoalSort.PROGRESS_DESC -> listOf(progress.desc(), Tables.GOALS.ID.asc())
            GoalSort.CREATED_AT_DESC -> listOf(Tables.GOALS.CREATED_AT.desc(), Tables.GOALS.ID.desc())
        }
    }

    private fun mapGoal(record: Record): Goal =
        Goal(
            id = record[Tables.GOALS.ID]!!,
            userId = record[Tables.GOALS.USER_ID]!!,
            name = record[Tables.GOALS.NAME]!!,
            targetAmount = record[Tables.GOALS.TARGET_AMOUNT]!!,
            currency = Currency.valueOf(record[Tables.GOALS.CURRENCY]!!),
            deadline = record[Tables.GOALS.DEADLINE],
            contributionPlanType = GoalContributionPlanType.valueOf(record[Tables.GOALS.CONTRIBUTION_PLAN_TYPE]!!),
            monthlyContributionAmount = record[Tables.GOALS.MONTHLY_CONTRIBUTION_AMOUNT]!!,
            icon = record[Tables.GOALS.ICON]!!,
            color = record[Tables.GOALS.COLOR]!!,
            status = GoalStatus.valueOf(record[Tables.GOALS.STATUS]!!),
            createdAt = record[Tables.GOALS.CREATED_AT]!!,
            updatedAt = record[Tables.GOALS.UPDATED_AT]!!,
            achievedAt = record[Tables.GOALS.ACHIEVED_AT],
            archivedAt = record[Tables.GOALS.ARCHIVED_AT],
        )

    private companion object {
        const val SAVED_AMOUNT_ALIAS = "saved_amount"
    }
}

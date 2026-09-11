package ru.digitalhustle.certis.features.goal.query.repository

import org.jooq.DSLContext
import org.jooq.SortField
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.goal.enums.GoalContributionSort
import ru.digitalhustle.certis.features.goal.enums.GoalTransactionType
import ru.digitalhustle.certis.features.goal.model.GoalContributionFilter
import ru.digitalhustle.certis.features.goal.model.GoalMonthlyAmount
import ru.digitalhustle.certis.features.goal.model.GoalTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class GoalTransactionQueryRepository(
    private val dsl: DSLContext,
) {

    fun findPageByGoalIdAndUserId(
        goalId: UUID,
        userId: UUID,
        filter: GoalContributionFilter,
    ): List<GoalTransaction> =
        dsl.selectFrom(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.GOAL_ID.eq(goalId)
                    .and(Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)),
            )
            .orderBy(orderBy(filter.sort))
            .limit(filter.size)
            .offset(filter.page * filter.size)
            .fetchInto(GoalTransaction::class.java)

    fun countByGoalIdAndUserId(
        goalId: UUID,
        userId: UUID,
    ): Long =
        dsl.selectCount()
            .from(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.GOAL_ID.eq(goalId)
                    .and(Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)),
            )
            .fetchOne(0, Long::class.java) ?: 0L

    fun findNetAmountsByGoalId(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<GoalMonthlyAmount> {
        val signedAmount = DSL
            .`when`(
                Tables.GOAL_TRANSACTIONS.TYPE.eq(GoalTransactionType.CONTRIBUTION.name),
                Tables.GOAL_TRANSACTIONS.AMOUNT,
            )
            .otherwise(Tables.GOAL_TRANSACTIONS.AMOUNT.neg())
        val amount = DSL.coalesce(DSL.sum(signedAmount), BigDecimal.ZERO)

        return dsl.select(Tables.GOAL_TRANSACTIONS.GOAL_ID, amount)
            .from(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)
                    .and(Tables.GOAL_TRANSACTIONS.CURRENCY.eq(currency.name))
                    .and(Tables.GOAL_TRANSACTIONS.DATE.ge(from))
                    .and(Tables.GOAL_TRANSACTIONS.DATE.lt(to)),
            )
            .groupBy(Tables.GOAL_TRANSACTIONS.GOAL_ID)
            .fetch { record ->
                GoalMonthlyAmount(
                    goalId = checkNotNull(record[Tables.GOAL_TRANSACTIONS.GOAL_ID]),
                    amount = record[amount] ?: BigDecimal.ZERO,
                )
            }
    }

    private fun orderBy(sort: GoalContributionSort): List<SortField<*>> =
        when (sort) {
            GoalContributionSort.CONTRIBUTED_AT_ASC -> listOf(
                Tables.GOAL_TRANSACTIONS.DATE.asc(),
                Tables.GOAL_TRANSACTIONS.CREATED_AT.asc(),
                Tables.GOAL_TRANSACTIONS.ID.asc(),
            )
            GoalContributionSort.CONTRIBUTED_AT_DESC -> listOf(
                Tables.GOAL_TRANSACTIONS.DATE.desc(),
                Tables.GOAL_TRANSACTIONS.CREATED_AT.desc(),
                Tables.GOAL_TRANSACTIONS.ID.desc(),
            )
        }
}

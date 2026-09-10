package ru.digitalhustle.certis.features.goal.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.goal.enums.GoalTransactionType
import ru.digitalhustle.certis.features.goal.model.GoalTransaction
import java.math.BigDecimal
import java.util.UUID

@Repository
class GoalTransactionRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserIdAndGoalIdForUpdate(
        id: UUID,
        userId: UUID,
        goalId: UUID,
    ): GoalTransaction? =
        dsl.selectFrom(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.ID.eq(id)
                    .and(Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId))
                    .and(Tables.GOAL_TRANSACTIONS.GOAL_ID.eq(goalId)),
            )
            .forUpdate()
            .fetchOneInto(GoalTransaction::class.java)

    fun findByIdempotencyKeyAndUserId(
        idempotencyKey: String,
        userId: UUID,
    ): GoalTransaction? =
        dsl.selectFrom(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.IDEMPOTENCY_KEY.eq(idempotencyKey)
                    .and(Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)),
            )
            .fetchOneInto(GoalTransaction::class.java)

    fun findRefundByContributionIdAndUserId(
        contributionId: UUID,
        userId: UUID,
    ): GoalTransaction? =
        dsl.selectFrom(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.REVERSAL_OF_GOAL_TRANSACTION_ID.eq(contributionId)
                    .and(Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)),
            )
            .fetchOneInto(GoalTransaction::class.java)

    fun insert(transaction: GoalTransaction): GoalTransaction =
        dsl.insertInto(Tables.GOAL_TRANSACTIONS)
            .set(dsl.newRecord(Tables.GOAL_TRANSACTIONS, transaction))
            .returning()
            .fetchSingleInto(GoalTransaction::class.java)

    fun findSavedAmount(goalId: UUID, userId: UUID): BigDecimal {
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

        return dsl.select(DSL.coalesce(DSL.sum(signedAmount), BigDecimal.ZERO))
            .from(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.GOAL_ID.eq(goalId)
                    .and(Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)),
            )
            .fetchOne(0, BigDecimal::class.java) ?: BigDecimal.ZERO
    }
}

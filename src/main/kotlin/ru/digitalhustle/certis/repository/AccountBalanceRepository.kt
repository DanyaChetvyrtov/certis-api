package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Select
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.GoalTransactionType
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.account.AccountBalanceDelta
import java.math.BigDecimal
import java.util.UUID

@Repository
class AccountBalanceRepository(
    private val dsl: DSLContext,
) {

    private companion object {
        private const val BALANCE_DELTAS_TABLE = "balance_deltas"
        private const val ACCOUNT_ID_COLUMN = "account_id"
        private const val DELTA_COLUMN = "delta"
    }

    fun findBalanceDeltas(
        userId: UUID,
        accountIds: Collection<UUID>,
    ): List<AccountBalanceDelta> {
        if (accountIds.isEmpty()) {
            return emptyList()
        }

        val transactionDeltas = selectTransactionDeltas(userId, accountIds)
        val goalTransactionDeltas = selectGoalTransactionDeltas(userId, accountIds)

        val balanceDeltas = transactionDeltas
            .unionAll(goalTransactionDeltas)
            .asTable(BALANCE_DELTAS_TABLE)

        val accountId = DSL.field(
            DSL.name(BALANCE_DELTAS_TABLE, ACCOUNT_ID_COLUMN),
            UUID::class.java,
        )
        val delta = DSL.field(
            DSL.name(BALANCE_DELTAS_TABLE, DELTA_COLUMN),
            BigDecimal::class.java,
        )

        val totalDelta = DSL.sum(delta)

        return dsl.select(accountId, totalDelta)
            .from(balanceDeltas)
            .groupBy(accountId)
            .fetch()
            .map { record ->
                AccountBalanceDelta(
                    accountId = record[accountId],
                    delta = record[totalDelta] ?: BigDecimal.ZERO,
                )
            }
    }

    private fun selectTransactionDeltas(
        userId: UUID,
        accountIds: Collection<UUID>,
    ): Select<Record2<UUID, BigDecimal>> {
        val transactionSignedAmount = DSL
            .`when`(
                Tables.TRANSACTIONS.TYPE.eq(TransactionType.INCOME.name),
                Tables.TRANSACTIONS.AMOUNT,
            )
            .`when`(
                Tables.TRANSACTIONS.TYPE.eq(TransactionType.EXPENSE.name),
                Tables.TRANSACTIONS.AMOUNT.neg(),
            )
            .otherwise(BigDecimal.ZERO)

        return dsl.select(
            Tables.TRANSACTIONS.ACCOUNT_ID.`as`(ACCOUNT_ID_COLUMN),
            DSL.sum(transactionSignedAmount).`as`(DELTA_COLUMN),
        )
            .from(Tables.TRANSACTIONS)
            .where(
                Tables.TRANSACTIONS.USER_ID.eq(userId)
                    .and(Tables.TRANSACTIONS.ACCOUNT_ID.`in`(accountIds))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull()),
            )
            .groupBy(Tables.TRANSACTIONS.ACCOUNT_ID)
    }

    private fun selectGoalTransactionDeltas(
        userId: UUID,
        accountIds: Collection<UUID>,
    ): Select<Record2<UUID, BigDecimal>> {
        val goalTransactionSignedAmount = DSL
            .`when`(
                Tables.GOAL_TRANSACTIONS.TYPE.eq(GoalTransactionType.CONTRIBUTION.name),
                Tables.GOAL_TRANSACTIONS.AMOUNT.neg(),
            )
            .`when`(
                Tables.GOAL_TRANSACTIONS.TYPE.eq(GoalTransactionType.REFUND.name),
                Tables.GOAL_TRANSACTIONS.AMOUNT,
            )
            .otherwise(BigDecimal.ZERO)

        return dsl.select(
            Tables.GOAL_TRANSACTIONS.ACCOUNT_ID.`as`(ACCOUNT_ID_COLUMN),
            DSL.sum(goalTransactionSignedAmount).`as`(DELTA_COLUMN),
        )
            .from(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)
                    .and(Tables.GOAL_TRANSACTIONS.ACCOUNT_ID.`in`(accountIds)),
            )
            .groupBy(Tables.GOAL_TRANSACTIONS.ACCOUNT_ID)
    }
}

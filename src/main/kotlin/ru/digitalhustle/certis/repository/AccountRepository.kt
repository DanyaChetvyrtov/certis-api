package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.GoalTransactionType
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.UpdateAccountData
import ru.digitalhustle.certis.model.entity.Account
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class AccountRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Account? =
        dsl.selectFrom(Tables.ACCOUNTS)
            .where(
                Tables.ACCOUNTS.ID.eq(id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(userId)),
            )
            .fetchOneInto(Account::class.java)

    fun findAllByUserId(userId: UUID): List<Account> =
        dsl.selectFrom(Tables.ACCOUNTS)
            .where(Tables.ACCOUNTS.USER_ID.eq(userId))
            .orderBy(
                Tables.ACCOUNTS.CLOSED_AT.asc().nullsFirst(),
                Tables.ACCOUNTS.CREATED_AT.desc(),
            )
            .fetchInto(Account::class.java)

    fun findBalanceDeltas(
        userId: UUID,
        accountIds: Collection<UUID>,
    ): Map<UUID, BigDecimal> {
        if (accountIds.isEmpty()) {
            return emptyMap()
        }

        val deltas = findTransactionDeltas(userId, accountIds).toMutableMap()

        findGoalTransactionDeltas(userId, accountIds).forEach { (accountId, delta) ->
            deltas.merge(accountId, delta) { current, addition ->
                current + addition
            }
        }

        return deltas
    }

    private fun findTransactionDeltas(
        userId: UUID,
        accountIds: Collection<UUID>,
    ): Map<UUID, BigDecimal> {
        val transactionTotal = DSL.sum(Tables.TRANSACTIONS.AMOUNT)

        return dsl.select(
            Tables.TRANSACTIONS.ACCOUNT_ID,
            Tables.TRANSACTIONS.TYPE,
            transactionTotal,
        )
            .from(Tables.TRANSACTIONS)
            .where(
                Tables.TRANSACTIONS.USER_ID.eq(userId)
                    .and(Tables.TRANSACTIONS.ACCOUNT_ID.`in`(accountIds)),
            )
            .groupBy(
                Tables.TRANSACTIONS.ACCOUNT_ID,
                Tables.TRANSACTIONS.TYPE,
            )
            .fetch()
            .fold(mutableMapOf<UUID, BigDecimal>()) { deltas, record ->
                val amount = record.get(transactionTotal) ?: BigDecimal.ZERO
                val delta = when (TransactionType.valueOf(record[Tables.TRANSACTIONS.TYPE])) {
                    TransactionType.INCOME -> amount
                    TransactionType.EXPENSE -> amount.negate()
                }

                deltas.merge(
                    record[Tables.TRANSACTIONS.ACCOUNT_ID],
                    delta,
                ) { current, addition ->
                    current + addition
                }
                deltas
            }
    }

    private fun findGoalTransactionDeltas(
        userId: UUID,
        accountIds: Collection<UUID>,
    ): Map<UUID, BigDecimal> {
        val goalTransactionTotal = DSL.sum(Tables.GOAL_TRANSACTIONS.AMOUNT)

        return dsl.select(
            Tables.GOAL_TRANSACTIONS.ACCOUNT_ID,
            Tables.GOAL_TRANSACTIONS.TYPE,
            goalTransactionTotal,
        )
            .from(Tables.GOAL_TRANSACTIONS)
            .where(
                Tables.GOAL_TRANSACTIONS.USER_ID.eq(userId)
                    .and(Tables.GOAL_TRANSACTIONS.ACCOUNT_ID.`in`(accountIds)),
            )
            .groupBy(
                Tables.GOAL_TRANSACTIONS.ACCOUNT_ID,
                Tables.GOAL_TRANSACTIONS.TYPE,
            )
            .fetch()
            .fold(mutableMapOf<UUID, BigDecimal>()) { deltas, record ->
                val amount = record.get(goalTransactionTotal) ?: BigDecimal.ZERO
                val delta = when (GoalTransactionType.valueOf(record[Tables.GOAL_TRANSACTIONS.TYPE])) {
                    GoalTransactionType.CONTRIBUTION -> amount.negate()
                    GoalTransactionType.REFUND -> amount
                }

                deltas.merge(
                    record[Tables.GOAL_TRANSACTIONS.ACCOUNT_ID],
                    delta,
                ) { current, addition ->
                    current + addition
                }
                deltas
            }
    }

    fun insert(account: Account): Account =
        dsl.insertInto(Tables.ACCOUNTS)
            .set(dsl.newRecord(Tables.ACCOUNTS, account))
            .returning()
            .fetchOneInto(Account::class.java)!!

    fun updateActive(account: UpdateAccountData): Account? =
        dsl.update(Tables.ACCOUNTS)
            .set(Tables.ACCOUNTS.NAME, account.name)
            .set(Tables.ACCOUNTS.TYPE, account.type.name)
            .set(Tables.ACCOUNTS.OPENING_BALANCE, account.openingBalance)
            .where(
                Tables.ACCOUNTS.ID.eq(account.id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(account.userId))
                    .and(Tables.ACCOUNTS.CLOSED_AT.isNull()),
            )
            .returning()
            .fetchOneInto(Account::class.java)

    fun close(
        id: UUID,
        userId: UUID,
        closedAt: OffsetDateTime,
    ): Boolean =
        dsl.update(Tables.ACCOUNTS)
            .set(Tables.ACCOUNTS.CLOSED_AT, closedAt)
            .where(
                Tables.ACCOUNTS.ID.eq(id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(userId))
                    .and(Tables.ACCOUNTS.CLOSED_AT.isNull()),
            )
            .execute() > 0
}

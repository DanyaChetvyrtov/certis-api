package ru.digitalhustle.certis.repository

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Records
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.model.account.AccountShortInfo
import ru.digitalhustle.certis.model.transaction.UncategorizedTransaction
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class UncategorizedTransactionRepository(
    private val dsl: DSLContext,
) {

    fun findByUserId(
        userId: UUID,
        filter: UncategorizedTransactionFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): UncategorizedTransactionPage {
        val condition = transactionCondition(userId, filter, monthStart, nextMonthStart)

        return UncategorizedTransactionPage(
            month = filter.month,
            currency = filter.currency,
            type = filter.type,
            items = fetchItems(condition, filter),
            page = filter.page,
            size = filter.size,
            totalElements = fetchTotalElements(condition),
        )
    }

    private fun fetchItems(
        condition: Condition,
        filter: UncategorizedTransactionFilter,
    ): List<UncategorizedTransaction> {
        val account = DSL.row(
            Tables.ACCOUNTS.ID,
            Tables.ACCOUNTS.NAME,
            Tables.ACCOUNTS.TYPE.convertFrom(AccountType::valueOf),
        ).mapping(::AccountShortInfo)

        return dsl
            .select(
                Tables.TRANSACTIONS.ID,
                Tables.TRANSACTIONS.MERCHANT,
                Tables.TRANSACTIONS.NOTE,
                Tables.TRANSACTIONS.AMOUNT,
                Tables.TRANSACTIONS.OCCURRED_AT,
                account,
            )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .where(condition)
            .orderBy(
                Tables.TRANSACTIONS.OCCURRED_AT.desc(),
                Tables.TRANSACTIONS.CREATED_AT.desc(),
                Tables.TRANSACTIONS.ID.desc(),
            )
            .limit(filter.size)
            .offset(filter.page * filter.size)
            .fetch(Records.mapping(::UncategorizedTransaction))
    }

    private fun fetchTotalElements(condition: Condition): Long =
        dsl
            .selectCount()
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(accountJoinCondition())
            .where(condition)
            .fetchOne(0, Long::class.java)
            ?: 0L

    private fun transactionCondition(
        userId: UUID,
        filter: UncategorizedTransactionFilter,
        monthStart: OffsetDateTime,
        nextMonthStart: OffsetDateTime,
    ): Condition {
        var condition = Tables.TRANSACTIONS.USER_ID.eq(userId)
            .and(Tables.TRANSACTIONS.CATEGORY_ID.isNull())
            .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
            .and(Tables.TRANSACTIONS.TRANSFER_ID.isNull())
            .and(Tables.TRANSACTIONS.TYPE.eq(filter.type.name))
            .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(monthStart))
            .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(nextMonthStart))
            .and(Tables.ACCOUNTS.CURRENCY.eq(filter.currency.name))

        filter.accountId?.let { accountId ->
            condition = condition.and(Tables.TRANSACTIONS.ACCOUNT_ID.eq(accountId))
        }

        filter.search
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { search ->
                condition = condition.and(
                    Tables.TRANSACTIONS.MERCHANT.containsIgnoreCase(search)
                        .or(Tables.TRANSACTIONS.NOTE.containsIgnoreCase(search)),
                )
            }

        return condition
    }

    private fun accountJoinCondition(): Condition =
        Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
            .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID))
}

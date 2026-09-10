package ru.digitalhustle.certis.features.transaction.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.features.transaction.query.model.TransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.TransactionPage
import java.util.UUID

@Repository
class TransactionQueryRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Transaction? =
        dsl.selectFrom(Tables.TRANSACTIONS)
            .where(
                Tables.TRANSACTIONS.ID.eq(id)
                    .and(Tables.TRANSACTIONS.USER_ID.eq(userId))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull()),
            )
            .fetchOneInto(Transaction::class.java)

    fun findAllByUserId(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionPage {
        var condition = Tables.TRANSACTIONS.USER_ID.eq(userId)
            .and(Tables.TRANSACTIONS.DELETED_AT.isNull())

        filter.accountId?.let {
            condition = condition.and(Tables.TRANSACTIONS.ACCOUNT_ID.eq(it))
        }
        filter.categoryId?.let {
            condition = condition.and(Tables.TRANSACTIONS.CATEGORY_ID.eq(it))
        }
        filter.type?.let {
            condition = condition.and(Tables.TRANSACTIONS.TYPE.eq(it.name))
        }
        filter.from?.let {
            condition = condition.and(Tables.TRANSACTIONS.OCCURRED_AT.ge(it))
        }
        filter.to?.let {
            condition = condition.and(Tables.TRANSACTIONS.OCCURRED_AT.le(it))
        }

        val items = dsl.selectFrom(Tables.TRANSACTIONS)
            .where(condition)
            .orderBy(
                Tables.TRANSACTIONS.OCCURRED_AT.desc(),
                Tables.TRANSACTIONS.CREATED_AT.desc(),
                Tables.TRANSACTIONS.ID.desc(),
            )
            .limit(filter.size)
            .offset(filter.page * filter.size)
            .fetchInto(Transaction::class.java)

        val totalElements = dsl.fetchCount(Tables.TRANSACTIONS, condition).toLong()

        return TransactionPage(
            items = items,
            page = filter.page,
            size = filter.size,
            totalElements = totalElements,
        )
    }
}

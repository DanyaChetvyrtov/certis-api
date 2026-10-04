package ru.digitalhustle.certis.features.transaction.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.features.transaction.query.model.TransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.TransactionPage
import ru.digitalhustle.certis.features.transaction.query.specification.TransactionSpecification
import ru.digitalhustle.certis.features.transaction.query.specification.TransactionSpecifications
import java.util.UUID

@Repository
class TransactionQueryRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Transaction? {
        val specification =
            TransactionSpecifications.hasId(id) and
                TransactionSpecifications.ownedBy(userId) and
                TransactionSpecifications.active()

        return dsl.selectFrom(Tables.TRANSACTIONS)
            .where(specification.toCondition())
            .fetchOneInto(Transaction::class.java)
    }

    fun findAllByUserId(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionPage {
        val condition = specification(userId, filter).toCondition()

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

    private fun specification(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionSpecification {
        var specification =
            TransactionSpecifications.ownedBy(userId) and
                TransactionSpecifications.active()

        filter.accountId?.let {
            specification = specification and TransactionSpecifications.fromAccount(it)
        }
        filter.categoryId?.let {
            specification = specification and TransactionSpecifications.inCategory(it)
        }
        filter.type?.let {
            specification = specification and TransactionSpecifications.hasType(it)
        }
        filter.from?.let {
            specification = specification and TransactionSpecifications.occurredAtOrAfter(it)
        }
        filter.to?.let {
            specification = specification and TransactionSpecifications.occurredAtOrBefore(it)
        }

        return specification
    }
}

package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.transaction.TransactionCategoryAssignment
import ru.digitalhustle.certis.model.transaction.TransactionFilter
import ru.digitalhustle.certis.model.transaction.TransactionPage
import ru.digitalhustle.certis.model.transaction.UpdateTransactionData
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class TransactionRepository(
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

    fun findByIdAndUserIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Transaction? =
        dsl.selectFrom(Tables.TRANSACTIONS)
            .where(
                Tables.TRANSACTIONS.ID.eq(id)
                    .and(Tables.TRANSACTIONS.USER_ID.eq(userId))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull()),
            )
            .forUpdate()
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

    fun existsIncludingDeletedByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(Tables.TRANSACTIONS)
                .where(
                    Tables.TRANSACTIONS.ID.eq(id)
                        .and(Tables.TRANSACTIONS.USER_ID.eq(userId)),
                ),
        )

    fun findByRecurringTemplateIdAndScheduledFor(
        recurringTransactionTemplateId: UUID,
        scheduledFor: LocalDate,
    ): Transaction? =
        dsl.selectFrom(Tables.TRANSACTIONS)
            .where(
                Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID.eq(recurringTransactionTemplateId)
                    .and(Tables.TRANSACTIONS.SCHEDULED_FOR.eq(scheduledFor)),
            )
            .fetchOneInto(Transaction::class.java)

    fun findAllByIdsAndUserIdForUpdate(
        ids: Collection<UUID>,
        userId: UUID,
    ): List<Transaction> =
        dsl.selectFrom(Tables.TRANSACTIONS)
            .where(
                Tables.TRANSACTIONS.ID.`in`(ids)
                    .and(Tables.TRANSACTIONS.USER_ID.eq(userId))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull()),
            )
            .orderBy(Tables.TRANSACTIONS.ID)
            .forUpdate()
            .fetchInto(Transaction::class.java)

    fun insert(transaction: Transaction): Transaction =
        dsl.insertInto(Tables.TRANSACTIONS)
            .set(dsl.newRecord(Tables.TRANSACTIONS, transaction))
            .returning()
            .fetchOneInto(Transaction::class.java)!!

    fun insertIgnoringConflict(transaction: Transaction): Transaction? =
        dsl.insertInto(Tables.TRANSACTIONS)
            .set(dsl.newRecord(Tables.TRANSACTIONS, transaction))
            .onConflictDoNothing()
            .returning()
            .fetchOneInto(Transaction::class.java)

    fun updateActive(
        transaction: UpdateTransactionData,
        updatedAt: OffsetDateTime,
    ): Transaction =
        dsl.update(Tables.TRANSACTIONS)
            .set(Tables.TRANSACTIONS.ACCOUNT_ID, transaction.accountId)
            .set(Tables.TRANSACTIONS.TYPE, transaction.type.name)
            .set(Tables.TRANSACTIONS.AMOUNT, transaction.amount)
            .set(Tables.TRANSACTIONS.CATEGORY_ID, transaction.categoryId)
            .set(Tables.TRANSACTIONS.MERCHANT, transaction.merchant)
            .set(Tables.TRANSACTIONS.NOTE, transaction.note)
            .set(Tables.TRANSACTIONS.OCCURRED_AT, transaction.occurredAt)
            .set(Tables.TRANSACTIONS.UPDATED_AT, updatedAt)
            .where(
                Tables.TRANSACTIONS.ID.eq(transaction.id)
                    .and(Tables.TRANSACTIONS.USER_ID.eq(transaction.userId))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull()),
            )
            .returning()
            .fetchOneInto(Transaction::class.java)!!

    fun assignCategories(
        assignments: Collection<TransactionCategoryAssignment>,
        userId: UUID,
        updatedAt: OffsetDateTime,
    ): Int {
        if (assignments.isEmpty()) {
            return 0
        }

        val queries = assignments.map { assignment ->
            dsl.update(Tables.TRANSACTIONS)
                .set(Tables.TRANSACTIONS.CATEGORY_ID, assignment.categoryId)
                .set(Tables.TRANSACTIONS.UPDATED_AT, updatedAt)
                .where(
                    Tables.TRANSACTIONS.ID.eq(assignment.transactionId)
                        .and(Tables.TRANSACTIONS.USER_ID.eq(userId))
                        .and(Tables.TRANSACTIONS.CATEGORY_ID.isNull())
                        .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
                        .and(Tables.TRANSACTIONS.TRANSFER_ID.isNull()),
                )
        }

        return dsl.batch(queries).execute().sum()
    }

    fun softDelete(
        id: UUID,
        userId: UUID,
        deletedAt: OffsetDateTime,
    ): Boolean =
        dsl.update(Tables.TRANSACTIONS)
            .set(Tables.TRANSACTIONS.DELETED_AT, deletedAt)
            .where(
                Tables.TRANSACTIONS.ID.eq(id)
                    .and(Tables.TRANSACTIONS.USER_ID.eq(userId))
                    .and(Tables.TRANSACTIONS.DELETED_AT.isNull()),
            )
            .execute() > 0
}

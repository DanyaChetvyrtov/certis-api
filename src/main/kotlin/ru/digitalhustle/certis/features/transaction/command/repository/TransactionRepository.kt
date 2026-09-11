package ru.digitalhustle.certis.features.transaction.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.transaction.command.model.TransactionCategoryAssignment
import ru.digitalhustle.certis.features.transaction.command.model.UpdateTransactionData
import ru.digitalhustle.certis.features.transaction.model.Transaction
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class TransactionRepository(
    private val dsl: DSLContext,
) {

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
            .fetchOneInto(Transaction::class.java)
            ?: error("Transaction insert returned no row")

    fun insertIgnoringConflict(transaction: Transaction): Transaction? =
        dsl.insertInto(Tables.TRANSACTIONS)
            .set(dsl.newRecord(Tables.TRANSACTIONS, transaction))
            .onConflictDoNothing()
            .returning()
            .fetchOneInto(Transaction::class.java)

    fun updateActive(
        transaction: UpdateTransactionData,
        updatedAt: OffsetDateTime,
    ): Transaction? =
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
            .fetchOneInto(Transaction::class.java)

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

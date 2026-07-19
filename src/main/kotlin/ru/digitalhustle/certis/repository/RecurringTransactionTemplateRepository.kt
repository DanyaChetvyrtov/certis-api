package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class RecurringTransactionTemplateRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): RecurringTransactionTemplate? =
        dsl.selectFrom(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .where(
                Tables.RECURRING_TRANSACTION_TEMPLATES.ID.eq(id)
                    .and(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(userId)),
            )
            .fetchOneInto(RecurringTransactionTemplate::class.java)

    fun findByIdAndUserIdForUpdate(
        id: UUID,
        userId: UUID,
    ): RecurringTransactionTemplate? =
        dsl.selectFrom(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .where(
                Tables.RECURRING_TRANSACTION_TEMPLATES.ID.eq(id)
                    .and(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(userId)),
            )
            .forUpdate()
            .fetchOneInto(RecurringTransactionTemplate::class.java)

    fun findByIdForUpdate(id: UUID): RecurringTransactionTemplate? =
        dsl.selectFrom(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .where(Tables.RECURRING_TRANSACTION_TEMPLATES.ID.eq(id))
            .forUpdate()
            .fetchOneInto(RecurringTransactionTemplate::class.java)

    fun findAllByUserId(userId: UUID): List<RecurringTransactionTemplate> =
        dsl.selectFrom(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .where(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(userId))
            .orderBy(
                Tables.RECURRING_TRANSACTION_TEMPLATES.CREATED_AT.desc(),
                Tables.RECURRING_TRANSACTION_TEMPLATES.ID.desc(),
            )
            .fetchInto(RecurringTransactionTemplate::class.java)

    fun findDueForUpdate(
        currentDate: LocalDate,
        currentTime: OffsetDateTime,
        excludedTemplateIds: Set<UUID>,
    ): RecurringTransactionTemplate? {
        var condition = Tables.RECURRING_TRANSACTION_TEMPLATES.STATUS.eq(
            RecurringTransactionTemplateStatus.ACTIVE.name,
        )
            .and(Tables.RECURRING_TRANSACTION_TEMPLATES.NEXT_RUN_DATE.le(currentDate))
            .and(RETRY_AFTER.isNull.or(RETRY_AFTER.le(currentTime)))

        if (excludedTemplateIds.isNotEmpty()) {
            condition = condition.and(Tables.RECURRING_TRANSACTION_TEMPLATES.ID.notIn(excludedTemplateIds))
        }

        return dsl.selectFrom(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .where(condition)
            .orderBy(
                Tables.RECURRING_TRANSACTION_TEMPLATES.NEXT_RUN_DATE.asc(),
                Tables.RECURRING_TRANSACTION_TEMPLATES.ID.asc(),
            )
            .limit(1)
            .forUpdate()
            .skipLocked()
            .fetchOneInto(RecurringTransactionTemplate::class.java)
    }

    fun findFailureCountForUpdate(
        id: UUID,
        scheduledFor: LocalDate,
    ): Int? =
        dsl.select(CONSECUTIVE_FAILURES)
            .from(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .where(executableOccurrenceCondition(id, scheduledFor))
            .forUpdate()
            .fetchOne(CONSECUTIVE_FAILURES)

    fun existsSchedulableByAccountIdAndUserId(
        accountId: UUID,
        userId: UUID,
    ): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(Tables.RECURRING_TRANSACTION_TEMPLATES)
                .where(
                    Tables.RECURRING_TRANSACTION_TEMPLATES.ACCOUNT_ID.eq(accountId)
                        .and(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(userId))
                        .and(
                            Tables.RECURRING_TRANSACTION_TEMPLATES.STATUS.`in`(
                                RecurringTransactionTemplateStatus.ACTIVE.name,
                                RecurringTransactionTemplateStatus.PAUSED.name,
                            ),
                        ),
                ),
        )

    fun insert(template: RecurringTransactionTemplate): RecurringTransactionTemplate =
        checkNotNull(
            dsl.insertInto(Tables.RECURRING_TRANSACTION_TEMPLATES)
                .set(dsl.newRecord(Tables.RECURRING_TRANSACTION_TEMPLATES, template))
                .returning()
                .fetchOneInto(RecurringTransactionTemplate::class.java),
        )

    fun update(template: RecurringTransactionTemplate): RecurringTransactionTemplate? =
        dsl.update(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.ACCOUNT_ID, template.accountId)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.CATEGORY_ID, template.categoryId)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.NAME, template.name)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.TYPE, template.type.name)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.AMOUNT, template.amount)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.MERCHANT, template.merchant)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.NOTE, template.note)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.STATUS, template.status.name)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.FREQUENCY, template.frequency.name)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.INTERVAL_COUNT, template.intervalCount)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.START_DATE, template.startDate)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.END_DATE, template.endDate)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.LAST_RUN_DATE, template.lastRunDate)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.NEXT_RUN_DATE, template.nextRunDate)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.UPDATED_AT, template.updatedAt)
            .set(CONSECUTIVE_FAILURES, 0)
            .setNull(RETRY_AFTER)
            .where(
                Tables.RECURRING_TRANSACTION_TEMPLATES.ID.eq(template.id)
                    .and(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(template.userId)),
            )
            .returning()
            .fetchOneInto(RecurringTransactionTemplate::class.java)

    fun recordExecutionFailure(
        id: UUID,
        scheduledFor: LocalDate,
        consecutiveFailures: Int,
        retryAfter: OffsetDateTime,
    ): Boolean =
        dsl.update(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .set(CONSECUTIVE_FAILURES, consecutiveFailures)
            .set(RETRY_AFTER, retryAfter)
            .where(executableOccurrenceCondition(id, scheduledFor))
            .execute() > 0

    private fun executableOccurrenceCondition(
        id: UUID,
        scheduledFor: LocalDate,
    ) = Tables.RECURRING_TRANSACTION_TEMPLATES.ID.eq(id)
        .and(
            Tables.RECURRING_TRANSACTION_TEMPLATES.STATUS.eq(
                RecurringTransactionTemplateStatus.ACTIVE.name,
            ),
        )
        .and(Tables.RECURRING_TRANSACTION_TEMPLATES.NEXT_RUN_DATE.eq(scheduledFor))

    private companion object {
        private val CONSECUTIVE_FAILURES = DSL.field(
            DSL.name("keeper", "recurring_transaction_templates", "consecutive_failures"),
            Int::class.java,
        )
        private val RETRY_AFTER = DSL.field(
            DSL.name("keeper", "recurring_transaction_templates", "retry_after"),
            OffsetDateTime::class.java,
        )
    }
}

package ru.digitalhustle.certis.features.transaction.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import java.util.UUID

@Repository
class RecurringTransactionTemplateQueryRepository(
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

    fun findAllByUserId(userId: UUID): List<RecurringTransactionTemplate> =
        dsl.selectFrom(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .where(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(userId))
            .orderBy(
                Tables.RECURRING_TRANSACTION_TEMPLATES.CREATED_AT.desc(),
                Tables.RECURRING_TRANSACTION_TEMPLATES.ID.desc(),
            )
            .fetchInto(RecurringTransactionTemplate::class.java)

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
}

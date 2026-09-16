package ru.digitalhustle.certis.features.transaction.query.repository

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.query.model.ForecastActualTransaction
import ru.digitalhustle.certis.features.transaction.query.model.ForecastHistoricalCategoryAmount
import ru.digitalhustle.certis.features.transaction.query.model.ForecastRecurringTemplate
import ru.digitalhustle.certis.features.transaction.query.model.ForecastTransactionCategory
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@Repository
class BudgetForecastSourceQueryRepository(
    private val dsl: DSLContext,
) {

    fun findRecurringTemplates(userId: UUID, currency: Currency): List<ForecastRecurringTemplate> =
        dsl.select(
            Tables.RECURRING_TRANSACTION_TEMPLATES.asterisk(),
            Tables.CATEGORIES.ID,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .join(Tables.ACCOUNTS)
            .on(
                Tables.ACCOUNTS.ID.eq(Tables.RECURRING_TRANSACTION_TEMPLATES.ACCOUNT_ID)
                    .and(Tables.ACCOUNTS.USER_ID.eq(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID)),
            )
            .leftJoin(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.RECURRING_TRANSACTION_TEMPLATES.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID)),
            )
            .where(
                Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(userId)
                    .and(Tables.ACCOUNTS.CURRENCY.eq(currency.name))
                    .and(
                        Tables.RECURRING_TRANSACTION_TEMPLATES.STATUS.eq(
                            RecurringTransactionTemplateStatus.ACTIVE.name,
                        ),
                    )
                    .and(Tables.RECURRING_TRANSACTION_TEMPLATES.NEXT_RUN_DATE.isNotNull()),
            )
            .orderBy(Tables.RECURRING_TRANSACTION_TEMPLATES.ID.asc())
            .fetch(::toRecurringTemplate)

    fun findActualOperations(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
        zoneId: ZoneId,
    ): List<ForecastActualTransaction> =
        dsl.select(
            Tables.TRANSACTIONS.asterisk(),
            Tables.CATEGORIES.ID,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(
                Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
                    .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID)),
            )
            .leftJoin(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.TRANSACTIONS.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.TRANSACTIONS.USER_ID)),
            )
            .where(
                baseTransactionCondition(userId, currency)
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(from))
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(toExclusive)),
            )
            .orderBy(Tables.TRANSACTIONS.OCCURRED_AT.asc(), Tables.TRANSACTIONS.ID.asc())
            .fetch { record -> toActualTransaction(record, zoneId) }

    fun findHistoricalCategoryAmounts(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
        zoneId: ZoneId,
    ): List<ForecastHistoricalCategoryAmount> {
        val month = DSL.field(
            "date_trunc('month', {0} at time zone {1})::date",
            SQLDataType.LOCALDATE,
            Tables.TRANSACTIONS.OCCURRED_AT,
            DSL.inline(zoneId.id),
        )
        val totalAmount = DSL.sum(Tables.TRANSACTIONS.AMOUNT).`as`("total_amount")

        return dsl.select(
            Tables.TRANSACTIONS.TYPE,
            Tables.CATEGORIES.ID,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
            month,
            totalAmount,
        )
            .from(Tables.TRANSACTIONS)
            .join(Tables.ACCOUNTS)
            .on(
                Tables.ACCOUNTS.ID.eq(Tables.TRANSACTIONS.ACCOUNT_ID)
                    .and(Tables.ACCOUNTS.USER_ID.eq(Tables.TRANSACTIONS.USER_ID)),
            )
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.TRANSACTIONS.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.TRANSACTIONS.USER_ID)),
            )
            .where(
                baseTransactionCondition(userId, currency)
                    .and(Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID.isNull())
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(from))
                    .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(toExclusive)),
            )
            .groupBy(
                Tables.TRANSACTIONS.TYPE,
                Tables.CATEGORIES.ID,
                Tables.CATEGORIES.NAME,
                Tables.CATEGORIES.ICON,
                Tables.CATEGORIES.COLOR,
                month,
            )
            .orderBy(month.asc(), Tables.CATEGORIES.ID.asc())
            .fetch { record ->
                ForecastHistoricalCategoryAmount(
                    type = TransactionType.valueOf(record[Tables.TRANSACTIONS.TYPE]),
                    category = requireNotNull(toCategory(record)),
                    month = YearMonth.from(record[month]),
                    amount = requireNotNull(record[totalAmount]),
                )
            }
    }

    private fun baseTransactionCondition(userId: UUID, currency: Currency) =
        Tables.TRANSACTIONS.USER_ID.eq(userId)
            .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
            .and(Tables.TRANSACTIONS.TRANSFER_ID.isNull())
            .and(Tables.ACCOUNTS.CURRENCY.eq(currency.name))

    private fun toRecurringTemplate(record: Record): ForecastRecurringTemplate =
        ForecastRecurringTemplate(
            id = requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.ID]),
            name = requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.NAME]),
            type = TransactionType.valueOf(requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.TYPE])),
            amount = requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.AMOUNT]),
            category = toCategory(record),
            frequency = RecurringTransactionFrequency.valueOf(
                requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.FREQUENCY]),
            ),
            intervalCount = requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.INTERVAL_COUNT]),
            startDate = requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.START_DATE]),
            endDate = record[Tables.RECURRING_TRANSACTION_TEMPLATES.END_DATE],
            nextRunDate = requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.NEXT_RUN_DATE]),
            updatedAt = requireNotNull(record[Tables.RECURRING_TRANSACTION_TEMPLATES.UPDATED_AT]),
        )

    private fun toActualTransaction(record: Record, zoneId: ZoneId): ForecastActualTransaction =
        ForecastActualTransaction(
            id = requireNotNull(record[Tables.TRANSACTIONS.ID]),
            type = TransactionType.valueOf(requireNotNull(record[Tables.TRANSACTIONS.TYPE])),
            amount = requireNotNull(record[Tables.TRANSACTIONS.AMOUNT]),
            title = record[Tables.TRANSACTIONS.MERCHANT] ?: record[Tables.TRANSACTIONS.NOTE] ?: "Transaction",
            category = toCategory(record),
            occurredOn = requireNotNull(record[Tables.TRANSACTIONS.OCCURRED_AT])
                .atZoneSameInstant(zoneId)
                .toLocalDate(),
            recurringTemplateId = record[Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID],
        )

    private fun toCategory(record: Record): ForecastTransactionCategory? =
        record[Tables.CATEGORIES.ID]?.let { categoryId ->
            ForecastTransactionCategory(
                id = categoryId,
                name = requireNotNull(record[Tables.CATEGORIES.NAME]),
                icon = requireNotNull(record[Tables.CATEGORIES.ICON]),
                color = requireNotNull(record[Tables.CATEGORIES.COLOR]),
            )
        }
}

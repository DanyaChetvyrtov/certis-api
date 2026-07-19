package ru.digitalhustle.certis.features.budget.command.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.BudgetForecastItem
import java.util.UUID

@Repository
class BudgetForecastItemRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun insertAll(forecastRevisionId: UUID, userId: UUID, items: List<BudgetForecastItem>) {
        items.forEach { item -> insert(forecastRevisionId, userId, item) }
    }

    private fun insert(forecastRevisionId: UUID, userId: UUID, item: BudgetForecastItem) {
        val sourcePayload = item.sourcePayload + listOfNotNull(
            item.recurring?.let { "frequency" to it.frequency.name },
        ).toMap()
        dsl.insertInto(Tables.BUDGET_FORECAST_ITEMS)
            .set(Tables.BUDGET_FORECAST_ITEMS.ID, item.id)
            .set(Tables.BUDGET_FORECAST_ITEMS.USER_ID, userId)
            .set(Tables.BUDGET_FORECAST_ITEMS.FORECAST_REVISION_ID, forecastRevisionId)
            .set(Tables.BUDGET_FORECAST_ITEMS.CATEGORY_ID, item.category?.id)
            .set(Tables.BUDGET_FORECAST_ITEMS.CATEGORY_TYPE, item.category?.type?.name)
            .set(Tables.BUDGET_FORECAST_ITEMS.SOURCE_KEY, item.sourceKey)
            .set(Tables.BUDGET_FORECAST_ITEMS.SOURCE_TYPE, item.sourceType.name)
            .set(Tables.BUDGET_FORECAST_ITEMS.OPERATION_TYPE, item.operationType.name)
            .set(Tables.BUDGET_FORECAST_ITEMS.TITLE, item.title)
            .set(Tables.BUDGET_FORECAST_ITEMS.INCLUDED, item.included)
            .set(Tables.BUDGET_FORECAST_ITEMS.CONSTRAINT_ROLE, item.defaultConstraintRole?.name)
            .set(Tables.BUDGET_FORECAST_ITEMS.CONFIDENCE, item.confidence.name)
            .set(Tables.BUDGET_FORECAST_ITEMS.ORIGINAL_AMOUNT, item.originalAmount)
            .set(Tables.BUDGET_FORECAST_ITEMS.EFFECTIVE_AMOUNT, item.effectiveAmount)
            .set(Tables.BUDGET_FORECAST_ITEMS.SCHEDULED_FOR, item.expectedDate)
            .set(Tables.BUDGET_FORECAST_ITEMS.RECURRING_TRANSACTION_TEMPLATE_ID, item.recurring?.templateId)
            .set(Tables.BUDGET_FORECAST_ITEMS.TRANSACTION_ID, item.transactionId)
            .set(Tables.BUDGET_FORECAST_ITEMS.MANUAL_CLIENT_ID, item.manualClientId)
            .set(Tables.BUDGET_FORECAST_ITEMS.SOURCE_UPDATED_AT, item.sourceUpdatedAt)
            .set(Tables.BUDGET_FORECAST_ITEMS.HISTORY_MONTHS_USED, item.history?.monthsUsed?.toShort())
            .set(Tables.BUDGET_FORECAST_ITEMS.HISTORY_METHOD, item.history?.method)
            .set(
                Tables.BUDGET_FORECAST_ITEMS.SOURCE_PAYLOAD,
                JSONB.valueOf(objectMapper.writeValueAsString(sourcePayload)),
            )
            .execute()
    }
}

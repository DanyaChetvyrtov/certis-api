package ru.digitalhustle.certis.features.budget.command.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import java.util.UUID

@Repository
class BudgetForecastRevisionRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun nextRevision(planId: UUID): Int =
        requireNotNull(
            dsl.select(DSL.coalesce(DSL.max(Tables.BUDGET_FORECAST_REVISIONS.REVISION), 0).plus(1))
                .from(Tables.BUDGET_FORECAST_REVISIONS)
                .where(Tables.BUDGET_FORECAST_REVISIONS.PLAN_ID.eq(planId))
                .fetchSingle(0, Int::class.java),
        )

    fun insert(forecast: BudgetForecast) {
        dsl.insertInto(Tables.BUDGET_FORECAST_REVISIONS)
            .set(Tables.BUDGET_FORECAST_REVISIONS.ID, forecast.id)
            .set(Tables.BUDGET_FORECAST_REVISIONS.USER_ID, forecast.userId)
            .set(Tables.BUDGET_FORECAST_REVISIONS.PLAN_ID, forecast.planId)
            .set(Tables.BUDGET_FORECAST_REVISIONS.REVISION, forecast.revision)
            .set(Tables.BUDGET_FORECAST_REVISIONS.SOURCE_FINGERPRINT, forecast.sourceFingerprint)
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_FINGERPRINT, forecast.forecastFingerprint)
            .set(Tables.BUDGET_FORECAST_REVISIONS.HISTORY_MONTHS_USED, forecast.historyWindow.monthsUsed.toShort())
            .set(Tables.BUDGET_FORECAST_REVISIONS.HISTORY_METHOD, forecast.historyWindow.method)
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_INCOME, forecast.summary.forecastIncome)
            .set(Tables.BUDGET_FORECAST_REVISIONS.RECURRING_INCOME, forecast.summary.recurringIncome)
            .set(Tables.BUDGET_FORECAST_REVISIONS.RECURRING_EXPENSES, forecast.summary.recurringExpenses)
            .set(Tables.BUDGET_FORECAST_REVISIONS.FLEXIBLE_ESTIMATE, forecast.summary.flexibleEstimate)
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_EXPENSES, forecast.summary.forecastExpenses)
            .set(Tables.BUDGET_FORECAST_REVISIONS.FORECAST_SAVINGS, forecast.summary.forecastSavings)
            .set(
                Tables.BUDGET_FORECAST_REVISIONS.SOURCE_SNAPSHOT,
                JSONB.valueOf(objectMapper.writeValueAsString(sourceSnapshot(forecast))),
            )
            .set(Tables.BUDGET_FORECAST_REVISIONS.CONFIRMED_AT, forecast.confirmedAt)
            .execute()
    }

    private fun sourceSnapshot(forecast: BudgetForecast): Map<String, Any?> =
        mapOf(
            "historyWindow" to forecast.historyWindow,
            "sourceFingerprint" to forecast.sourceFingerprint,
            "items" to forecast.items
                .filterNot { item -> item.sourceType.name == MANUAL_SOURCE_TYPE }
                .sortedBy { item -> item.sourceKey }
                .map { item ->
                    mapOf(
                        "sourceKey" to item.sourceKey,
                        "sourceType" to item.sourceType,
                        "operationType" to item.operationType,
                        "categoryId" to item.category?.id,
                        "expectedDate" to item.expectedDate,
                        "originalAmount" to item.originalAmount,
                        "sourceUpdatedAt" to item.sourceUpdatedAt,
                        "sourcePayload" to item.sourcePayload,
                    )
                },
        )

    companion object {
        private const val MANUAL_SOURCE_TYPE = "MANUAL"
    }
}

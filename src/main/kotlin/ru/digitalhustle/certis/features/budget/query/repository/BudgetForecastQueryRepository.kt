package ru.digitalhustle.certis.features.budget.query.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastConfidence
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastSourceType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastBaselineAllocation
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetForecastFrequency
import ru.digitalhustle.certis.features.budget.model.BudgetForecastHistorySource
import ru.digitalhustle.certis.features.budget.model.BudgetForecastHistoryWindow
import ru.digitalhustle.certis.features.budget.model.BudgetForecastItem
import ru.digitalhustle.certis.features.budget.model.BudgetForecastRecurringSource
import ru.digitalhustle.certis.features.budget.model.BudgetForecastSummary
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.time.YearMonth
import java.util.UUID

@Repository
class BudgetForecastQueryRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun findLatestByPlanIdAndUserId(planId: UUID, userId: UUID): BudgetForecast? {
        val revision = dsl.select(
            *Tables.BUDGET_FORECAST_REVISIONS.fields(),
            Tables.BUDGET_PLANS.BUDGET_MONTH,
            Tables.BUDGET_PLANS.VERSION,
        )
            .from(Tables.BUDGET_FORECAST_REVISIONS)
            .join(Tables.BUDGET_PLANS)
            .on(
                Tables.BUDGET_PLANS.ID.eq(Tables.BUDGET_FORECAST_REVISIONS.PLAN_ID)
                    .and(Tables.BUDGET_PLANS.USER_ID.eq(Tables.BUDGET_FORECAST_REVISIONS.USER_ID)),
            )
            .where(
                Tables.BUDGET_FORECAST_REVISIONS.PLAN_ID.eq(planId)
                    .and(Tables.BUDGET_FORECAST_REVISIONS.USER_ID.eq(userId)),
            )
            .orderBy(Tables.BUDGET_FORECAST_REVISIONS.REVISION.desc())
            .limit(1)
            .fetchOne() ?: return null

        val forecastId = requireNotNull(revision[Tables.BUDGET_FORECAST_REVISIONS.ID])
        val items = findItems(forecastId, userId)
        val historyMonths = requireNotNull(revision[Tables.BUDGET_FORECAST_REVISIONS.HISTORY_MONTHS_USED]).toInt()
        val budgetMonth = YearMonth.from(requireNotNull(revision[Tables.BUDGET_PLANS.BUDGET_MONTH]))
        return BudgetForecast(
            id = forecastId,
            userId = userId,
            planId = planId,
            revision = requireNotNull(revision[Tables.BUDGET_FORECAST_REVISIONS.REVISION]),
            status = BudgetForecastStatus.CURRENT,
            sourceFingerprint = requireNotNull(revision[Tables.BUDGET_FORECAST_REVISIONS.SOURCE_FINGERPRINT]),
            forecastFingerprint = requireNotNull(revision[Tables.BUDGET_FORECAST_REVISIONS.FORECAST_FINGERPRINT]),
            historyWindow = BudgetForecastHistoryWindow(
                fromMonth = budgetMonth.minusMonths(historyMonths.toLong()),
                toMonth = budgetMonth.minusMonths(1),
                monthsUsed = historyMonths,
                method = requireNotNull(revision[Tables.BUDGET_FORECAST_REVISIONS.HISTORY_METHOD]),
            ),
            summary = toSummary(revision, items),
            items = items,
            planVersion = requireNotNull(revision[Tables.BUDGET_PLANS.VERSION]),
            confirmedAt = requireNotNull(revision[Tables.BUDGET_FORECAST_REVISIONS.CONFIRMED_AT]),
        )
    }

    fun findBaselineAllocations(budgetId: UUID?, userId: UUID): List<BudgetForecastBaselineAllocation> {
        if (budgetId == null) return emptyList()

        return dsl.select(
            Tables.BUDGET_CATEGORIES.CATEGORY_ID,
            Tables.BUDGET_CATEGORIES.LIMIT_AMOUNT,
            Tables.BUDGET_CATEGORIES.EXPENSE_TYPE,
            Tables.CATEGORIES.TYPE,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.BUDGET_CATEGORIES)
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.BUDGET_CATEGORIES.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.BUDGET_CATEGORIES.USER_ID)),
            )
            .where(
                Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(budgetId)
                    .and(Tables.BUDGET_CATEGORIES.USER_ID.eq(userId)),
            )
            .orderBy(Tables.CATEGORIES.NAME.asc())
            .fetch { record ->
                BudgetForecastBaselineAllocation(
                    category = BudgetForecastCategory(
                        id = requireNotNull(record[Tables.BUDGET_CATEGORIES.CATEGORY_ID]),
                        type = CategoryType.valueOf(requireNotNull(record[Tables.CATEGORIES.TYPE])),
                        name = requireNotNull(record[Tables.CATEGORIES.NAME]),
                        icon = requireNotNull(record[Tables.CATEGORIES.ICON]),
                        color = requireNotNull(record[Tables.CATEGORIES.COLOR]),
                    ),
                    limitAmount = requireNotNull(record[Tables.BUDGET_CATEGORIES.LIMIT_AMOUNT]),
                    fixed = BudgetExpenseType.valueOf(
                        requireNotNull(record[Tables.BUDGET_CATEGORIES.EXPENSE_TYPE]),
                    ) == BudgetExpenseType.FIXED,
                )
            }
    }

    private fun findItems(forecastId: UUID, userId: UUID): List<BudgetForecastItem> =
        dsl.select(
            *Tables.BUDGET_FORECAST_ITEMS.fields(),
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.BUDGET_FORECAST_ITEMS)
            .leftJoin(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.BUDGET_FORECAST_ITEMS.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.BUDGET_FORECAST_ITEMS.USER_ID)),
            )
            .where(
                Tables.BUDGET_FORECAST_ITEMS.FORECAST_REVISION_ID.eq(forecastId)
                    .and(Tables.BUDGET_FORECAST_ITEMS.USER_ID.eq(userId)),
            )
            .orderBy(Tables.BUDGET_FORECAST_ITEMS.SOURCE_KEY.asc())
            .fetch(::toItem)

    private fun toItem(record: Record): BudgetForecastItem {
        val sourceType = BudgetForecastSourceType.valueOf(
            requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.SOURCE_TYPE]),
        )
        val operationType = BudgetForecastOperationType.valueOf(
            requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.OPERATION_TYPE]),
        )
        return BudgetForecastItem(
            id = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.ID]),
            sourceKey = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.SOURCE_KEY]),
            sourceType = sourceType,
            operationType = operationType,
            title = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.TITLE]),
            category = toCategory(record),
            expectedDate = record[Tables.BUDGET_FORECAST_ITEMS.SCHEDULED_FOR],
            originalAmount = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.ORIGINAL_AMOUNT]),
            effectiveAmount = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.EFFECTIVE_AMOUNT]),
            included = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.INCLUDED]),
            defaultConstraintRole = record[Tables.BUDGET_FORECAST_ITEMS.CONSTRAINT_ROLE]
                ?.let(BudgetConstraintRole::valueOf),
            confidence = BudgetForecastConfidence.valueOf(
                requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.CONFIDENCE]),
            ),
            recurring = toRecurringSource(record, sourceType),
            transactionId = record[Tables.BUDGET_FORECAST_ITEMS.TRANSACTION_ID],
            manualClientId = record[Tables.BUDGET_FORECAST_ITEMS.MANUAL_CLIENT_ID],
            sourceUpdatedAt = record[Tables.BUDGET_FORECAST_ITEMS.SOURCE_UPDATED_AT],
            history = toHistorySource(record, sourceType),
            sourcePayload = objectMapper.readValue(
                requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.SOURCE_PAYLOAD]).data(),
                object : TypeReference<Map<String, Any?>>() {},
            ),
        )
    }

    private fun toCategory(record: Record): BudgetForecastCategory? =
        record[Tables.BUDGET_FORECAST_ITEMS.CATEGORY_ID]?.let { categoryId ->
            BudgetForecastCategory(
                id = categoryId,
                type = CategoryType.valueOf(requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.CATEGORY_TYPE])),
                name = requireNotNull(record[Tables.CATEGORIES.NAME]),
                icon = requireNotNull(record[Tables.CATEGORIES.ICON]),
                color = requireNotNull(record[Tables.CATEGORIES.COLOR]),
            )
        }

    private fun toRecurringSource(
        record: Record,
        sourceType: BudgetForecastSourceType,
    ): BudgetForecastRecurringSource? {
        if (sourceType != BudgetForecastSourceType.RECURRING) return null
        val payload = objectMapper.readTree(requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.SOURCE_PAYLOAD]).data())
        return BudgetForecastRecurringSource(
            templateId = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.RECURRING_TRANSACTION_TEMPLATE_ID]),
            frequency = BudgetForecastFrequency.valueOf(payload.path("frequency").asText()),
        )
    }

    private fun toHistorySource(record: Record, sourceType: BudgetForecastSourceType): BudgetForecastHistorySource? =
        if (sourceType == BudgetForecastSourceType.HISTORICAL_CATEGORY) {
            BudgetForecastHistorySource(
                monthsUsed = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.HISTORY_MONTHS_USED]).toInt(),
                method = requireNotNull(record[Tables.BUDGET_FORECAST_ITEMS.HISTORY_METHOD]),
            )
        } else {
            null
        }

    private fun toSummary(record: Record, items: List<BudgetForecastItem>): BudgetForecastSummary =
        BudgetForecastSummary(
            forecastIncome = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.FORECAST_INCOME]),
            recurringIncome = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.RECURRING_INCOME]),
            recurringExpenses = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.RECURRING_EXPENSES]),
            flexibleEstimate = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.FLEXIBLE_ESTIMATE]),
            forecastExpenses = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.FORECAST_EXPENSES]),
            forecastSavings = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.FORECAST_SAVINGS]),
            includedItemCount = items.count(BudgetForecastItem::included),
            excludedItemCount = items.count { !it.included },
        )
}

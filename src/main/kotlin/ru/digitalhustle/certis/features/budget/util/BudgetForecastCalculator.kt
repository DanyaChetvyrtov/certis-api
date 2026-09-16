package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.command.model.BudgetForecastManualAdjustmentData
import ru.digitalhustle.certis.features.budget.command.model.BudgetForecastOverrideData
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastConfidence
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastSourceType
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastBaselineAllocation
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetForecastFrequency
import ru.digitalhustle.certis.features.budget.model.BudgetForecastHistorySource
import ru.digitalhustle.certis.features.budget.model.BudgetForecastHistoryWindow
import ru.digitalhustle.certis.features.budget.model.BudgetForecastItem
import ru.digitalhustle.certis.features.budget.model.BudgetForecastPreview
import ru.digitalhustle.certis.features.budget.model.BudgetForecastRecurringSource
import ru.digitalhustle.certis.features.budget.model.BudgetForecastSummary
import ru.digitalhustle.certis.features.budget.model.BudgetForecastWarning
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.spi.ActualForecastOperation
import ru.digitalhustle.certis.features.budget.spi.BudgetForecastSourceSet
import ru.digitalhustle.certis.features.budget.spi.ForecastSourceCategory
import ru.digitalhustle.certis.features.budget.spi.ForecastSourceOperationType
import ru.digitalhustle.certis.features.budget.spi.HistoricalCategorySpending
import ru.digitalhustle.certis.features.budget.spi.ProjectedRecurringOccurrence
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID

@Component
@Suppress("LargeClass")
class BudgetForecastCalculator(
    private val fingerprintCalculator: BudgetForecastFingerprintCalculator,
) {

    fun preview(
        plan: BudgetPlan,
        sources: BudgetForecastSourceSet,
        baseline: List<BudgetForecastBaselineAllocation>,
        previous: BudgetForecast?,
        generatedAt: OffsetDateTime,
    ): BudgetForecastPreview {
        val baseItems = createBaseItems(sources, baseline)
        val items = applyPreviousDecisions(baseItems, previous)
        return BudgetForecastPreview(
            planId = plan.id,
            basedOnPlanVersion = plan.version,
            sourceFingerprint = fingerprintCalculator.sourceFingerprint(baseItems),
            generatedAt = generatedAt,
            historyWindow = historyWindow(sources),
            summary = summarize(items),
            items = items,
            warnings = warnings(items),
        )
    }

    fun confirm(
        preview: BudgetForecastPreview,
        overrides: List<BudgetForecastOverrideData>,
        manualAdjustments: List<BudgetForecastManualAdjustmentData>,
        categories: Map<UUID, BudgetForecastCategory>,
        userId: UUID,
        revision: Int,
        planVersion: Long,
        confirmedAt: OffsetDateTime,
    ): BudgetForecast {
        val overridesByKey = overrides.associateBy(BudgetForecastOverrideData::sourceKey)
        val overriddenItems = preview.items.map { item -> applyOverride(item, overridesByKey[item.sourceKey]) }
        val items = overriddenItems + manualAdjustments.map { adjustment -> manualItem(adjustment, categories) }
        return BudgetForecast(
            id = UUID.randomUUID(),
            userId = userId,
            planId = preview.planId,
            revision = revision,
            status = ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus.CURRENT,
            sourceFingerprint = preview.sourceFingerprint,
            forecastFingerprint = fingerprintCalculator.forecastFingerprint(preview.sourceFingerprint, items),
            historyWindow = preview.historyWindow,
            summary = summarize(items),
            items = items,
            planVersion = planVersion,
            confirmedAt = confirmedAt,
        )
    }

    private fun createBaseItems(
        sources: BudgetForecastSourceSet,
        baseline: List<BudgetForecastBaselineAllocation>,
    ): List<BudgetForecastItem> {
        val actualRecurringKeys = sources.actualOperations.mapNotNull { actual ->
            actual.recurringTemplateId?.let { templateId -> templateId to actual.occurredOn }
        }.toSet()
        val recurring = sources.recurringOccurrences
            .filterNot { occurrence -> (occurrence.templateId to occurrence.scheduledFor) in actualRecurringKeys }
            .map { occurrence -> recurringItem(occurrence, baseline) }
        val actual = sources.actualOperations.map(::actualItem)
        val estimates = estimateItems(sources, baseline, recurring, actual)
        return (recurring + actual + estimates).sortedBy(BudgetForecastItem::sourceKey)
    }

    private fun estimateItems(
        sources: BudgetForecastSourceSet,
        baseline: List<BudgetForecastBaselineAllocation>,
        recurring: List<BudgetForecastItem>,
        actual: List<BudgetForecastItem>,
    ): List<BudgetForecastItem> {
        val recurringIncomeExists = recurring.any { it.operationType == BudgetForecastOperationType.INCOME }
        val historical = sources.historicalCategories
            .filterNot { source -> recurringIncomeExists && source.operationType == ForecastSourceOperationType.INCOME }
            .mapNotNull { source -> historicalItem(source, sources, recurring, actual, baseline) }
        val historyCategoryIds = historical.mapNotNull { it.category?.id }.toSet()
        val currentLimits = baseline
            .filterNot { allocation -> allocation.category.id in historyCategoryIds }
            .mapNotNull { allocation -> currentLimitItem(allocation, recurring, actual) }
        return historical + currentLimits
    }

    private fun recurringItem(
        source: ProjectedRecurringOccurrence,
        baseline: List<BudgetForecastBaselineAllocation>,
    ): BudgetForecastItem {
        val operationType = source.operationType.toDomain()
        val category = source.category?.toDomain(operationType)
        return baseItem(
            sourceKey = "RECURRING:${source.templateId}:${source.scheduledFor}",
            sourceType = BudgetForecastSourceType.RECURRING,
            operationType = operationType,
            title = source.title,
            category = category,
            expectedDate = source.scheduledFor,
            amount = source.amount,
            role = role(operationType, category?.id, baseline),
            confidence = BudgetForecastConfidence.HIGH,
            recurring = BudgetForecastRecurringSource(
                templateId = source.templateId,
                frequency = BudgetForecastFrequency.valueOf(source.frequency.name),
            ),
            sourceUpdatedAt = source.sourceUpdatedAt,
        )
    }

    private fun actualItem(source: ActualForecastOperation): BudgetForecastItem {
        val operationType = source.operationType.toDomain()
        return baseItem(
            sourceKey = "ACTUAL:${source.transactionId}",
            sourceType = BudgetForecastSourceType.ACTUAL,
            operationType = operationType,
            title = source.title,
            category = source.category?.toDomain(operationType),
            expectedDate = source.occurredOn,
            amount = source.amount,
            role = if (operationType == BudgetForecastOperationType.EXPENSE) BudgetConstraintRole.REQUIRED else null,
            confidence = BudgetForecastConfidence.HIGH,
            transactionId = source.transactionId,
        )
    }

    private fun historicalItem(
        source: HistoricalCategorySpending,
        sources: BudgetForecastSourceSet,
        recurring: List<BudgetForecastItem>,
        actual: List<BudgetForecastItem>,
        baseline: List<BudgetForecastBaselineAllocation>,
    ): BudgetForecastItem? {
        val operationType = source.operationType.toDomain()
        val estimate = estimate(
            source.monthlyAmounts.entries
                .sortedBy { entry -> entry.key }
                .map { entry -> entry.value },
        )
        val committed = committedAmount(source.category.id, operationType, recurring, actual)
        val remaining = estimate.amount.subtract(committed).coerceAtLeast(ZERO)
        if (remaining.signum() == 0) return null
        val category = source.category.toDomain(operationType)
        return baseItem(
            sourceKey = "HISTORICAL_CATEGORY:${source.category.id}",
            sourceType = BudgetForecastSourceType.HISTORICAL_CATEGORY,
            operationType = operationType,
            title = source.category.name,
            category = category,
            amount = remaining,
            role = role(operationType, category.id, baseline),
            confidence = estimate.confidence,
            history = BudgetForecastHistorySource(estimate.monthsUsed, estimate.method),
            sourcePayload = mapOf(
                "monthlyAmounts" to source.monthlyAmounts.toSortedMap()
                    .mapKeys { (month, _) -> month.toString() },
            ),
        )
    }

    private fun currentLimitItem(
        allocation: BudgetForecastBaselineAllocation,
        recurring: List<BudgetForecastItem>,
        actual: List<BudgetForecastItem>,
    ): BudgetForecastItem? {
        val committed = committedAmount(
            allocation.category.id,
            BudgetForecastOperationType.EXPENSE,
            recurring,
            actual,
        )
        val amount = allocation.limitAmount.subtract(committed).coerceAtLeast(ZERO)
        if (amount.signum() == 0) return null
        return baseItem(
            sourceKey = "CURRENT_LIMIT:${allocation.category.id}",
            sourceType = BudgetForecastSourceType.CURRENT_LIMIT,
            operationType = BudgetForecastOperationType.EXPENSE,
            title = allocation.category.name,
            category = allocation.category,
            amount = amount,
            role = if (allocation.fixed) BudgetConstraintRole.REQUIRED else BudgetConstraintRole.FLEXIBLE,
            confidence = BudgetForecastConfidence.LOW,
        )
    }

    private fun committedAmount(
        categoryId: UUID,
        operationType: BudgetForecastOperationType,
        recurring: List<BudgetForecastItem>,
        actual: List<BudgetForecastItem>,
    ): BigDecimal =
        (recurring + actual)
            .filter { item -> item.category?.id == categoryId && item.operationType == operationType }
            .fold(ZERO) { total, item -> total + item.effectiveAmount }

    private fun estimate(amounts: List<BigDecimal>): HistoricalEstimate {
        val monthsUsed = amounts.size
        if (monthsUsed <= SIMPLE_AVERAGE_MAX_MONTHS) {
            return HistoricalEstimate(
                amount = amounts.fold(ZERO, BigDecimal::add).divide(monthsUsed.toBigDecimal(), SCALE, ROUNDING),
                monthsUsed = monthsUsed,
                method = SIMPLE_AVERAGE,
                confidence = BudgetForecastConfidence.LOW,
            )
        }
        val weightedTotal = amounts.withIndex().fold(ZERO) { total, (index, amount) ->
            total + amount.multiply((index + 1).toBigDecimal())
        }
        val weightTotal = (1..monthsUsed).sum().toBigDecimal()
        return HistoricalEstimate(
            amount = weightedTotal.divide(weightTotal, SCALE, ROUNDING),
            monthsUsed = monthsUsed,
            method = WEIGHTED_AVERAGE,
            confidence = if (monthsUsed >= HIGH_CONFIDENCE_MONTHS) {
                BudgetForecastConfidence.HIGH
            } else {
                BudgetForecastConfidence.MEDIUM
            },
        )
    }

    private fun applyPreviousDecisions(
        baseItems: List<BudgetForecastItem>,
        previous: BudgetForecast?,
    ): List<BudgetForecastItem> {
        if (previous == null) return baseItems
        val previousByKey = previous.items.associateBy(BudgetForecastItem::sourceKey)
        val sourced = baseItems.map { current ->
            previousByKey[current.sourceKey]?.let { old ->
                current.copy(
                    included = old.included,
                    effectiveAmount = if (old.effectiveAmount.compareTo(old.originalAmount) == 0) {
                        current.originalAmount
                    } else {
                        old.effectiveAmount
                    },
                )
            } ?: current
        }
        return sourced + previous.items.filter { it.sourceType == BudgetForecastSourceType.MANUAL }
    }

    private fun applyOverride(
        item: BudgetForecastItem,
        override: BudgetForecastOverrideData?,
    ): BudgetForecastItem =
        override?.let {
            item.copy(
                included = it.included,
                effectiveAmount = it.amount ?: item.originalAmount,
            )
        } ?: item

    private fun manualItem(
        adjustment: BudgetForecastManualAdjustmentData,
        categories: Map<UUID, BudgetForecastCategory>,
    ): BudgetForecastItem =
        baseItem(
            sourceKey = "MANUAL:${adjustment.clientId}",
            sourceType = BudgetForecastSourceType.MANUAL,
            operationType = adjustment.operationType,
            title = adjustment.title.trim(),
            category = adjustment.categoryId?.let(categories::get),
            expectedDate = adjustment.expectedDate,
            amount = adjustment.amount,
            role = if (adjustment.operationType == BudgetForecastOperationType.EXPENSE) {
                BudgetConstraintRole.FLEXIBLE
            } else {
                null
            },
            confidence = BudgetForecastConfidence.HIGH,
            manualClientId = adjustment.clientId,
        )

    @Suppress("LongParameterList")
    private fun baseItem(
        sourceKey: String,
        sourceType: BudgetForecastSourceType,
        operationType: BudgetForecastOperationType,
        title: String,
        category: BudgetForecastCategory?,
        amount: BigDecimal,
        role: BudgetConstraintRole?,
        confidence: BudgetForecastConfidence,
        expectedDate: java.time.LocalDate? = null,
        recurring: BudgetForecastRecurringSource? = null,
        transactionId: UUID? = null,
        manualClientId: UUID? = null,
        sourceUpdatedAt: OffsetDateTime? = null,
        history: BudgetForecastHistorySource? = null,
        sourcePayload: Map<String, Any?> = emptyMap(),
    ): BudgetForecastItem =
        BudgetForecastItem(
            id = UUID.randomUUID(),
            sourceKey = sourceKey,
            sourceType = sourceType,
            operationType = operationType,
            title = title,
            category = category,
            expectedDate = expectedDate,
            originalAmount = amount,
            effectiveAmount = amount,
            included = true,
            defaultConstraintRole = role,
            confidence = confidence,
            recurring = recurring,
            transactionId = transactionId,
            manualClientId = manualClientId,
            sourceUpdatedAt = sourceUpdatedAt,
            history = history,
            sourcePayload = sourcePayload,
        )

    private fun summarize(items: List<BudgetForecastItem>): BudgetForecastSummary {
        val included = items.filter(BudgetForecastItem::included)
        val income = included.amount(BudgetForecastOperationType.INCOME)
        val expenses = included.amount(BudgetForecastOperationType.EXPENSE)
        val recurringIncome = included.amount(BudgetForecastOperationType.INCOME, BudgetForecastSourceType.RECURRING)
        val recurringExpenses = included.amount(BudgetForecastOperationType.EXPENSE, BudgetForecastSourceType.RECURRING)
        return BudgetForecastSummary(
            forecastIncome = income,
            recurringIncome = recurringIncome,
            recurringExpenses = recurringExpenses,
            flexibleEstimate = expenses - recurringExpenses,
            forecastExpenses = expenses,
            forecastSavings = income - expenses,
            includedItemCount = included.size,
            excludedItemCount = items.size - included.size,
        )
    }

    private fun List<BudgetForecastItem>.amount(
        operationType: BudgetForecastOperationType,
        sourceType: BudgetForecastSourceType? = null,
    ): BigDecimal =
        filter { item -> item.operationType == operationType && (sourceType == null || item.sourceType == sourceType) }
            .fold(ZERO) { total, item -> total + item.effectiveAmount }

    private fun warnings(items: List<BudgetForecastItem>): List<BudgetForecastWarning> =
        items.filter { it.category == null && it.operationType == BudgetForecastOperationType.EXPENSE }
            .map { item -> BudgetForecastWarning("UNCATEGORIZED_EXPENSE", mapOf("sourceKey" to item.sourceKey)) }

    private fun historyWindow(sources: BudgetForecastSourceSet): BudgetForecastHistoryWindow =
        BudgetForecastHistoryWindow(
            fromMonth = sources.historyFromMonth,
            toMonth = sources.historyToMonth,
            monthsUsed = sources.historyMonthsUsed,
            method = when {
                sources.historyMonthsUsed == 0 -> NO_HISTORY
                sources.historyMonthsUsed > SIMPLE_AVERAGE_MAX_MONTHS -> WEIGHTED_AVERAGE
                else -> SIMPLE_AVERAGE
            },
        )

    private fun role(
        operationType: BudgetForecastOperationType,
        categoryId: UUID?,
        baseline: List<BudgetForecastBaselineAllocation>,
    ): BudgetConstraintRole? {
        if (operationType == BudgetForecastOperationType.INCOME) return null
        return if (baseline.any { it.category.id == categoryId && it.fixed }) {
            BudgetConstraintRole.REQUIRED
        } else {
            BudgetConstraintRole.FLEXIBLE
        }
    }

    private fun ForecastSourceOperationType.toDomain(): BudgetForecastOperationType =
        BudgetForecastOperationType.valueOf(name)

    private fun ForecastSourceCategory.toDomain(operationType: BudgetForecastOperationType): BudgetForecastCategory =
        BudgetForecastCategory(
            id = id,
            type = CategoryType.valueOf(operationType.name),
            name = name,
            icon = icon,
            color = color,
        )

    private data class HistoricalEstimate(
        val amount: BigDecimal,
        val monthsUsed: Int,
        val method: String,
        val confidence: BudgetForecastConfidence,
    )

    companion object {
        private val ZERO = BigDecimal.ZERO
        private const val SCALE = 4
        private val ROUNDING = RoundingMode.HALF_UP
        private const val SIMPLE_AVERAGE_MAX_MONTHS = 2
        private const val HIGH_CONFIDENCE_MONTHS = 6
        private const val NO_HISTORY = "NONE"
        private const val SIMPLE_AVERAGE = "SIMPLE_AVERAGE"
        private const val WEIGHTED_AVERAGE = "WEIGHTED_AVERAGE"
    }
}

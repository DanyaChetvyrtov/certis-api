package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetConstraintsData
import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetFeasibilityStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryFundingLevel
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintBaseline
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetForecastItem
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanFeasibility
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID

@Component
class BudgetConstraintCalculator(
    private val fingerprintCalculator: BudgetConstraintFingerprintCalculator,
) {

    fun suggest(
        plan: BudgetPlan,
        forecast: BudgetForecast,
        baseline: BudgetConstraintBaseline,
    ): BudgetConstraintSet {
        val categories = expenseItems(forecast)
            .groupBy { item -> requireNotNull(item.category).id }
            .values
            .map { items -> suggestedCategory(items, baseline) }
            .sortedBy { category -> category.category.name }
        return BudgetConstraintSet(
            id = null,
            userId = plan.userId,
            planId = plan.id,
            forecastRevisionId = forecast.id,
            basedOnForecastRevision = forecast.revision,
            revision = null,
            status = BudgetConstraintStatus.SUGGESTED,
            savingsFloorAmount = baseline.savingsFloorAmount,
            categories = categories,
            feasibility = feasibility(forecast.summary.forecastIncome, baseline.savingsFloorAmount, categories),
            constraintFingerprint = null,
            planVersion = plan.version,
            createdAt = null,
        )
    }

    fun confirm(
        suggestion: BudgetConstraintSet,
        forecast: BudgetForecast,
        data: SaveBudgetConstraintsData,
        categorySnapshots: Map<UUID, CategorySnapshot>,
        revision: Int,
        planVersion: Long,
        createdAt: OffsetDateTime,
    ): BudgetConstraintSet {
        val suggestions = suggestion.categories.associateBy { category -> category.category.id }
        val forecastAmounts = expenseItems(forecast)
            .groupBy { item -> requireNotNull(item.category).id }
            .mapValues { (_, items) -> items.sum() }
        val categories = data.categories.map { input ->
            val source = suggestions[input.categoryId]
            val snapshot = requireNotNull(categorySnapshots[input.categoryId])
            val coverageBase = forecastAmounts[input.categoryId]
                ?: input.fundingLevels.maxOfOrNull { level -> level.amount }
                ?: input.requiredAmount
            BudgetCategoryConstraint(
                id = UUID.randomUUID(),
                category = source?.category ?: snapshot.toForecastCategory(),
                allocationType = input.allocationType,
                constraintRole = input.constraintRole,
                requiredAmount = input.requiredAmount,
                priority = input.priority,
                currentLimitAmount = source?.currentLimitAmount ?: ZERO,
                fundingLevels = input.fundingLevels.map { level ->
                    BudgetCategoryFundingLevel(
                        id = UUID.randomUUID(),
                        level = level.level,
                        amount = level.amount,
                        coverage = coverage(level.amount, coverageBase),
                    )
                },
                sourceKeys = source?.sourceKeys ?: emptyList(),
            )
        }.sortedBy { category -> category.category.name }
        val fingerprint = fingerprintCalculator.calculate(
            forecastFingerprint = forecast.forecastFingerprint,
            savingsFloorAmount = data.savingsFloorAmount,
            categories = categories,
        )
        return BudgetConstraintSet(
            id = UUID.randomUUID(),
            userId = data.userId,
            planId = data.planId,
            forecastRevisionId = forecast.id,
            basedOnForecastRevision = forecast.revision,
            revision = revision,
            status = BudgetConstraintStatus.CONFIRMED,
            savingsFloorAmount = data.savingsFloorAmount,
            categories = categories,
            feasibility = feasibility(forecast.summary.forecastIncome, data.savingsFloorAmount, categories),
            constraintFingerprint = fingerprint,
            planVersion = planVersion,
            createdAt = createdAt,
        )
    }

    private fun suggestedCategory(
        items: List<BudgetForecastItem>,
        baseline: BudgetConstraintBaseline,
    ): BudgetCategoryConstraint {
        val category = requireNotNull(items.first().category)
        val requiredAmount = items.amount(BudgetConstraintRole.REQUIRED)
        val flexibleAmount = items.sum() - requiredAmount
        val currentLimit = baseline.allocations.firstOrNull { allocation -> allocation.category.id == category.id }
            ?.limitAmount ?: items.sum()
        val allocationType = if (flexibleAmount.signum() == 0 && requiredAmount.signum() > 0) {
            BudgetAllocationType.FIXED
        } else {
            BudgetAllocationType.VARIABLE
        }
        return BudgetCategoryConstraint(
            id = UUID.randomUUID(),
            category = category,
            allocationType = allocationType,
            constraintRole = if (requiredAmount.signum() > 0) {
                BudgetConstraintRole.REQUIRED
            } else {
                BudgetConstraintRole.FLEXIBLE
            },
            requiredAmount = requiredAmount,
            priority = if (allocationType == BudgetAllocationType.VARIABLE) BudgetPriority.MEDIUM else null,
            currentLimitAmount = currentLimit,
            fundingLevels = if (allocationType == BudgetAllocationType.VARIABLE) {
                suggestedFundingLevels(requiredAmount, flexibleAmount, currentLimit)
            } else {
                emptyList()
            },
            sourceKeys = items.map(BudgetForecastItem::sourceKey).sorted(),
        )
    }

    private fun suggestedFundingLevels(
        requiredAmount: BigDecimal,
        flexibleAmount: BigDecimal,
        currentLimit: BigDecimal,
    ): List<BudgetCategoryFundingLevel> {
        val forecastAmount = requiredAmount + flexibleAmount
        val candidates = listOf(
            BudgetFundingLevel.MINIMUM to requiredAmount + flexibleAmount.multiply(MINIMUM_COVERAGE),
            BudgetFundingLevel.BALANCED to requiredAmount + flexibleAmount.multiply(BALANCED_COVERAGE),
            BudgetFundingLevel.COMFORTABLE to forecastAmount.max(currentLimit),
        )
        return candidates.fold(emptyList()) { levels, (level, amount) ->
            val normalized = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            if (levels.any { existing -> existing.amount.compareTo(normalized) == 0 }) {
                levels
            } else {
                levels + BudgetCategoryFundingLevel(
                    id = UUID.randomUUID(),
                    level = level,
                    amount = normalized,
                    coverage = coverage(normalized, forecastAmount),
                )
            }
        }
    }

    // Coverage describes the fraction of the confirmed category forecast funded by this option,
    // not the fixed percentage used to generate its initial suggested amount. A user-edited
    // MINIMUM can therefore cover less (or more) than the default 60%.
    private fun coverage(amount: BigDecimal, forecastAmount: BigDecimal): BigDecimal =
        if (forecastAmount.signum() == 0) {
            ZERO.setScale(COVERAGE_SCALE)
        } else {
            amount.divide(forecastAmount, COVERAGE_SCALE, RoundingMode.HALF_UP)
                .coerceIn(ZERO, ONE)
        }

    private fun feasibility(
        forecastIncome: BigDecimal,
        savingsFloorAmount: BigDecimal,
        categories: List<BudgetCategoryConstraint>,
    ): BudgetPlanFeasibility {
        val requiredAmount = categories.fold(ZERO) { total, category -> total + category.requiredAmount }
        val variableMinimum = categories
            .filter { category -> category.allocationType == BudgetAllocationType.VARIABLE }
            .fold(ZERO) { total, category ->
                val minimum = requireNotNull(category.fundingLevels.minByOrNull { level -> level.amount })
                total + (minimum.amount - category.requiredAmount)
            }
        val maximumSavings = forecastIncome - requiredAmount - variableMinimum
        val shortfall = (savingsFloorAmount - maximumSavings).coerceAtLeast(ZERO)
        val status = if (shortfall.signum() == 0) {
            BudgetFeasibilityStatus.FEASIBLE
        } else {
            BudgetFeasibilityStatus.INFEASIBLE
        }
        val violations = if (status == BudgetFeasibilityStatus.INFEASIBLE) {
            listOf(
                BudgetPlanningViolation(
                    code = MINIMUM_ALLOCATION_EXCEEDS_CAPACITY,
                    shortfall = shortfall,
                    requiredAmount = variableMinimum,
                    availableAmount = forecastIncome - requiredAmount - savingsFloorAmount,
                ),
            )
        } else {
            emptyList()
        }
        return BudgetPlanFeasibility(
            status = status,
            forecastIncome = forecastIncome,
            requiredAmount = requiredAmount,
            variableMinimumAmount = variableMinimum,
            savingsFloorAmount = savingsFloorAmount,
            maximumSavingsAmount = maximumSavings,
            capacityAtSavingsFloor = forecastIncome - requiredAmount - savingsFloorAmount,
            shortfall = shortfall,
            violations = violations,
        )
    }

    private fun expenseItems(forecast: BudgetForecast): List<BudgetForecastItem> =
        forecast.items.filter { item ->
            item.included && item.operationType == BudgetForecastOperationType.EXPENSE
        }

    private fun List<BudgetForecastItem>.sum(): BigDecimal =
        fold(ZERO) { total, item -> total + item.effectiveAmount }

    private fun List<BudgetForecastItem>.amount(role: BudgetConstraintRole): BigDecimal =
        filter { item -> item.defaultConstraintRole == role }.sum()

    private fun CategorySnapshot.toForecastCategory(): BudgetForecastCategory =
        BudgetForecastCategory(
            id = id,
            type = type,
            name = name,
            icon = icon,
            color = color,
        )

    companion object {
        private const val MONEY_SCALE = 4
        private const val COVERAGE_SCALE = 6
        private const val MINIMUM_ALLOCATION_EXCEEDS_CAPACITY = "MINIMUM_ALLOCATION_EXCEEDS_CAPACITY"
        private val ZERO = BigDecimal.ZERO
        private val ONE = BigDecimal.ONE
        private val MINIMUM_COVERAGE = BigDecimal("0.600000")
        private val BALANCED_COVERAGE = BigDecimal("0.850000")
    }
}

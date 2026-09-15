package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.digitalhustle.certis.features.budget.command.model.BudgetCategoryConstraintData
import ru.digitalhustle.certis.features.budget.command.model.BudgetFundingLevelData
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetConstraintsData
import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastConfidence
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastSourceType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintBaseline
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastBaselineAllocation
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetForecastHistoryWindow
import ru.digitalhustle.certis.features.budget.model.BudgetForecastItem
import ru.digitalhustle.certis.features.budget.model.BudgetForecastSummary
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.util.BudgetConstraintCalculator
import ru.digitalhustle.certis.features.budget.util.BudgetConstraintFingerprintCalculator
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

class BudgetConstraintCalculatorTest {

    private val calculator = BudgetConstraintCalculator(BudgetConstraintFingerprintCalculator())
    private val userId = UUID.randomUUID()
    private val planId = UUID.randomUUID()
    private val category = BudgetForecastCategory(
        UUID.randomUUID(),
        CategoryType.EXPENSE,
        "Groceries",
        "wallet",
        "#10B981",
    )
    private val now = OffsetDateTime.parse("2026-09-16T12:00:00+03:00")

    @Test
    fun `should calculate actual coverage for user-edited funding amounts`() {
        val forecast = forecast("32000.00")
        val suggestion = calculator.suggest(plan(), forecast, BudgetConstraintBaseline(BigDecimal.ZERO, emptyList()))
        val suggested = suggestion.categories.single().fundingLevels.associateBy { it.level }
        assertThat(suggested.getValue(BudgetFundingLevel.MINIMUM).amount).isEqualByComparingTo("19200.00")
        assertThat(suggested.getValue(BudgetFundingLevel.MINIMUM).coverage).isEqualByComparingTo("0.600000")
        assertThat(suggested.getValue(BudgetFundingLevel.BALANCED).amount).isEqualByComparingTo("27200.00")
        assertThat(suggested.getValue(BudgetFundingLevel.BALANCED).coverage).isEqualByComparingTo("0.850000")

        val confirmed = calculator.confirm(
            suggestion = suggestion,
            forecast = forecast,
            data = request(
                listOf(
                    BudgetFundingLevelData(BudgetFundingLevel.MINIMUM, BigDecimal("24000.00")),
                    BudgetFundingLevelData(BudgetFundingLevel.BALANCED, BigDecimal("28500.00")),
                    BudgetFundingLevelData(BudgetFundingLevel.COMFORTABLE, BigDecimal("32000.00")),
                ),
            ),
            revision = 1,
            planVersion = 2,
            createdAt = now,
        )
        val levels = confirmed.categories.single().fundingLevels.associateBy { it.level }
        assertThat(levels.getValue(BudgetFundingLevel.MINIMUM).coverage).isEqualByComparingTo("0.750000")
        assertThat(levels.getValue(BudgetFundingLevel.BALANCED).coverage).isEqualByComparingTo("0.890625")
        assertThat(levels.getValue(BudgetFundingLevel.COMFORTABLE).coverage).isEqualByComparingTo("1.000000")
        assertThat(confirmed.feasibility.variableMinimumAmount).isEqualByComparingTo("24000.00")
    }

    @Test
    fun `should preserve user minimum below suggested sixty percent`() {
        val forecast = forecast("32000.00")
        val suggestion = calculator.suggest(plan(), forecast, BudgetConstraintBaseline(BigDecimal.ZERO, emptyList()))
        val confirmed = calculator.confirm(
            suggestion = suggestion,
            forecast = forecast,
            data = request(
                listOf(BudgetFundingLevelData(BudgetFundingLevel.MINIMUM, BigDecimal("12000.00"))),
            ),
            revision = 1,
            planVersion = 2,
            createdAt = now,
        )
        val minimum = confirmed.categories.single().fundingLevels.single()
        assertThat(minimum.coverage).isEqualByComparingTo("0.375000")
        assertThat(confirmed.feasibility.variableMinimumAmount).isEqualByComparingTo("12000.00")
    }

    @Test
    fun `should cap coverage at fully funded when baseline exceeds forecast`() {
        val forecast = forecast("32000.00")
        val suggestion = calculator.suggest(
            plan(),
            forecast,
            BudgetConstraintBaseline(
                BigDecimal.ZERO,
                listOf(
                    BudgetForecastBaselineAllocation(
                        category,
                        BigDecimal("40000.00"),
                        false,
                    ),
                ),
            ),
        )
        val comfortable = suggestion.categories.single().fundingLevels.last()
        assertThat(comfortable.amount).isEqualByComparingTo("40000.00")
        assertThat(comfortable.coverage).isEqualByComparingTo("1.000000")
    }

    private fun request(levels: List<BudgetFundingLevelData>): SaveBudgetConstraintsData =
        SaveBudgetConstraintsData(
            planId = planId,
            userId = userId,
            expectedVersion = 1,
            forecastRevision = 1,
            savingsFloorAmount = BigDecimal.ZERO,
            categories = listOf(
                BudgetCategoryConstraintData(
                    categoryId = category.id,
                    allocationType = BudgetAllocationType.VARIABLE,
                    constraintRole = BudgetConstraintRole.FLEXIBLE,
                    requiredAmount = BigDecimal.ZERO,
                    priority = BudgetPriority.MEDIUM,
                    fundingLevels = levels,
                ),
            ),
        )

    private fun forecast(amount: String): BudgetForecast =
        BudgetForecast(
            id = UUID.randomUUID(),
            userId = userId,
            planId = planId,
            revision = 1,
            status = BudgetForecastStatus.CURRENT,
            sourceFingerprint = "sha256:source",
            forecastFingerprint = "sha256:forecast",
            historyWindow = BudgetForecastHistoryWindow(
                YearMonth.parse("2026-08"),
                YearMonth.parse("2026-08"),
                1,
                "HISTORICAL",
            ),
            summary = BudgetForecastSummary(
                forecastIncome = BigDecimal("80000.00"),
                recurringIncome = BigDecimal.ZERO,
                recurringExpenses = BigDecimal.ZERO,
                flexibleEstimate = BigDecimal(amount),
                forecastExpenses = BigDecimal(amount),
                forecastSavings = BigDecimal("80000.00") - BigDecimal(amount),
                includedItemCount = 1,
                excludedItemCount = 0,
            ),
            items = listOf(
                BudgetForecastItem(
                    id = UUID.randomUUID(),
                    sourceKey = "HISTORICAL_CATEGORY:${category.id}",
                    sourceType = BudgetForecastSourceType.HISTORICAL_CATEGORY,
                    operationType = BudgetForecastOperationType.EXPENSE,
                    title = "Groceries",
                    category = category,
                    expectedDate = null,
                    originalAmount = BigDecimal(amount),
                    effectiveAmount = BigDecimal(amount),
                    included = true,
                    defaultConstraintRole = BudgetConstraintRole.FLEXIBLE,
                    confidence = BudgetForecastConfidence.MEDIUM,
                    recurring = null,
                    transactionId = null,
                    manualClientId = null,
                    sourceUpdatedAt = null,
                    history = null,
                ),
            ),
            planVersion = 1,
            confirmedAt = now,
        )

    private fun plan(): BudgetPlan =
        BudgetPlan(
            id = planId,
            userId = userId,
            previousPlanId = null,
            baselineBudgetId = null,
            appliedBudgetId = null,
            budgetMonth = LocalDate.parse("2026-10-01"),
            currency = Currency.RUB,
            revision = 1,
            version = 1,
            status = BudgetPlanStatus.DRAFT,
            idempotencyKey = "test-plan",
            createdAt = now,
            updatedAt = now,
            appliedAt = null,
            supersededAt = null,
            cancelledAt = null,
        )
}

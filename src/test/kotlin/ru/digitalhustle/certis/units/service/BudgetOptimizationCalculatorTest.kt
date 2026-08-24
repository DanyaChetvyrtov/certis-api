package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.BudgetOptimizationReason
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.budget.BudgetAllocationDetails
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.service.budget.BudgetOptimizationCalculator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BudgetOptimizationCalculatorTest {

    private val calculator = BudgetOptimizationCalculator()

    @Test
    fun `should preserve fixed costs reallocate released budget and increase savings`() {
        // given
        val budget = createBudget(
            listOf(
                allocation("Rent", BudgetExpenseType.FIXED, "200.0000", "210.0000"),
                allocation("Groceries", BudgetExpenseType.VARIABLE, "400.0000", "100.0000"),
                allocation("Transport", BudgetExpenseType.VARIABLE, "100.0000", "120.0000"),
                allocation("Dining", BudgetExpenseType.VARIABLE, "100.0000", "98.0000"),
            ),
        )

        // when
        val result = calculator.calculate(budget)

        // then
        assertThat(result.savingsBefore).isEqualByComparingTo("200.0000")
        assertThat(result.savingsAfter).isEqualByComparingTo("247.2000")
        assertThat(result.resultSnapshot.allocations).extracting("categoryName", "recommendedLimit", "reason")
            .containsExactly(
                tuple("Rent", BigDecimal("200.0000"), BudgetOptimizationReason.FIXED_PRESERVED),
                tuple("Groceries", BigDecimal("325.0000"), BudgetOptimizationReason.LOW_UTILIZATION_REDUCTION),
                tuple("Transport", BigDecimal("120.0000"), BudgetOptimizationReason.OVERSPENDING_REALLOCATION),
                tuple("Dining", BigDecimal("107.8000"), BudgetOptimizationReason.NEAR_LIMIT_REALLOCATION),
            )
    }

    @Test
    fun `should leave risk unchanged when no amount can be released`() {
        // given
        val budget = createBudget(
            listOf(
                allocation("Transport", BudgetExpenseType.VARIABLE, "100.0000", "120.0000"),
            ),
        )

        // when
        val result = calculator.calculate(budget)

        // then
        val recommendation = result.resultSnapshot.allocations.single()
        assertThat(recommendation.recommendedLimit).isEqualByComparingTo("100.0000")
        assertThat(recommendation.reason).isEqualTo(BudgetOptimizationReason.RISK_UNCHANGED)
        assertThat(result.savingsAfter).isEqualByComparingTo(result.savingsBefore)
    }

    private fun createBudget(allocations: List<BudgetAllocationDetails>): BudgetDetails =
        BudgetDetails(
            id = UUID.randomUUID(),
            budgetMonth = LocalDate.parse("2026-08-01"),
            plannedIncome = BigDecimal("1000.0000"),
            savingsTarget = BigDecimal("200.0000"),
            currency = Currency.RUB,
            allocations = allocations,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
        )

    private fun allocation(
        name: String,
        type: BudgetExpenseType,
        limit: String,
        spent: String,
    ): BudgetAllocationDetails =
        BudgetAllocationDetails.create(
            id = UUID.randomUUID(),
            categoryId = UUID.randomUUID(),
            categoryName = name,
            categoryIcon = "wallet",
            categoryColor = "#10B981",
            expenseType = type,
            limitAmount = BigDecimal(limit),
            spentAmount = BigDecimal(spent),
        )
}

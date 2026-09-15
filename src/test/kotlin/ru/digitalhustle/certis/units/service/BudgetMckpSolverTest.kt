package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryFundingLevel
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.util.BudgetMckpSolver
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.math.BigDecimal
import java.util.UUID

class BudgetMckpSolverTest {

    private val solver = BudgetMckpSolver()

    @Test
    fun `should find an exact combination instead of taking the largest single upgrade`() {
        val expensive = category(1, BudgetPriority.HIGH, "10.00")
        val firstMedium = category(2, BudgetPriority.MEDIUM, "5.00")
        val secondMedium = category(3, BudgetPriority.MEDIUM, "5.00")

        val solution = solver.solve(
            categories = listOf(expensive, firstMedium, secondMedium),
            capacity = BigDecimal("10.00"),
        )

        assertThat(solution.cost).isEqualByComparingTo("10.00")
        assertThat(solution.objectiveValue).isEqualByComparingTo("5.800000")
        assertThat(solution.selections.getValue(expensive.category.id).level)
            .isEqualTo(BudgetFundingLevel.MINIMUM)
        assertThat(solution.selections.getValue(firstMedium.category.id).level)
            .isEqualTo(BudgetFundingLevel.COMFORTABLE)
        assertThat(solution.selections.getValue(secondMedium.category.id).level)
            .isEqualTo(BudgetFundingLevel.COMFORTABLE)
    }

    @Test
    fun `should use stable category order when objective and savings are equal`() {
        val first = category(1, BudgetPriority.MEDIUM, "5.00")
        val second = category(2, BudgetPriority.MEDIUM, "5.00")

        val solution = solver.solve(listOf(second, first), BigDecimal("5.00"))

        assertThat(solution.selections.getValue(first.category.id).level)
            .isEqualTo(BudgetFundingLevel.COMFORTABLE)
        assertThat(solution.selections.getValue(second.category.id).level)
            .isEqualTo(BudgetFundingLevel.MINIMUM)
    }

    private fun category(
        idSuffix: Int,
        priority: BudgetPriority,
        upgradeCost: String,
    ): BudgetCategoryConstraint {
        val categoryId = UUID.fromString("00000000-0000-0000-0000-${idSuffix.toString().padStart(12, '0')}")
        return BudgetCategoryConstraint(
            id = UUID.randomUUID(),
            category = BudgetForecastCategory(
                id = categoryId,
                type = CategoryType.EXPENSE,
                name = "Category $idSuffix",
                icon = "wallet",
                color = "#10B981",
            ),
            allocationType = BudgetAllocationType.VARIABLE,
            constraintRole = BudgetConstraintRole.FLEXIBLE,
            requiredAmount = BigDecimal.ZERO,
            priority = priority,
            currentLimitAmount = BigDecimal(upgradeCost),
            fundingLevels = listOf(
                level(BudgetFundingLevel.MINIMUM, "0.00", "0.600000"),
                level(BudgetFundingLevel.COMFORTABLE, upgradeCost, "1.000000"),
            ),
            sourceKeys = emptyList(),
        )
    }

    private fun level(
        level: BudgetFundingLevel,
        amount: String,
        coverage: String,
    ): BudgetCategoryFundingLevel =
        BudgetCategoryFundingLevel(
            id = UUID.randomUUID(),
            level = level,
            amount = BigDecimal(amount),
            coverage = BigDecimal(coverage),
        )
}

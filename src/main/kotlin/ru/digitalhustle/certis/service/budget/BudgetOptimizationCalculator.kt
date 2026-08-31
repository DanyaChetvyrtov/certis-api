package ru.digitalhustle.certis.service.budget

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.BudgetOptimizationReason
import ru.digitalhustle.certis.model.budget.BudgetAllocationDetails
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.BudgetOptimizationAllocation
import ru.digitalhustle.certis.model.budget.BudgetOptimizationInputAllocation
import ru.digitalhustle.certis.model.budget.BudgetOptimizationInputSnapshot
import ru.digitalhustle.certis.model.budget.BudgetOptimizationResultSnapshot
import ru.digitalhustle.certis.model.budget.CalculatedBudgetOptimization
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Component
class BudgetOptimizationCalculator {

    private companion object {
        private const val MONEY_SCALE = 4
        private val RELEASE_FACTOR = BigDecimal("0.25")
        private val RISK_BUFFER_FACTOR = BigDecimal("1.10")
    }

    fun calculate(budget: BudgetDetails): CalculatedBudgetOptimization {
        val recommendedLimits = budget.allocations.associate { allocation ->
            allocation.id to initialRecommendedLimit(allocation)
        }.toMutableMap()
        var availableForReallocation = calculateReleasedAmount(budget.allocations, recommendedLimits)

        riskAllocations(budget.allocations).forEach { allocation ->
            val requestedIncrease = requestedIncrease(allocation)
            val grantedIncrease = requestedIncrease.min(availableForReallocation)

            recommendedLimits.compute(allocation.id) { _, current ->
                requireNotNull(current) + grantedIncrease
            }
            availableForReallocation -= grantedIncrease
        }

        val recommendations = budget.allocations.map { allocation ->
            allocation.toRecommendation(requireNotNull(recommendedLimits[allocation.id]))
        }
        val allocatedBefore = budget.allocations.sumOf(BudgetAllocationDetails::limitAmount)
        val allocatedAfter = recommendations.sumOf(BudgetOptimizationAllocation::recommendedLimit)

        return CalculatedBudgetOptimization(
            inputSnapshot = budget.toInputSnapshot(),
            resultSnapshot = BudgetOptimizationResultSnapshot(recommendations),
            savingsBefore = budget.plannedIncome - allocatedBefore,
            savingsAfter = budget.plannedIncome - allocatedAfter,
        )
    }

    private fun initialRecommendedLimit(allocation: BudgetAllocationDetails): BigDecimal {
        if (
            allocation.expenseType == BudgetExpenseType.FIXED ||
            allocation.status != BudgetAllocationStatus.ON_TRACK
        ) {
            return allocation.limitAmount
        }

        val unusedAmount = (allocation.limitAmount - allocation.spentAmount).max(BigDecimal.ZERO)
        val releasedAmount = unusedAmount.multiply(RELEASE_FACTOR).money()

        return (allocation.limitAmount - releasedAmount)
            .max(allocation.spentAmount)
            .money()
    }

    private fun calculateReleasedAmount(
        allocations: List<BudgetAllocationDetails>,
        recommendedLimits: Map<UUID, BigDecimal>,
    ): BigDecimal =
        allocations.fold(BigDecimal.ZERO) { released, allocation ->
            released + allocation.limitAmount - requireNotNull(recommendedLimits[allocation.id])
        }.money()

    private fun riskAllocations(allocations: List<BudgetAllocationDetails>): List<BudgetAllocationDetails> =
        allocations
            .filter { allocation ->
                allocation.expenseType == BudgetExpenseType.VARIABLE &&
                    allocation.status != BudgetAllocationStatus.ON_TRACK
            }
            .sortedWith(
                compareBy<BudgetAllocationDetails> { allocation ->
                    when (allocation.status) {
                        BudgetAllocationStatus.OVERSPENT -> 0
                        BudgetAllocationStatus.NEAR_LIMIT -> 1
                        BudgetAllocationStatus.ON_TRACK -> 2
                    }
                }.thenBy(BudgetAllocationDetails::categoryName),
            )

    private fun requestedIncrease(allocation: BudgetAllocationDetails): BigDecimal =
        when (allocation.status) {
            BudgetAllocationStatus.OVERSPENT -> allocation.spentAmount - allocation.limitAmount
            BudgetAllocationStatus.NEAR_LIMIT ->
                allocation.spentAmount.multiply(RISK_BUFFER_FACTOR) - allocation.limitAmount
            BudgetAllocationStatus.ON_TRACK -> BigDecimal.ZERO
        }.max(BigDecimal.ZERO).money()

    private fun BudgetAllocationDetails.toRecommendation(recommendedLimit: BigDecimal): BudgetOptimizationAllocation =
        BudgetOptimizationAllocation(
            allocationId = id,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            expenseType = expenseType,
            status = status,
            currentLimit = limitAmount,
            recommendedLimit = recommendedLimit,
            spentAmount = spentAmount,
            reason = optimizationReason(recommendedLimit),
        )

    private fun BudgetAllocationDetails.optimizationReason(recommendedLimit: BigDecimal): BudgetOptimizationReason =
        when {
            expenseType == BudgetExpenseType.FIXED -> BudgetOptimizationReason.FIXED_PRESERVED
            recommendedLimit < limitAmount -> BudgetOptimizationReason.LOW_UTILIZATION_REDUCTION
            recommendedLimit > limitAmount && status == BudgetAllocationStatus.OVERSPENT ->
                BudgetOptimizationReason.OVERSPENDING_REALLOCATION
            recommendedLimit > limitAmount -> BudgetOptimizationReason.NEAR_LIMIT_REALLOCATION
            status != BudgetAllocationStatus.ON_TRACK -> BudgetOptimizationReason.RISK_UNCHANGED
            else -> BudgetOptimizationReason.NO_CHANGE
        }

    private fun BudgetDetails.toInputSnapshot(): BudgetOptimizationInputSnapshot =
        BudgetOptimizationInputSnapshot(
            budgetId = id,
            budgetMonth = budgetMonth,
            plannedIncome = plannedIncome,
            savingsTarget = savingsTarget,
            currency = currency,
            budgetUpdatedAt = updatedAt,
            allocations = allocations.map { allocation ->
                BudgetOptimizationInputAllocation(
                    allocationId = allocation.id,
                    categoryId = allocation.categoryId,
                    expenseType = allocation.expenseType,
                    limitAmount = allocation.limitAmount,
                    spentAmount = allocation.spentAmount,
                )
            },
        )

    private fun BigDecimal.money(): BigDecimal = setScale(MONEY_SCALE, RoundingMode.HALF_UP)
}

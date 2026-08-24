package ru.digitalhustle.certis.service.budget.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.model.budget.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.BudgetOptimizationDetails
import ru.digitalhustle.certis.model.budget.SaveBudgetAllocationData
import ru.digitalhustle.certis.service.budget.BudgetOptimizationAggregator
import ru.digitalhustle.certis.service.budget.BudgetOptimizationCalculator
import ru.digitalhustle.certis.service.domain.BudgetOptimizationService
import ru.digitalhustle.certis.service.domain.BudgetService
import java.time.LocalDate
import java.util.UUID

@Service
class BudgetOptimizationAggregatorImpl(
    private val budgetService: BudgetService,
    private val budgetOptimizationService: BudgetOptimizationService,
    private val budgetOptimizationCalculator: BudgetOptimizationCalculator,
) : BudgetOptimizationAggregator {

    @Transactional(readOnly = true)
    override fun getLatest(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimizationDetails = budgetOptimizationService.getLatest(userId, budgetMonth)

    @Transactional
    override fun generate(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimizationDetails {
        val budget = budgetService.getByMonthForUpdate(userId, budgetMonth)
        val calculation = budgetOptimizationCalculator.calculate(budget)

        return budgetOptimizationService.create(userId, calculation)
    }

    @Transactional
    override fun apply(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails {
        budgetService.getByMonthForUpdate(userId, budgetMonth)
        val optimization = budgetOptimizationService.getProposedForUpdate(id, userId, budgetMonth)
        val budget = budgetService.applyOptimization(
            ApplyBudgetOptimizationData(
                userId = userId,
                inputSnapshot = optimization.inputSnapshot,
                allocations = optimization.allocations.map { recommendation ->
                    SaveBudgetAllocationData(
                        categoryId = recommendation.categoryId,
                        expenseType = recommendation.expenseType,
                        limitAmount = recommendation.recommendedLimit,
                    )
                },
            ),
        )
        budgetOptimizationService.apply(id, userId)

        return budget
    }

    @Transactional
    override fun dismiss(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ) {
        budgetService.getByMonthForUpdate(userId, budgetMonth)
        budgetOptimizationService.getProposedForUpdate(id, userId, budgetMonth)
        budgetOptimizationService.dismiss(id, userId)
    }
}

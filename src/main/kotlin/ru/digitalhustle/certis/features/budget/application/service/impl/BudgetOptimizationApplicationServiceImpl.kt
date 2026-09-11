package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.application.service.BudgetApplicationService
import ru.digitalhustle.certis.features.budget.application.service.BudgetOptimizationApplicationService
import ru.digitalhustle.certis.features.budget.command.model.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetAllocationData
import ru.digitalhustle.certis.features.budget.command.service.BudgetOptimizationService
import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationDetails
import ru.digitalhustle.certis.features.budget.util.BudgetOptimizationCalculator
import java.time.LocalDate
import java.util.UUID

@Service
class BudgetOptimizationApplicationServiceImpl(
    private val budgetService: BudgetApplicationService,
    private val budgetOptimizationService: BudgetOptimizationService,
) : BudgetOptimizationApplicationService {

    @Transactional
    override fun generate(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimizationDetails {
        val budget = budgetService.getByMonthForUpdate(userId, budgetMonth)
        val calculation = BudgetOptimizationCalculator.calculate(budget)

        return budgetOptimizationService.create(userId, calculation)
    }

    @Transactional
    override fun apply(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails {
        val lockedBudget = budgetService.getByMonthForUpdate(userId, budgetMonth)
        val optimization = budgetOptimizationService.getProposedForUpdate(id, userId, lockedBudget.id)
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
        val lockedBudget = budgetService.getByMonthForUpdate(userId, budgetMonth)
        budgetOptimizationService.getProposedForUpdate(id, userId, lockedBudget.id)
        budgetOptimizationService.dismiss(id, userId)
    }
}

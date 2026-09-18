package ru.digitalhustle.certis.features.budget.query.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.budget.model.BudgetOptimization
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationDetails
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationInputSnapshot
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationResultSnapshot
import ru.digitalhustle.certis.features.budget.query.repository.BudgetOptimizationQueryRepository
import ru.digitalhustle.certis.features.budget.query.service.BudgetOptimizationQueryService
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BudgetOptimizationQueryServiceImpl(
    private val budgetOptimizationRepository: BudgetOptimizationQueryRepository,
    private val objectMapper: ObjectMapper,
) : BudgetOptimizationQueryService {

    override fun getLatest(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimizationDetails =
        budgetOptimizationRepository.findLatestByUserIdAndMonth(userId, budgetMonth)
            ?.toDetails()
            ?: throw NotFoundException.entity("Budget optimization")

    private fun BudgetOptimization.toDetails(): BudgetOptimizationDetails {
        val input = objectMapper.treeToValue(inputSnapshot, BudgetOptimizationInputSnapshot::class.java)
        val result = objectMapper.treeToValue(resultSnapshot, BudgetOptimizationResultSnapshot::class.java)

        return BudgetOptimizationDetails(
            id = id,
            budgetId = budgetId,
            budgetMonth = input.budgetMonth,
            currency = input.currency,
            algorithmVersion = algorithmVersion,
            status = status,
            savingsBefore = savingsBefore,
            savingsAfter = savingsAfter,
            allocations = result.allocations,
            createdAt = createdAt,
            appliedAt = appliedAt,
            inputSnapshot = input,
        )
    }
}

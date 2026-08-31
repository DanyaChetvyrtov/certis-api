package ru.digitalhustle.certis.service.domain.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.BudgetOptimizationStatus
import ru.digitalhustle.certis.exception.custom.BudgetOptimizationConflictException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.budget.BudgetOptimizationDetails
import ru.digitalhustle.certis.model.budget.BudgetOptimizationInputSnapshot
import ru.digitalhustle.certis.model.budget.BudgetOptimizationResultSnapshot
import ru.digitalhustle.certis.model.budget.CalculatedBudgetOptimization
import ru.digitalhustle.certis.model.entity.BudgetOptimization
import ru.digitalhustle.certis.repository.BudgetOptimizationRepository
import ru.digitalhustle.certis.service.domain.BudgetOptimizationService
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BudgetOptimizationServiceImpl(
    private val budgetOptimizationRepository: BudgetOptimizationRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) : BudgetOptimizationService {

    private companion object {
        private const val SNAPSHOT_SCHEMA_VERSION: Short = 1
        private const val ALGORITHM_VERSION = "rule-based-v1"
        private const val ENTITY_NAME = "Budget optimization"
    }

    override fun getLatest(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimizationDetails =
        budgetOptimizationRepository.findLatestByUserIdAndMonth(userId, budgetMonth)
            ?.toDetails()
            ?: throw NotFoundException.entity(ENTITY_NAME)

    override fun getProposedForUpdate(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimizationDetails {
        val optimization = budgetOptimizationRepository.findByIdAndUserIdAndMonthForUpdate(
            id = id,
            userId = userId,
            budgetMonth = budgetMonth,
        ) ?: throw NotFoundException.entity(ENTITY_NAME)

        if (optimization.status != BudgetOptimizationStatus.PROPOSED) {
            throw BudgetOptimizationConflictException(ErrorMessages.BUDGET_OPTIMIZATION_NOT_PROPOSED)
        }

        return optimization.toDetails()
    }

    override fun create(
        userId: UUID,
        calculation: CalculatedBudgetOptimization,
    ): BudgetOptimizationDetails {
        val now = OffsetDateTime.now(clock)

        budgetOptimizationRepository.dismissProposedByBudgetId(
            budgetId = calculation.inputSnapshot.budgetId,
            userId = userId,
        )

        return budgetOptimizationRepository.insert(
            BudgetOptimization(
                id = UUID.randomUUID(),
                userId = userId,
                budgetId = calculation.inputSnapshot.budgetId,
                snapshotSchemaVersion = SNAPSHOT_SCHEMA_VERSION,
                algorithmVersion = ALGORITHM_VERSION,
                status = BudgetOptimizationStatus.PROPOSED,
                inputSnapshot = objectMapper.valueToTree(calculation.inputSnapshot),
                resultSnapshot = objectMapper.valueToTree(calculation.resultSnapshot),
                savingsBefore = calculation.savingsBefore,
                savingsAfter = calculation.savingsAfter,
                createdAt = now,
                appliedAt = null,
            ),
        ).toDetails()
    }

    override fun apply(
        id: UUID,
        userId: UUID,
    ): BudgetOptimizationDetails =
        budgetOptimizationRepository.updateStatus(
            id = id,
            userId = userId,
            status = BudgetOptimizationStatus.APPLIED,
            appliedAt = OffsetDateTime.now(clock),
        )?.toDetails()
            ?: throw BudgetOptimizationConflictException(ErrorMessages.BUDGET_OPTIMIZATION_NOT_PROPOSED)

    override fun dismiss(
        id: UUID,
        userId: UUID,
    ) {
        budgetOptimizationRepository.updateStatus(
            id = id,
            userId = userId,
            status = BudgetOptimizationStatus.DISMISSED,
            appliedAt = null,
        ) ?: throw BudgetOptimizationConflictException(ErrorMessages.BUDGET_OPTIMIZATION_NOT_PROPOSED)
    }

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

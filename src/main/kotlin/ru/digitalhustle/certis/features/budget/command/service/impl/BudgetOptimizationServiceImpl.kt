package ru.digitalhustle.certis.features.budget.command.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.budget.command.repository.BudgetOptimizationRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetOptimizationService
import ru.digitalhustle.certis.features.budget.command.validator.BudgetOptimizationValidator
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationStatus
import ru.digitalhustle.certis.features.budget.exceptions.BudgetOptimizationConflictException
import ru.digitalhustle.certis.features.budget.model.BudgetOptimization
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationDetails
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationInputSnapshot
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationResultSnapshot
import ru.digitalhustle.certis.features.budget.model.CalculatedBudgetOptimization
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class BudgetOptimizationServiceImpl(
    private val budgetOptimizationRepository: BudgetOptimizationRepository,
    private val objectMapper: ObjectMapper,
    private val applicationClock: ApplicationClock,
    private val budgetOptimizationValidator: BudgetOptimizationValidator,
) : BudgetOptimizationService {

    private companion object {
        private const val SNAPSHOT_SCHEMA_VERSION: Short = 1
        private const val ALGORITHM_VERSION = "rule-based-v1"
        private const val ENTITY_NAME = "Budget optimization"
    }

    override fun getProposedForUpdate(
        id: UUID,
        userId: UUID,
        budgetId: UUID,
    ): BudgetOptimizationDetails {
        val optimization = budgetOptimizationRepository.findByIdAndUserIdAndBudgetIdForUpdate(
            id = id,
            userId = userId,
            budgetId = budgetId,
        ) ?: throw NotFoundException.entity(ENTITY_NAME)

        budgetOptimizationValidator.validateProposed(optimization)

        return optimization.toDetails()
    }

    override fun create(
        userId: UUID,
        calculation: CalculatedBudgetOptimization,
    ): BudgetOptimizationDetails {
        val now = applicationClock.now()

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
            appliedAt = applicationClock.now(),
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

package ru.digitalhustle.certis.features.budget.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.features.budget.command.repository.BudgetCategoryConstraintRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetConstraintFundingLevelRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetConstraintRevisionRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetOptimizationRunRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetConstraintCommandService
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BudgetConstraintCommandServiceImpl(
    private val revisionRepository: BudgetConstraintRevisionRepository,
    private val categoryRepository: BudgetCategoryConstraintRepository,
    private val fundingLevelRepository: BudgetConstraintFundingLevelRepository,
    private val optimizationRunRepository: BudgetOptimizationRunRepository,
) : BudgetConstraintCommandService {

    override fun nextRevision(planId: UUID): Int = revisionRepository.nextRevision(planId)

    override fun save(constraints: BudgetConstraintSet) {
        revisionRepository.insert(constraints)
        categoryRepository.insertAll(requireNotNull(constraints.id), constraints.userId, constraints.categories)
        fundingLevelRepository.insertAll(constraints.userId, constraints.categories)
    }

    override fun markGeneratedOptimizationsStale(planId: UUID, userId: UUID, staleAt: OffsetDateTime) {
        optimizationRunRepository.markGeneratedStale(planId, userId, staleAt)
    }
}

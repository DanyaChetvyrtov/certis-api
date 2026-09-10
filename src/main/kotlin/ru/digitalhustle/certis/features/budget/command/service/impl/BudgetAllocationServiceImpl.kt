package ru.digitalhustle.certis.features.budget.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.features.budget.command.repository.BudgetAllocationRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetAllocationService
import ru.digitalhustle.certis.features.budget.model.BudgetCategory
import java.util.UUID

@Service
class BudgetAllocationServiceImpl(
    private val repository: BudgetAllocationRepository,
) : BudgetAllocationService {

    override fun insertAllocations(allocations: Collection<BudgetCategory>): Unit =
        repository.insertAllocations(allocations)

    override fun deleteAllocations(budgetId: UUID): Unit = repository.deleteAllocations(budgetId)
}

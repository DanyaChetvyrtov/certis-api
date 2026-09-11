package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.model.BudgetCategory
import java.util.UUID

interface BudgetAllocationService {

    fun insertAllocations(allocations: Collection<BudgetCategory>)

    fun deleteAllocations(budgetId: UUID)
}

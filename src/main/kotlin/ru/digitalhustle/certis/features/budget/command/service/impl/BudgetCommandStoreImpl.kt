package ru.digitalhustle.certis.features.budget.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.features.budget.command.repository.BudgetRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetCommandStore
import ru.digitalhustle.certis.features.budget.model.Budget
import java.time.LocalDate
import java.util.UUID

@Service
class BudgetCommandStoreImpl(
    private val repository: BudgetRepository,
) : BudgetCommandStore {

    override fun findByUserIdAndMonthForUpdate(userId: UUID, budgetMonth: LocalDate): Budget? =
        repository.findByUserIdAndMonthForUpdate(userId, budgetMonth)

    override fun insert(budget: Budget): Budget = repository.insert(budget)

    override fun update(budget: Budget): Budget = repository.update(budget)
}

package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.model.Budget
import java.time.LocalDate
import java.util.UUID

interface BudgetCommandStore {

    fun findByUserIdAndMonthForUpdate(userId: UUID, budgetMonth: LocalDate): Budget?

    fun insert(budget: Budget): Budget

    fun update(budget: Budget): Budget
}

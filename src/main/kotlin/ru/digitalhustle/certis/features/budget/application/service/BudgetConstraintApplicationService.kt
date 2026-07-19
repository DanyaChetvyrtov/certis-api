package ru.digitalhustle.certis.features.budget.application.service

import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetConstraintsData
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet

interface BudgetConstraintApplicationService {

    fun save(data: SaveBudgetConstraintsData): BudgetConstraintSet
}

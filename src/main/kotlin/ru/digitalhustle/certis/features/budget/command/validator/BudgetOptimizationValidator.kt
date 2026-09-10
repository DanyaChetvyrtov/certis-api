package ru.digitalhustle.certis.features.budget.command.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationStatus
import ru.digitalhustle.certis.features.budget.exceptions.BudgetOptimizationConflictException
import ru.digitalhustle.certis.features.budget.model.BudgetOptimization

@Component
class BudgetOptimizationValidator {

    fun validateProposed(optimization: BudgetOptimization) {
        if (optimization.status != BudgetOptimizationStatus.PROPOSED) {
            throw BudgetOptimizationConflictException(ErrorMessages.BUDGET_OPTIMIZATION_NOT_PROPOSED)
        }
    }
}

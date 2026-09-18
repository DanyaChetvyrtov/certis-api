package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun

@Component
class BudgetPlanningOptimizationFreshnessChecker(
    private val fingerprintCalculator: BudgetPlanningOptimizationFingerprintCalculator,
) {

    fun isCurrent(
        run: BudgetPlanningOptimizationRun,
        forecast: BudgetForecast,
        constraints: BudgetConstraintSet?,
    ): Boolean {
        if (
            forecast.status != BudgetForecastStatus.CURRENT ||
            constraints == null ||
            run.algorithmVersion != BudgetPlanningOptimizationFingerprintCalculator.ALGORITHM_VERSION ||
            run.forecastRevisionId != forecast.id ||
            run.constraintRevisionId != constraints.id
        ) {
            return false
        }
        val fingerprint = fingerprintCalculator.calculate(forecast, constraints, run.input.targetSavingsAmount)
        return run.inputFingerprint == fingerprint
    }
}

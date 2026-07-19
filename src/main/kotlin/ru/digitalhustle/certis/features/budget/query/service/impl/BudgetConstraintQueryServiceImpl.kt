package ru.digitalhustle.certis.features.budget.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.query.repository.BudgetConstraintQueryRepository
import ru.digitalhustle.certis.features.budget.query.service.BudgetConstraintQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetForecastQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanQueryService
import ru.digitalhustle.certis.features.budget.util.BudgetConstraintCalculator
import ru.digitalhustle.certis.features.budget.validation.BudgetConstraintValidator
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BudgetConstraintQueryServiceImpl(
    private val planQueryService: BudgetPlanQueryService,
    private val forecastQueryService: BudgetForecastQueryService,
    private val repository: BudgetConstraintQueryRepository,
    private val calculator: BudgetConstraintCalculator,
    private val validator: BudgetConstraintValidator,
) : BudgetConstraintQueryService {

    override fun getConstraints(planId: UUID, userId: UUID): BudgetConstraintSet {
        val plan = planQueryService.getEntityById(planId, userId)
        val forecast = forecastQueryService.getCurrent(planId, userId)
        validator.validateCurrentForecast(forecast)
        val latest = repository.findLatestByPlanIdAndUserId(planId, userId)
        return if (latest?.forecastRevisionId == forecast.id) {
            latest.copy(planVersion = plan.version)
        } else {
            getSuggestion(plan, forecast)
        }
    }

    override fun getSuggestion(plan: BudgetPlan, forecast: BudgetForecast): BudgetConstraintSet {
        validator.validateCurrentForecast(forecast)
        validator.validateForecastSources(forecast)
        return calculator.suggest(
            plan = plan,
            forecast = forecast,
            baseline = repository.findBaseline(plan.baselineBudgetId, plan.userId),
        )
    }

    override fun findLatest(planId: UUID, userId: UUID): BudgetConstraintSet? =
        repository.findLatestByPlanIdAndUserId(planId, userId)
}

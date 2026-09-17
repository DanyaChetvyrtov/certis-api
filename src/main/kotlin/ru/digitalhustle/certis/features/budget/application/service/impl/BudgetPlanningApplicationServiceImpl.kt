package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningApplicationService
import ru.digitalhustle.certis.features.budget.application.validator.BudgetForecastValidator
import ru.digitalhustle.certis.features.budget.command.model.CancelBudgetPlanData
import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.model.BudgetPlanView
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanQueryService
import ru.digitalhustle.certis.util.time.ApplicationClock

@Service
class BudgetPlanningApplicationServiceImpl(
    private val commandService: BudgetPlanCommandService,
    private val queryService: BudgetPlanQueryService,
    private val forecastValidator: BudgetForecastValidator,
    private val applicationClock: ApplicationClock,
) : BudgetPlanningApplicationService {

    @Transactional
    override fun create(data: CreateBudgetPlanData): BudgetPlanView {
        val plan = commandService.create(data)

        return queryService.getById(plan.id, plan.userId)
    }

    @Transactional
    override fun cancel(data: CancelBudgetPlanData): BudgetPlanView {
        val plan = commandService.getByIdForUpdate(data.planId, data.userId)
        forecastValidator.validatePlan(plan, data.expectedVersion)
        commandService.cancel(plan, applicationClock.now())
        return queryService.getById(plan.id, plan.userId)
    }
}

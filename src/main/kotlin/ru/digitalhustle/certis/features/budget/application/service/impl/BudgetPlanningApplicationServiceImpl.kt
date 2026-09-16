package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningApplicationService
import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.model.BudgetPlanView
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanQueryService

@Service
class BudgetPlanningApplicationServiceImpl(
    private val commandService: BudgetPlanCommandService,
    private val queryService: BudgetPlanQueryService,
) : BudgetPlanningApplicationService {

    @Transactional
    override fun create(data: CreateBudgetPlanData): BudgetPlanView {
        val plan = commandService.create(data)

        return queryService.getById(plan.id, plan.userId)
    }
}

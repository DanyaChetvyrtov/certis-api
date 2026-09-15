package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetPlanCancellationController
import ru.digitalhustle.certis.api.dto.request.CancelBudgetPlanRq
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.api.mapper.BudgetPlanningMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningApplicationService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class BudgetPlanCancellationControllerImpl(
    private val applicationService: BudgetPlanningApplicationService,
    private val mapper: BudgetPlanningMapper,
) : BudgetPlanCancellationController {

    override fun cancelPlan(
        planId: UUID,
        cancelBudgetPlanRq: CancelBudgetPlanRq,
        jwtDetails: JwtDetails,
    ): BudgetPlanRs =
        mapper.convert(
            applicationService.cancel(
                mapper.convert(cancelBudgetPlanRq, planId, jwtDetails.id),
            ),
        )
}

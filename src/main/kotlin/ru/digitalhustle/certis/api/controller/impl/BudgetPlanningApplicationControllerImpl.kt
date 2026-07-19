package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetPlanningApplicationController
import ru.digitalhustle.certis.api.dto.request.ApplyBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationApplyRs
import ru.digitalhustle.certis.api.mapper.BudgetPlanningApplicationMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningBudgetApplicationService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class BudgetPlanningApplicationControllerImpl(
    private val applicationService: BudgetPlanningBudgetApplicationService,
    private val mapper: BudgetPlanningApplicationMapper,
) : BudgetPlanningApplicationController {

    override fun applyOptimization(
        planId: UUID,
        optimizationId: UUID,
        idempotencyKey: String,
        applyBudgetOptimizationRq: ApplyBudgetOptimizationRq,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationApplyRs =
        mapper.convert(
            applicationService.apply(
                mapper.convert(
                    applyBudgetOptimizationRq,
                    planId,
                    optimizationId,
                    jwtDetails.id,
                    idempotencyKey,
                ),
            ),
        )
}

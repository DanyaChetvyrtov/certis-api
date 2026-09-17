package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetPlanningOptimizationController
import ru.digitalhustle.certis.api.dto.request.DismissBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.request.GenerateBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationDismissRs
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationRunRs
import ru.digitalhustle.certis.api.mapper.BudgetPlanningOptimizationMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningOptimizationApplicationService
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanningOptimizationQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class BudgetPlanningOptimizationControllerImpl(
    private val queryService: BudgetPlanningOptimizationQueryService,
    private val applicationService: BudgetPlanningOptimizationApplicationService,
    private val mapper: BudgetPlanningOptimizationMapper,
) : BudgetPlanningOptimizationController {

    override fun getLatestOptimization(planId: UUID, jwtDetails: JwtDetails): BudgetOptimizationRunRs =
        mapper.convert(applicationService.getLatest(planId, jwtDetails.id))

    override fun getOptimization(
        planId: UUID,
        optimizationId: UUID,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationRunRs =
        mapper.convert(queryService.getById(planId, optimizationId, jwtDetails.id))

    override fun generateOptimization(
        planId: UUID,
        idempotencyKey: String,
        generateBudgetOptimizationRq: GenerateBudgetOptimizationRq,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationRunRs =
        mapper.convert(
            applicationService.generate(
                mapper.convert(generateBudgetOptimizationRq, planId, jwtDetails.id, idempotencyKey),
            ),
        )

    override fun dismissOptimization(
        planId: UUID,
        optimizationId: UUID,
        dismissBudgetOptimizationRq: DismissBudgetOptimizationRq,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationDismissRs =
        mapper.convert(
            applicationService.dismiss(
                mapper.convert(dismissBudgetOptimizationRq, planId, optimizationId, jwtDetails.id),
            ),
        )
}

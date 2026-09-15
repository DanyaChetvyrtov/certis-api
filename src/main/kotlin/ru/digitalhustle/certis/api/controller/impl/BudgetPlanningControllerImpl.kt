package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetPlanningController
import ru.digitalhustle.certis.api.dto.request.CreateBudgetPlanRq
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRevisionsRs
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.api.mapper.BudgetPlanningMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetPlanningApplicationService
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.YearMonth
import java.util.UUID

@RestController
class BudgetPlanningControllerImpl(
    private val queryService: BudgetPlanQueryService,
    private val applicationService: BudgetPlanningApplicationService,
    private val mapper: BudgetPlanningMapper,
) : BudgetPlanningController {

    override fun getCurrentPlan(
        month: YearMonth,
        currency: Currency,
        jwtDetails: JwtDetails,
    ): BudgetPlanRs =
        mapper.convert(
            queryService.getCurrent(
                userId = jwtDetails.id,
                budgetMonth = month.atDay(1),
                currency = currency,
            ),
        )

    override fun getPlan(
        planId: UUID,
        jwtDetails: JwtDetails,
    ): BudgetPlanRs = mapper.convert(queryService.getById(planId, jwtDetails.id))

    override fun getPlanRevisions(
        month: YearMonth,
        currency: Currency,
        jwtDetails: JwtDetails,
    ): BudgetPlanRevisionsRs =
        mapper.convert(
            queryService.getRevisions(
                userId = jwtDetails.id,
                budgetMonth = month.atDay(1),
                currency = currency,
            ),
        )

    override fun createPlan(
        idempotencyKey: String,
        createBudgetPlanRq: CreateBudgetPlanRq,
        jwtDetails: JwtDetails,
    ): BudgetPlanRs =
        mapper.convert(
            applicationService.create(
                mapper.convert(
                    source = createBudgetPlanRq,
                    userId = jwtDetails.id,
                    idempotencyKey = idempotencyKey,
                ),
            ),
        )
}

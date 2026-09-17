package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetForecastController
import ru.digitalhustle.certis.api.dto.request.ConfirmBudgetForecastRq
import ru.digitalhustle.certis.api.dto.response.BudgetForecastPreviewRs
import ru.digitalhustle.certis.api.dto.response.BudgetForecastRs
import ru.digitalhustle.certis.api.mapper.BudgetForecastMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetForecastApplicationService
import ru.digitalhustle.certis.features.budget.query.service.BudgetForecastQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class BudgetForecastControllerImpl(
    private val queryService: BudgetForecastQueryService,
    private val applicationService: BudgetForecastApplicationService,
    private val mapper: BudgetForecastMapper,
) : BudgetForecastController {

    override fun getForecastPreview(planId: UUID, jwtDetails: JwtDetails): BudgetForecastPreviewRs =
        mapper.convert(queryService.getPreview(planId, jwtDetails.id))

    override fun getForecast(planId: UUID, jwtDetails: JwtDetails): BudgetForecastRs =
        mapper.convert(queryService.getCurrent(planId, jwtDetails.id))

    override fun confirmForecast(
        planId: UUID,
        confirmBudgetForecastRq: ConfirmBudgetForecastRq,
        jwtDetails: JwtDetails,
    ): BudgetForecastRs =
        mapper.convert(
            applicationService.confirm(
                mapper.convert(
                    source = confirmBudgetForecastRq,
                    planId = planId,
                    userId = jwtDetails.id,
                ),
            ),
        )
}

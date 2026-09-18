package ru.digitalhustle.certis.api.controller

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.dto.request.ConfirmBudgetForecastRq
import ru.digitalhustle.certis.api.dto.response.BudgetForecastPreviewRs
import ru.digitalhustle.certis.api.dto.response.BudgetForecastRs
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@Validated
@RequestMapping(PathConstants.BUDGET_PLANS)
interface BudgetForecastController {

    @GetMapping(PathConstants.BUDGET_PLAN_FORECAST_PREVIEW)
    fun getForecastPreview(
        @PathVariable planId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetForecastPreviewRs

    @GetMapping(PathConstants.BUDGET_PLAN_FORECAST)
    fun getForecast(
        @PathVariable planId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetForecastRs

    @PutMapping(PathConstants.BUDGET_PLAN_FORECAST)
    fun confirmForecast(
        @PathVariable planId: UUID,
        @RequestBody @Valid confirmBudgetForecastRq: ConfirmBudgetForecastRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetForecastRs
}

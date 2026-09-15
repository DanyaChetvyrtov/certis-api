package ru.digitalhustle.certis.api.controller

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.dto.request.CancelBudgetPlanRq
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@Validated
@RequestMapping(PathConstants.BUDGET_PLANS)
interface BudgetPlanCancellationController {

    @PutMapping(PathConstants.BUDGET_PLAN_CANCELLATION)
    fun cancelPlan(
        @PathVariable planId: UUID,
        @RequestBody @Valid cancelBudgetPlanRq: CancelBudgetPlanRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetPlanRs
}

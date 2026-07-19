package ru.digitalhustle.certis.api.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.dto.request.ApplyBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationApplyRs
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@Validated
@RequestMapping(PathConstants.BUDGET_PLANS)
interface BudgetPlanningApplicationController {

    @PutMapping(PathConstants.BUDGET_PLAN_OPTIMIZATION_BUDGET_APPLICATION)
    fun applyOptimization(
        @PathVariable planId: UUID,
        @PathVariable optimizationId: UUID,
        @RequestHeader(name = "Idempotency-Key")
        @NotBlank
        @Size(max = 100)
        idempotencyKey: String,
        @RequestBody @Valid applyBudgetOptimizationRq: ApplyBudgetOptimizationRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetOptimizationApplyRs
}

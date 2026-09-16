package ru.digitalhustle.certis.api.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.dto.request.ApplyBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.request.DismissBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.request.GenerateBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationApplyRs
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationDismissRs
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationRunRs
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@Validated
@RequestMapping(PathConstants.BUDGET_PLANS)
interface BudgetPlanningOptimizationController {

    @GetMapping(PathConstants.BUDGET_PLAN_OPTIMIZATIONS_LATEST)
    fun getLatestOptimization(
        @PathVariable planId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetOptimizationRunRs

    @GetMapping(PathConstants.BUDGET_PLAN_OPTIMIZATION_ID)
    fun getOptimization(
        @PathVariable planId: UUID,
        @PathVariable optimizationId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetOptimizationRunRs

    @PostMapping(PathConstants.BUDGET_PLAN_OPTIMIZATIONS)
    @ResponseStatus(HttpStatus.CREATED)
    fun generateOptimization(
        @PathVariable planId: UUID,
        @RequestHeader(name = "Idempotency-Key")
        @NotBlank
        @Size(max = 100)
        idempotencyKey: String,
        @RequestBody @Valid generateBudgetOptimizationRq: GenerateBudgetOptimizationRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetOptimizationRunRs

    @PutMapping(PathConstants.BUDGET_PLAN_OPTIMIZATION_DISMISSAL)
    fun dismissOptimization(
        @PathVariable planId: UUID,
        @PathVariable optimizationId: UUID,
        @RequestBody @Valid dismissBudgetOptimizationRq: DismissBudgetOptimizationRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetOptimizationDismissRs

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

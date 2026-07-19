package ru.digitalhustle.certis.api.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.dto.request.CreateBudgetPlanRq
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRevisionsRs
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.features.security.model.JwtDetails
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.YearMonth
import java.util.UUID

@Validated
@RequestMapping(PathConstants.BUDGET_PLANS)
interface BudgetPlanningController {

    @GetMapping(PathConstants.BUDGET_PLAN_CURRENT)
    fun getCurrentPlan(
        @RequestParam
        @DateTimeFormat(pattern = "yyyy-MM")
        month: YearMonth,
        @RequestParam currency: Currency,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetPlanRs

    @GetMapping(PathConstants.BUDGET_PLAN_ID)
    fun getPlan(
        @PathVariable planId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetPlanRs

    @GetMapping
    fun getPlanRevisions(
        @RequestParam
        @DateTimeFormat(pattern = "yyyy-MM")
        month: YearMonth,
        @RequestParam currency: Currency,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetPlanRevisionsRs

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPlan(
        @RequestHeader(name = "Idempotency-Key")
        @NotBlank
        @Size(max = 100)
        idempotencyKey: String,
        @RequestBody @Valid createBudgetPlanRq: CreateBudgetPlanRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetPlanRs
}

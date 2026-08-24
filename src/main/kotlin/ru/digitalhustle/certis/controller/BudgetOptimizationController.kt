package ru.digitalhustle.certis.controller

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.response.BudgetOptimizationRs
import ru.digitalhustle.certis.dto.response.BudgetRs
import ru.digitalhustle.certis.model.security.JwtDetails
import java.time.YearMonth
import java.util.UUID

@RequestMapping(PathConstants.BUDGETS)
interface BudgetOptimizationController {

    @GetMapping(PathConstants.BUDGET_OPTIMIZATIONS_LATEST)
    fun getLatestOptimization(
        @PathVariable
        @DateTimeFormat(pattern = "yyyy-MM")
        budgetMonth: YearMonth,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetOptimizationRs

    @PostMapping(PathConstants.BUDGET_OPTIMIZATIONS)
    @ResponseStatus(HttpStatus.CREATED)
    fun generateOptimization(
        @PathVariable
        @DateTimeFormat(pattern = "yyyy-MM")
        budgetMonth: YearMonth,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetOptimizationRs

    @PostMapping(PathConstants.BUDGET_OPTIMIZATION_APPLY)
    fun applyOptimization(
        @PathVariable
        @DateTimeFormat(pattern = "yyyy-MM")
        budgetMonth: YearMonth,
        @PathVariable optimizationId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetRs

    @PostMapping(PathConstants.BUDGET_OPTIMIZATION_DISMISS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun dismissOptimization(
        @PathVariable
        @DateTimeFormat(pattern = "yyyy-MM")
        budgetMonth: YearMonth,
        @PathVariable optimizationId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )
}

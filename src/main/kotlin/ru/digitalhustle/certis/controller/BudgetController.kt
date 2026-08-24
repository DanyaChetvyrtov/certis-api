package ru.digitalhustle.certis.controller

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.SaveBudgetRq
import ru.digitalhustle.certis.dto.response.BudgetRs
import ru.digitalhustle.certis.model.security.JwtDetails
import java.time.YearMonth

@RequestMapping(PathConstants.BUDGETS)
interface BudgetController {

    @GetMapping(PathConstants.BUDGET_MONTH)
    fun getBudget(
        @PathVariable
        @DateTimeFormat(pattern = "yyyy-MM")
        budgetMonth: YearMonth,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetRs

    @PutMapping(PathConstants.BUDGET_MONTH)
    fun saveBudget(
        @PathVariable
        @DateTimeFormat(pattern = "yyyy-MM")
        budgetMonth: YearMonth,
        @RequestBody @Valid saveBudgetRq: SaveBudgetRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetRs
}

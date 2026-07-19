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
import ru.digitalhustle.certis.api.dto.request.SaveBudgetConstraintsRq
import ru.digitalhustle.certis.api.dto.response.BudgetConstraintSetRs
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@Validated
@RequestMapping(PathConstants.BUDGET_PLANS)
interface BudgetConstraintController {

    @GetMapping(PathConstants.BUDGET_PLAN_CONSTRAINTS)
    fun getConstraints(
        @PathVariable planId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetConstraintSetRs

    @PutMapping(PathConstants.BUDGET_PLAN_CONSTRAINTS)
    fun saveConstraints(
        @PathVariable planId: UUID,
        @RequestBody @Valid saveBudgetConstraintsRq: SaveBudgetConstraintsRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): BudgetConstraintSetRs
}

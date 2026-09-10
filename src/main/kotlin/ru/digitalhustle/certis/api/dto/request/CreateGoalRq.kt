package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.YearMonth

data class CreateGoalRq(

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val targetAmount: BigDecimal,

    val currency: Currency,

    val targetMonth: YearMonth,

    @field:Valid
    val contributionPlan: GoalContributionPlanRq,

    @field:Valid
    val initialContribution: InitialGoalContributionRq? = null,

    @field:NotBlank
    @field:Size(max = 50)
    val icon: String,

    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    val color: String,
)

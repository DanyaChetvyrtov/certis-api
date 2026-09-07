package ru.digitalhustle.certis.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Positive
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.YearMonth

data class GoalPlanPreviewRq(

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val targetAmount: BigDecimal,

    @field:DecimalMin("0.0")
    @field:Digits(integer = 15, fraction = 4)
    val initialAmount: BigDecimal = BigDecimal.ZERO,

    val currency: Currency,

    val targetMonth: YearMonth,

    @field:Valid
    val contributionPlan: GoalContributionPlanRq,
)

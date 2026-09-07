package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Positive
import ru.digitalhustle.certis.enums.GoalContributionPlanType
import java.math.BigDecimal

data class GoalContributionPlanRq(

    val type: GoalContributionPlanType,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val monthlyAmount: BigDecimal? = null,
) {

    @get:AssertTrue(message = "monthlyAmount must be set only for a custom contribution plan")
    val validMonthlyAmount: Boolean
        get() = when (type) {
            GoalContributionPlanType.CUSTOM -> monthlyAmount != null
            GoalContributionPlanType.RECOMMENDED -> monthlyAmount == null
        }
}

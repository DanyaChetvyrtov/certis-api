package ru.digitalhustle.certis.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.enums.GoalStatus
import java.math.BigDecimal
import java.time.YearMonth

data class UpdateGoalRq(

    @field:Size(min = 1, max = 100)
    val name: String? = null,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val targetAmount: BigDecimal? = null,

    val targetMonth: YearMonth? = null,

    @field:Valid
    val contributionPlan: GoalContributionPlanRq? = null,

    @field:Size(min = 1, max = 50)
    val icon: String? = null,

    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    val color: String? = null,

    val status: GoalStatus? = null,
) {

    @get:AssertTrue(message = "at least one field must be provided")
    val hasChanges: Boolean
        get() = listOf(name, targetAmount, targetMonth, contributionPlan, icon, color, status).any { it != null }
}

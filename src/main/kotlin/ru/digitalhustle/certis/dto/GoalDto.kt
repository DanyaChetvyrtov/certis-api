package ru.digitalhustle.certis.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalContributionPlanType
import ru.digitalhustle.certis.enums.GoalPaceStatus
import ru.digitalhustle.certis.enums.GoalStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GoalDto(

    val id: UUID,

    val name: String,

    val currency: Currency,

    val targetAmount: BigDecimal,

    val savedAmount: BigDecimal,

    val remainingAmount: BigDecimal,

    val progressPercentage: BigDecimal,

    val targetMonth: String?,

    val monthsRemaining: Int?,

    val contributionPlan: GoalContributionPlanDto,

    val status: GoalStatus,

    val paceStatus: GoalPaceStatus,

    val projectedCompletionMonth: String?,

    val icon: String,

    val color: String,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,

    val achievedAt: OffsetDateTime?,

    val archivedAt: OffsetDateTime?,
)

data class GoalContributionPlanDto(

    val type: GoalContributionPlanType,

    val monthlyAmount: BigDecimal,

    val recommendedMonthlyAmount: BigDecimal,
)

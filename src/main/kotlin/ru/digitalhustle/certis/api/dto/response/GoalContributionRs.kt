package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.GoalContributionDto
import ru.digitalhustle.certis.features.goal.enums.GoalPaceStatus
import ru.digitalhustle.certis.features.goal.enums.GoalStatus
import java.math.BigDecimal
import java.util.UUID

data class GoalContributionRs(

    val contribution: GoalContributionDto,

    val goal: GoalProgressRs,
)

data class GoalProgressRs(

    val id: UUID,

    val savedAmount: BigDecimal,

    val remainingAmount: BigDecimal,

    val progressPercentage: BigDecimal,

    val status: GoalStatus,

    val paceStatus: GoalPaceStatus,
)

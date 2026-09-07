package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalPaceStatus
import java.math.BigDecimal

data class GoalPlanPreviewRs(

    val currency: Currency,

    val targetAmount: BigDecimal,

    val initialAmount: BigDecimal,

    val remainingAmount: BigDecimal,

    val progressPercentage: BigDecimal,

    val targetMonth: String,

    val contributionMonths: Int,

    val recommendedMonthlyAmount: BigDecimal,

    val selectedMonthlyAmount: BigDecimal,

    val projectedCompletionMonth: String?,

    val paceStatus: GoalPaceStatus,
)

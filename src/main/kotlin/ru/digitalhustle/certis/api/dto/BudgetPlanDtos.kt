package ru.digitalhustle.certis.api.dto

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetPlanForecastStateDto(

    val revision: Int?,

    val status: BudgetForecastStatus,

    val summary: BudgetForecastSummaryDto?,
)

data class BudgetPlanConstraintStateDto(

    val revision: Int?,

    val status: BudgetConstraintStatus,

    val feasibility: BudgetFeasibilityDto?,
)

data class BudgetPlanOptimizationStateDto(

    val id: UUID,

    val status: BudgetOptimizationRunStatus,

    val targetSavingsAmount: BigDecimal,

    val actualSavingsAmount: BigDecimal?,

    val createdAt: OffsetDateTime,
)

data class BudgetPlanCapabilitiesDto(

    val canEditForecast: Boolean,

    val canEditConstraints: Boolean,

    val canRunOptimization: Boolean,

    val canApply: Boolean,

    val canCancel: Boolean,
)

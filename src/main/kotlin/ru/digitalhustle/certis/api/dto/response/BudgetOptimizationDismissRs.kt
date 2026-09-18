package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.api.dto.BudgetPlanningStep
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetOptimizationDismissRs(

    val optimizationId: UUID,

    val status: BudgetOptimizationRunStatus,

    val planVersion: Long,

    val currentStep: BudgetPlanningStep,

    val dismissedAt: OffsetDateTime,
)

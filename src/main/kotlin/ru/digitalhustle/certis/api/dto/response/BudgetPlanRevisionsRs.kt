package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetPlanStatus
import ru.digitalhustle.certis.api.dto.BudgetPlanningStep
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetPlanRevisionsRs(

    val items: List<BudgetPlanRevisionDto>,
)

data class BudgetPlanRevisionDto(

    val id: UUID,

    val revision: Int,

    val status: BudgetPlanStatus,

    val currentStep: BudgetPlanningStep,

    val createdAt: OffsetDateTime,

    val appliedAt: OffsetDateTime?,
)

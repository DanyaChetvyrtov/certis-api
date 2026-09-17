package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetCategoryConstraintDto
import ru.digitalhustle.certis.api.dto.BudgetConstraintStatus
import ru.digitalhustle.certis.api.dto.BudgetFeasibilityDto
import java.math.BigDecimal
import java.util.UUID

data class BudgetConstraintSetRs(

    val planId: UUID,

    val basedOnForecastRevision: Int,

    val revision: Int?,

    val status: BudgetConstraintStatus,

    val savingsFloorAmount: BigDecimal,

    val categories: List<BudgetCategoryConstraintDto>,

    val feasibility: BudgetFeasibilityDto,

    val planVersion: Long,
)

package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetPlanCapabilitiesDto
import ru.digitalhustle.certis.api.dto.BudgetPlanConstraintStateDto
import ru.digitalhustle.certis.api.dto.BudgetPlanForecastStateDto
import ru.digitalhustle.certis.api.dto.BudgetPlanOptimizationStateDto
import ru.digitalhustle.certis.api.dto.BudgetPlanStatus
import ru.digitalhustle.certis.api.dto.BudgetPlanningStep
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetPlanRs(

    val id: UUID,

    val previousPlanId: UUID?,

    val month: String,

    val currency: Currency,

    val revision: Int,

    val status: BudgetPlanStatus,

    val currentStep: BudgetPlanningStep,

    val version: Long,

    val forecast: BudgetPlanForecastStateDto,

    val constraints: BudgetPlanConstraintStateDto,

    val currentOptimization: BudgetPlanOptimizationStateDto?,

    val capabilities: BudgetPlanCapabilitiesDto,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,
)

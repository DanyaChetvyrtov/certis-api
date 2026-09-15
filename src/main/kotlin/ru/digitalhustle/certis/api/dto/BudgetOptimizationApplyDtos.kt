package ru.digitalhustle.certis.api.dto

import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetAppliedPlanDto(

    val id: UUID,

    val revision: Int,

    val status: BudgetPlanStatus,

    val currentStep: BudgetPlanningStep,

    val version: Long,

    val appliedAt: OffsetDateTime,
)

data class BudgetAppliedOptimizationDto(

    val id: UUID,

    val status: BudgetOptimizationRunStatus,
)

data class BudgetAppliedBudgetDto(

    val id: UUID,

    val month: String,

    val currency: Currency,

    val totalLimit: BigDecimal,

    val sourceOptimizationId: UUID,

    val allocations: List<BudgetAppliedAllocationDto>,
)

data class BudgetAppliedAllocationDto(

    val categoryId: UUID,

    val limit: BigDecimal,
)

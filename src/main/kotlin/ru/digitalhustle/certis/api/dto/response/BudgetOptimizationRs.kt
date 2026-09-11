package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetOptimizationAllocationDto
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetOptimizationRs(

    val id: UUID,

    val budgetId: UUID,

    val month: String,

    val currency: Currency,

    val algorithmVersion: String,

    val status: BudgetOptimizationStatus,

    val savingsBefore: BigDecimal,

    val savingsAfter: BigDecimal,

    val additionalSavings: BigDecimal,

    val allocations: List<BudgetOptimizationAllocationDto>,

    val createdAt: OffsetDateTime,

    val appliedAt: OffsetDateTime?,
)

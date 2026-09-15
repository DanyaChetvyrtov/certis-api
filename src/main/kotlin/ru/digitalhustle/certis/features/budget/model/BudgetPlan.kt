package ru.digitalhustle.certis.features.budget.model

import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetPlan(

    val id: UUID,

    val userId: UUID,

    val previousPlanId: UUID?,

    val baselineBudgetId: UUID?,

    val appliedBudgetId: UUID?,

    val budgetMonth: LocalDate,

    val currency: Currency,

    val revision: Int,

    val version: Long,

    val status: BudgetPlanStatus,

    val idempotencyKey: String,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,

    val appliedAt: OffsetDateTime?,

    val supersededAt: OffsetDateTime?,

    val cancelledAt: OffsetDateTime?,
)

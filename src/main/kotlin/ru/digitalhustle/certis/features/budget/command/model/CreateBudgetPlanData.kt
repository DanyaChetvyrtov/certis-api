package ru.digitalhustle.certis.features.budget.command.model

import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.util.UUID

data class CreateBudgetPlanData(
    val userId: UUID,
    val budgetMonth: LocalDate,
    val currency: Currency,
    val idempotencyKey: String,
)

data class CancelBudgetPlanData(
    val planId: UUID,
    val userId: UUID,
    val expectedVersion: Long,
)

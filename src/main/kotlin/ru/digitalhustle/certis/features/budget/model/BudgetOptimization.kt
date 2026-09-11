package ru.digitalhustle.certis.features.budget.model

import com.fasterxml.jackson.databind.JsonNode
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetOptimization(

    val id: UUID,

    val userId: UUID,

    val budgetId: UUID,

    val snapshotSchemaVersion: Short,

    val algorithmVersion: String,

    val status: BudgetOptimizationStatus,

    val inputSnapshot: JsonNode,

    val resultSnapshot: JsonNode,

    val savingsBefore: BigDecimal,

    val savingsAfter: BigDecimal,

    val createdAt: OffsetDateTime,

    val appliedAt: OffsetDateTime?,
)

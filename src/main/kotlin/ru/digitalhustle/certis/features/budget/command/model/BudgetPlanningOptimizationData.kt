package ru.digitalhustle.certis.features.budget.command.model

import java.math.BigDecimal
import java.util.UUID

data class GenerateBudgetPlanningOptimizationData(
    val planId: UUID,
    val userId: UUID,
    val idempotencyKey: String,
    val expectedVersion: Long,
    val forecastRevision: Int,
    val constraintsRevision: Int,
    val targetSavingsAmount: BigDecimal,
)

data class DismissBudgetPlanningOptimizationData(
    val planId: UUID,
    val optimizationId: UUID,
    val userId: UUID,
    val expectedVersion: Long,
)

data class ApplyBudgetPlanningOptimizationData(
    val planId: UUID,
    val optimizationId: UUID,
    val userId: UUID,
    val idempotencyKey: String,
    val expectedVersion: Long,
)

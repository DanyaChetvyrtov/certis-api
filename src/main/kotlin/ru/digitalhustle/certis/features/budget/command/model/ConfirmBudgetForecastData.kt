package ru.digitalhustle.certis.features.budget.command.model

import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ConfirmBudgetForecastData(
    val planId: UUID,
    val userId: UUID,
    val expectedVersion: Long,
    val sourceFingerprint: String,
    val overrides: List<BudgetForecastOverrideData>,
    val manualAdjustments: List<BudgetForecastManualAdjustmentData>,
)

data class BudgetForecastOverrideData(
    val sourceKey: String,
    val included: Boolean,
    val amount: BigDecimal?,
)

data class BudgetForecastManualAdjustmentData(
    val clientId: UUID,
    val operationType: BudgetForecastOperationType,
    val title: String,
    val categoryId: UUID?,
    val expectedDate: LocalDate?,
    val amount: BigDecimal,
)

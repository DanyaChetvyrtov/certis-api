package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.api.dto.BudgetForecastOperationType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ConfirmBudgetForecastRq(

    @field:PositiveOrZero
    val expectedVersion: Long,

    @field:Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    val sourceFingerprint: String,

    @field:Valid
    @field:Size(max = 500)
    val overrides: List<BudgetForecastOverrideRq> = emptyList(),

    @field:Valid
    @field:Size(max = 500)
    val manualAdjustments: List<BudgetForecastManualAdjustmentRq> = emptyList(),
)

data class BudgetForecastOverrideRq(

    @field:NotBlank
    @field:Size(max = 255)
    val sourceKey: String,

    val included: Boolean,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val amount: BigDecimal?,
)

data class BudgetForecastManualAdjustmentRq(

    val clientId: UUID,

    val operationType: BudgetForecastOperationType,

    @field:NotBlank
    @field:Size(max = 255)
    val title: String,

    val categoryId: UUID?,

    val expectedDate: LocalDate?,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val amount: BigDecimal,
)

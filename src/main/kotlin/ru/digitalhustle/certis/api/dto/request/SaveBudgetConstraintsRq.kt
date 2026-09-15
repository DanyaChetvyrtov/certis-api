package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.api.dto.BudgetAllocationType
import ru.digitalhustle.certis.api.dto.BudgetConstraintRole
import ru.digitalhustle.certis.api.dto.BudgetFundingLevel
import ru.digitalhustle.certis.api.dto.BudgetPriority
import java.math.BigDecimal
import java.util.UUID

data class SaveBudgetConstraintsRq(

    @field:PositiveOrZero
    val expectedVersion: Long,

    @field:Positive
    val forecastRevision: Int,

    @field:PositiveOrZero
    @field:Digits(integer = 15, fraction = 4)
    val savingsFloorAmount: BigDecimal,

    @field:Valid
    @field:Size(max = 500)
    val categories: List<BudgetCategoryConstraintRq>,
)

data class BudgetCategoryConstraintRq(

    val categoryId: UUID,

    val allocationType: BudgetAllocationType,

    val constraintRole: BudgetConstraintRole,

    @field:PositiveOrZero
    @field:Digits(integer = 15, fraction = 4)
    val requiredAmount: BigDecimal,

    val priority: BudgetPriority?,

    @field:Valid
    @field:Size(max = 3)
    val fundingLevels: List<BudgetFundingLevelRq>,
)

data class BudgetFundingLevelRq(

    val level: BudgetFundingLevel,

    @field:PositiveOrZero
    @field:Digits(integer = 15, fraction = 4)
    val amount: BigDecimal,
)

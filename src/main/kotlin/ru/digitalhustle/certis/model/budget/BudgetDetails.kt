package ru.digitalhustle.certis.model.budget

import ru.digitalhustle.certis.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetDetails(

    val id: UUID,

    val budgetMonth: LocalDate,

    val plannedIncome: BigDecimal,

    val savingsTarget: BigDecimal,

    val currency: Currency,

    val allocations: List<BudgetAllocationDetails>,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,
)

data class BudgetAllocationDetails(

    val id: UUID,

    val categoryId: UUID,

    val categoryName: String,

    val categoryIcon: String,

    val categoryColor: String,

    val expenseType: BudgetExpenseType,

    val limitAmount: BigDecimal,

    val spentAmount: BigDecimal,

    val status: BudgetAllocationStatus,
) {
    companion object {
        private val NEAR_LIMIT_PERCENT = BigDecimal("85")
        private val ONE_HUNDRED = BigDecimal("100")

        fun create(
            id: UUID,
            categoryId: UUID,
            categoryName: String,
            categoryIcon: String,
            categoryColor: String,
            expenseType: BudgetExpenseType,
            limitAmount: BigDecimal,
            spentAmount: BigDecimal,
        ): BudgetAllocationDetails =
            BudgetAllocationDetails(
                id = id,
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColor = categoryColor,
                expenseType = expenseType,
                limitAmount = limitAmount,
                spentAmount = spentAmount,
                status = calculateStatus(limitAmount, spentAmount),
            )

        private fun calculateStatus(
            limitAmount: BigDecimal,
            spentAmount: BigDecimal,
        ): BudgetAllocationStatus =
            when {
                spentAmount > limitAmount -> BudgetAllocationStatus.OVERSPENT
                limitAmount.signum() > 0 &&
                    spentAmount * ONE_HUNDRED >= limitAmount * NEAR_LIMIT_PERCENT ->
                    BudgetAllocationStatus.NEAR_LIMIT
                else -> BudgetAllocationStatus.ON_TRACK
            }
    }
}

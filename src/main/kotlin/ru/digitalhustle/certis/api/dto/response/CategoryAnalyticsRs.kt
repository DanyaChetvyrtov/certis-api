package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.math.BigDecimal
import java.util.UUID

data class CategoryAnalyticsRs(

    val month: String,

    val currency: Currency,

    val type: CategoryType,

    val totalTransactionCount: Int,

    val categorizedTransactionCount: Int,

    val uncategorizedTransactionCount: Int,

    val totalSum: BigDecimal,

    val categorizedSum: BigDecimal,

    val uncategorizedSum: BigDecimal,

    val coveragePercentage: BigDecimal?,

    val topExpenseCategories: List<TopCategoryAnalyticsRs>,
)

data class TopCategoryAnalyticsRs(

    val categoryId: UUID,

    val name: String,

    val color: String,

    val amount: BigDecimal,

    val sharePercentage: BigDecimal,
)

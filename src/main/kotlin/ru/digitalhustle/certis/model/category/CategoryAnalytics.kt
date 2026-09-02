package ru.digitalhustle.certis.model.category

import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class CategoryAnalyticsFilter(

    val month: YearMonth,

    val currency: Currency,

    val topLimit: Int,

    val type: CategoryType,
)

data class CategoryAnalytics(

    val month: YearMonth,

    val currency: Currency,

    val type: CategoryType,

    val totalTransactionCount: Int,

    val categorizedTransactionCount: Int,

    val uncategorizedTransactionCount: Int,

    val totalSum: BigDecimal,

    val categorizedSum: BigDecimal,

    val uncategorizedSum: BigDecimal,

    val coveragePercentage: BigDecimal?,

    val topExpenseCategories: List<TopCategoryAnalytics>,
)

data class TopCategoryAnalytics(

    val categoryId: UUID,

    val name: String,

    val color: String,

    val amount: BigDecimal,

    val sharePercentage: BigDecimal,
)

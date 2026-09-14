package ru.digitalhustle.certis.features.category.query.model

import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class CategorySpendingOverTimeFilter(

    val month: YearMonth,

    val currency: Currency,

    val type: CategoryType,

    val bucketCount: Int,

    val topLimit: Int,
)

data class CategorySpendingOverTime(

    val month: YearMonth,

    val currency: Currency,

    val type: CategoryType,

    val totalSum: BigDecimal,

    val series: List<CategorySpendingSeriesData>,
)

data class CategorySpendingSeriesData(

    val categoryId: UUID?,

    val categoryName: String,

    val categoryColor: String?,

    val total: BigDecimal,

    val points: List<CategorySpendingPoint>,
)

data class CategorySpendingPoint(

    val bucketMonth: YearMonth,

    val amount: BigDecimal,
)

data class CategorySpendingQueryResult(

    val topCategories: List<CategorySpendingCategory>,

    val buckets: List<CategorySpendingBucket>,
)

data class CategorySpendingCategory(

    val categoryId: UUID,

    val categoryName: String,

    val categoryColor: String,
)

data class CategorySpendingBucket(

    val categoryId: UUID?,

    val bucketMonth: YearMonth,

    val amount: BigDecimal,
)

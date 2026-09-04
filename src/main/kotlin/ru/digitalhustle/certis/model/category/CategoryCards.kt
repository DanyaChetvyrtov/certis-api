package ru.digitalhustle.certis.model.category

import ru.digitalhustle.certis.enums.CategoryCardSort
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

data class CategoryCards(

    val month: YearMonth,

    val currency: Currency,

    val categories: List<CategoryCard>,

    val page: Int,

    val size: Int,

    val totalElements: Long,
) {

    val totalPages: Int =
        if (totalElements == 0L) {
            0
        } else {
            ((totalElements - 1) / size + 1).toInt()
        }
}

data class CategoryCardFilter(

    val month: YearMonth,

    val currency: Currency,

    val page: Int,

    val size: Int,

    val sort: CategoryCardSort,
)

data class CategoryCard(

    val id: UUID,

    val name: String,

    val type: CategoryType,

    val icon: String,

    val color: String,

    val archivedAt: OffsetDateTime?,

    val monthlyTransactionCount: Int,

    val monthlyAmount: BigDecimal,

    val monthlySharePercentage: BigDecimal,
)

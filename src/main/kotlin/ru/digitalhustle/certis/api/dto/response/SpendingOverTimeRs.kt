package ru.digitalhustle.certis.api.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class SpendingOverTimeRs(

    @JsonFormat(pattern = "yyyy-MM")
    val month: YearMonth,

    val currency: Currency,

    val type: CategoryType,

    val totalSum: BigDecimal,

    val series: List<CategorySpendingSeries>,
)

data class CategorySpendingSeries(

    val categoryId: UUID?,

    val categoryName: String,

    val categoryColor: String?,

    val total: BigDecimal,

    val points: List<CategorySeries>,
)

data class CategorySeries(

    @JsonFormat(pattern = "yyyy-MM")
    val bucketMonth: YearMonth,

    val amount: BigDecimal,
)

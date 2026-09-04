package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.enums.CategoryCardSort
import ru.digitalhustle.certis.enums.Currency
import java.time.YearMonth

data class CategoryCardPageRq(

    @field:Min(0)
    @field:Max(MAX_PAGE)
    val page: Int = 0,

    @field:Min(1)
    @field:Max(MAX_SIZE)
    val size: Int = DEFAULT_SIZE,

    @DateTimeFormat(pattern = "yyyy-MM")
    val month: YearMonth,

    val currency: Currency,

    val sort: CategoryCardSort = CategoryCardSort.AMOUNT_DESC,
) {

    companion object {
        const val MAX_PAGE = 1_000_000L
        const val MAX_SIZE = 100L
        const val DEFAULT_SIZE = 20
    }
}

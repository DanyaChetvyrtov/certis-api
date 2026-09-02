package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import java.time.YearMonth

data class CategoryAnalyticsRq(

    @DateTimeFormat(pattern = "yyyy-MM")
    val month: YearMonth,

    val currency: Currency,

    @field:Min(1)
    @field:Max(MAX_TOP_LIMIT)
    val topLimit: Int = DEFAULT_TOP_LIMIT,

    val type: CategoryType,
) {

    companion object {
        const val DEFAULT_TOP_LIMIT = 4
        const val MAX_TOP_LIMIT = 100L
    }
}

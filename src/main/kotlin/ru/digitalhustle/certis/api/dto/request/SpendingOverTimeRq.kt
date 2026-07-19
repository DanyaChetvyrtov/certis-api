package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.YearMonth

data class SpendingOverTimeRq(

    @DateTimeFormat(pattern = "yyyy-MM")
    val month: YearMonth,

    val currency: Currency,

    val type: CategoryType,

    @field:Min(1)
    @field:Max(MAX_BUCKET_COUNT)
    val bucketCount: Int = DEFAULT_BUCKET_COUNT,

    @field:Min(1)
    @field:Max(MAX_TOP_LIMIT)
    val topLimit: Int = DEFAULT_TOP_LIMIT,
) {

    companion object {
        const val DEFAULT_BUCKET_COUNT = 6
        const val MAX_BUCKET_COUNT = 24L
        const val DEFAULT_TOP_LIMIT = 5
        const val MAX_TOP_LIMIT = 10L
    }
}

package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import java.time.YearMonth
import java.util.UUID

data class UncategorizedTransactionFilterRq(

    @DateTimeFormat(pattern = "yyyy-MM")
    val month: YearMonth,

    val currency: Currency,

    val type: TransactionType,

    val accountId: UUID? = null,

    @field:Size(max = MAX_SEARCH_LENGTH)
    val search: String? = null,

    @field:Min(0)
    @field:Max(MAX_PAGE)
    val page: Int = 0,

    @field:Min(1)
    @field:Max(MAX_SIZE)
    val size: Int = DEFAULT_SIZE,
) {
    companion object {
        const val MAX_PAGE = 1_000_000L
        const val MAX_SIZE = 100L
        const val DEFAULT_SIZE = 20
        const val MAX_SEARCH_LENGTH = 255
    }
}

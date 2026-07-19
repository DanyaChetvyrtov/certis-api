package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.enums.TransactionType
import java.time.OffsetDateTime
import java.util.UUID

data class TransactionFilterRq(

    val accountId: UUID? = null,

    val categoryId: UUID? = null,

    val type: TransactionType? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val from: OffsetDateTime? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val to: OffsetDateTime? = null,

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
    }

    @get:AssertTrue(message = "from must be before or equal to to")
    val validDateRange: Boolean
        get() = from == null || to == null || !from.isAfter(to)
}

package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalSort
import ru.digitalhustle.certis.enums.GoalStatus

data class GoalFilterRq(

    val currency: Currency? = null,

    val status: GoalStatus = GoalStatus.ACTIVE,

    val sort: GoalSort = GoalSort.TARGET_MONTH_ASC,

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
}

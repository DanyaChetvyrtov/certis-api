package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import ru.digitalhustle.certis.enums.GoalContributionSort

data class GoalContributionFilterRq(

    val sort: GoalContributionSort = GoalContributionSort.CONTRIBUTED_AT_DESC,

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

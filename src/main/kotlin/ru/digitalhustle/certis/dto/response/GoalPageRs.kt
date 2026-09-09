package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.dto.GoalDto
import ru.digitalhustle.certis.enums.Currency

data class GoalPageRs(

    val currency: Currency,

    val items: List<GoalDto>,

    val statusCounts: GoalStatusCountsRs,

    val page: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,
)

data class GoalStatusCountsRs(

    val active: Long,

    val completed: Long,
)

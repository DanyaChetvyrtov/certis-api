package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.GoalContributionDto

data class GoalContributionPageRs(

    val items: List<GoalContributionDto>,

    val page: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,
)

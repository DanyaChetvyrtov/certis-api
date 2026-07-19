package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetForecastItemDto
import ru.digitalhustle.certis.api.dto.BudgetForecastStatus
import ru.digitalhustle.certis.api.dto.BudgetForecastSummaryDto
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetForecastRs(

    val planId: UUID,

    val revision: Int,

    val status: BudgetForecastStatus,

    val sourceFingerprint: String,

    val inputFingerprint: String,

    val summary: BudgetForecastSummaryDto,

    val items: List<BudgetForecastItemDto>,

    val planVersion: Long,

    val confirmedAt: OffsetDateTime,
)

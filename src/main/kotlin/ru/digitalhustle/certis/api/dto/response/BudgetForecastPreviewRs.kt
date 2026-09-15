package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetForecastHistoryWindowDto
import ru.digitalhustle.certis.api.dto.BudgetForecastItemDto
import ru.digitalhustle.certis.api.dto.BudgetForecastSummaryDto
import ru.digitalhustle.certis.api.dto.BudgetForecastWarningDto
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetForecastPreviewRs(

    val planId: UUID,

    val basedOnPlanVersion: Long,

    val sourceFingerprint: String,

    val generatedAt: OffsetDateTime,

    val historyWindow: BudgetForecastHistoryWindowDto,

    val summary: BudgetForecastSummaryDto,

    val items: List<BudgetForecastItemDto>,

    val warnings: List<BudgetForecastWarningDto>,
)

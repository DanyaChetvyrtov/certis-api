package ru.digitalhustle.certis.model.entity

import com.fasterxml.jackson.databind.JsonNode
import ru.digitalhustle.certis.enums.InsightSeverity
import ru.digitalhustle.certis.enums.InsightType
import java.time.OffsetDateTime
import java.util.UUID

data class Insight(

    val id: UUID,

    val userId: UUID,

    val type: InsightType,

    val title: String,

    val description: String,

    val severity: InsightSeverity,

    val metadata: JsonNode?,

    val createdAt: OffsetDateTime,
)

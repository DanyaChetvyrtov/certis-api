package ru.digitalhustle.certis.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExceptionRs(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: OffsetDateTime,
    val errors: Map<String, String>?,
)

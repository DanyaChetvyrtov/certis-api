package ru.digitalhustle.certis.dto.request

import java.time.OffsetDateTime

data class ReverseTransferRq(

    val note: String?,

    val occurredAt: OffsetDateTime,
)

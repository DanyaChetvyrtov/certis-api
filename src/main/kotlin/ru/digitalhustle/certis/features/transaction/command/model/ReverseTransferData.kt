package ru.digitalhustle.certis.features.transaction.command.model

import java.time.OffsetDateTime
import java.util.UUID

data class ReverseTransferData(

    val userId: UUID,

    val transferId: UUID,

    val note: String?,

    val occurredAt: OffsetDateTime,
)

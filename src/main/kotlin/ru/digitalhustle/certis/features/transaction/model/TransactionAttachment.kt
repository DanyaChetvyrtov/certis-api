package ru.digitalhustle.certis.features.transaction.model

import java.util.UUID

data class TransactionAttachment(

    val id: UUID,

    val transactionId: UUID,

    val fileUrl: String,

    val fileType: String,
)

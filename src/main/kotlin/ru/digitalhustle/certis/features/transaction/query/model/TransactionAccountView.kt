package ru.digitalhustle.certis.features.transaction.query.model

import java.util.UUID

data class TransactionAccountView(
    val id: UUID,
    val name: String,
    val type: String,
)

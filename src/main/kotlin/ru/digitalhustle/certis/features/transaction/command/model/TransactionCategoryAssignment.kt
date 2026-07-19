package ru.digitalhustle.certis.features.transaction.command.model

import java.util.UUID

data class AssignTransactionsCategory(

    val userId: UUID,

    val assignments: List<TransactionCategoryAssignment>,
)

data class TransactionCategoryAssignment(

    val transactionId: UUID,

    val categoryId: UUID,
)

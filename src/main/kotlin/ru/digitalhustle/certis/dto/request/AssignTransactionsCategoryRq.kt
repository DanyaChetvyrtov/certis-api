package ru.digitalhustle.certis.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

data class AssignTransactionsCategoryRq(

    @field:NotEmpty
    @field:Size(max = MAX_TRANSACTIONS)
    @field:Valid
    val assignments: List<TransactionCategoryAssignmentRq>,
) {

    companion object {
        const val MAX_TRANSACTIONS = 100
    }
}

data class TransactionCategoryAssignmentRq(

    val transactionId: UUID,

    val categoryId: UUID,
)

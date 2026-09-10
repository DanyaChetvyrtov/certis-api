package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.RecurringTransactionDto

data class RecurringTransactionsRs(

    val recurringTransactions: List<RecurringTransactionDto>,
)

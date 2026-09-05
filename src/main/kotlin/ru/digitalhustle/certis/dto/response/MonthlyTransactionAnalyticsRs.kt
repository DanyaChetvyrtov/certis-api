package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal

data class MonthlyTransactionAnalyticsRs(

    val month: String,

    val currency: Currency,

    val income: MonthlyTransactionTotalRs,

    val expenses: MonthlyTransactionTotalRs,

    val netCashFlow: BigDecimal,
)

data class MonthlyTransactionTotalRs(

    val transactionCount: Int,

    val amount: BigDecimal,
)

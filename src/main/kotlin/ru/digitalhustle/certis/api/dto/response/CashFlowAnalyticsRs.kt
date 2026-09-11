package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.transaction.enums.CashFlowGranularity
import ru.digitalhustle.certis.features.transaction.enums.CashFlowRange
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CashFlowAnalyticsRs(

    val range: CashFlowRange,

    val currency: Currency,

    val granularity: CashFlowGranularity,

    val from: OffsetDateTime,

    val toExclusive: OffsetDateTime,

    val totals: CashFlowTotalsRs,

    val points: List<CashFlowPointRs>,
)

data class CashFlowTotalsRs(

    val income: BigDecimal,

    val expenses: BigDecimal,

    val netCashFlow: BigDecimal,
)

data class CashFlowPointRs(

    val bucketStart: OffsetDateTime,

    val income: BigDecimal,

    val expenses: BigDecimal,

    val netCashFlow: BigDecimal,
)

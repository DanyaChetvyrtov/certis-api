package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.request.CashFlowAnalyticsRq
import ru.digitalhustle.certis.dto.request.MonthlyTransactionAnalyticsRq
import ru.digitalhustle.certis.dto.response.CashFlowAnalyticsRs
import ru.digitalhustle.certis.dto.response.CashFlowPointRs
import ru.digitalhustle.certis.dto.response.CashFlowTotalsRs
import ru.digitalhustle.certis.dto.response.MonthlyTransactionAnalyticsRs
import ru.digitalhustle.certis.dto.response.MonthlyTransactionTotalRs
import ru.digitalhustle.certis.model.transaction.CashFlowAnalytics
import ru.digitalhustle.certis.model.transaction.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.CashFlowPoint
import ru.digitalhustle.certis.model.transaction.CashFlowTotals
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionTotal

@Mapper(config = BaseMapperConfig::class)
interface TransactionAnalyticsMapper {

    fun convert(source: MonthlyTransactionAnalyticsRq): MonthlyTransactionAnalyticsFilter

    @Mapping(target = "month", expression = "java(source.getMonth().toString())")
    fun convert(source: MonthlyTransactionAnalytics): MonthlyTransactionAnalyticsRs

    fun convert(source: MonthlyTransactionTotal): MonthlyTransactionTotalRs

    @Mapping(target = "timeZone", expression = "java(java.time.ZoneId.of(source.getTimeZone()))")
    fun convert(source: CashFlowAnalyticsRq): CashFlowAnalyticsFilter

    fun convert(source: CashFlowAnalytics): CashFlowAnalyticsRs

    fun convert(source: CashFlowTotals): CashFlowTotalsRs

    fun convert(source: CashFlowPoint): CashFlowPointRs
}

package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.request.CashFlowAnalyticsRq
import ru.digitalhustle.certis.api.dto.request.MonthlyTransactionAnalyticsRq
import ru.digitalhustle.certis.api.dto.response.CashFlowAnalyticsRs
import ru.digitalhustle.certis.api.dto.response.CashFlowPointRs
import ru.digitalhustle.certis.api.dto.response.CashFlowTotalsRs
import ru.digitalhustle.certis.api.dto.response.MonthlyTransactionAnalyticsRs
import ru.digitalhustle.certis.api.dto.response.MonthlyTransactionTotalRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowAnalytics
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowPoint
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowTotals
import ru.digitalhustle.certis.features.transaction.query.model.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.features.transaction.query.model.MonthlyTransactionAnalyticsFilter
import ru.digitalhustle.certis.features.transaction.query.model.MonthlyTransactionTotal

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

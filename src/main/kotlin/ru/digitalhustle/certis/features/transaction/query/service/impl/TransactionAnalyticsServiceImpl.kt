package ru.digitalhustle.certis.features.transaction.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.MoneyConstants
import ru.digitalhustle.certis.features.transaction.enums.CashFlowGranularity
import ru.digitalhustle.certis.features.transaction.enums.CashFlowRange
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowAnalytics
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowPoint
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowTotals
import ru.digitalhustle.certis.features.transaction.query.model.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.features.transaction.query.model.MonthlyTransactionAnalyticsFilter
import ru.digitalhustle.certis.features.transaction.query.repository.TransactionAnalyticsQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.TransactionAnalyticsService
import ru.digitalhustle.certis.util.time.ApplicationClock
import ru.digitalhustle.certis.util.time.nextMonthStart
import ru.digitalhustle.certis.util.time.nextWeekStart
import ru.digitalhustle.certis.util.time.startOfMonth
import ru.digitalhustle.certis.util.time.startOfWeek
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TransactionAnalyticsServiceImpl(
    private val transactionAnalyticsRepository: TransactionAnalyticsQueryRepository,
    private val applicationClock: ApplicationClock,
) : TransactionAnalyticsService {

    @Transactional(readOnly = true)
    override fun getMonthlyAnalytics(
        userId: UUID,
        filter: MonthlyTransactionAnalyticsFilter,
    ): MonthlyTransactionAnalytics =
        transactionAnalyticsRepository.findMonthlyByUserId(
            userId = userId,
            filter = filter,
            monthStart = applicationClock.startOfMonth(filter.month),
            nextMonthStart = applicationClock.startOfNextMonth(filter.month),
        )

    @Transactional(readOnly = true)
    override fun getCashFlowAnalytics(
        userId: UUID,
        filter: CashFlowAnalyticsFilter,
    ): CashFlowAnalytics {
        val period = cashFlowPeriod(filter)
        val existingPoints = transactionAnalyticsRepository.findCashFlowPointsByUserId(
            userId = userId,
            filter = filter,
            granularity = period.granularity,
            from = period.from.toOffsetDateTime(),
            toExclusive = period.toExclusive.toOffsetDateTime(),
        ).associateBy { point -> point.bucketStart.toInstant() }

        val points = generatePoints(period, existingPoints)
        val totalIncome = points.fold(ZERO_AMOUNT) { total, point -> total + point.income }
        val totalExpenses = points.fold(ZERO_AMOUNT) { total, point -> total + point.expenses }

        return CashFlowAnalytics(
            range = filter.range,
            currency = filter.currency,
            granularity = period.granularity,
            from = period.from.toOffsetDateTime(),
            toExclusive = period.toExclusive.toOffsetDateTime(),
            totals = CashFlowTotals(
                income = totalIncome,
                expenses = totalExpenses,
                netCashFlow = totalIncome - totalExpenses,
            ),
            points = points,
        )
    }

    private fun cashFlowPeriod(filter: CashFlowAnalyticsFilter): CashFlowPeriod {
        val startDate: LocalDate
        val endDate: LocalDate
        val granularity: CashFlowGranularity

        when (filter.range) {
            CashFlowRange.DAY -> {
                startDate = filter.anchorDate
                endDate = startDate.plusDays(1)
                granularity = CashFlowGranularity.HOUR
            }

            CashFlowRange.WEEK -> {
                startDate = filter.anchorDate.startOfWeek()
                endDate = startDate.nextWeekStart()
                granularity = CashFlowGranularity.DAY
            }

            CashFlowRange.MONTH -> {
                startDate = filter.anchorDate.startOfMonth()
                endDate = startDate.nextMonthStart()
                granularity = CashFlowGranularity.DAY
            }

            CashFlowRange.SIX_MONTHS -> {
                val currentMonthStart = filter.anchorDate.startOfMonth()

                startDate = currentMonthStart.minusMonths(SIX_MONTHS_OFFSET)
                endDate = currentMonthStart.nextMonthStart()
                granularity = CashFlowGranularity.MONTH
            }

            CashFlowRange.YEAR -> {
                val currentMonthStart = filter.anchorDate.startOfMonth()

                startDate = currentMonthStart.minusMonths(YEAR_OFFSET)
                endDate = currentMonthStart.nextMonthStart()
                granularity = CashFlowGranularity.MONTH
            }
        }

        return CashFlowPeriod(
            granularity = granularity,
            from = startDate.atStartOfDay(filter.timeZone),
            toExclusive = endDate.atStartOfDay(filter.timeZone),
        )
    }

    private fun generatePoints(
        period: CashFlowPeriod,
        existingPoints: Map<Instant, CashFlowPoint>,
    ): List<CashFlowPoint> =
        generateSequence(period.from) { current -> current.next(period.granularity) }
            .takeWhile { current -> current.isBefore(period.toExclusive) }
            .map { bucketStart ->
                existingPoints[bucketStart.toInstant()]
                    ?.copy(bucketStart = bucketStart.toOffsetDateTime())
                    ?: CashFlowPoint(
                        bucketStart = bucketStart.toOffsetDateTime(),
                        income = ZERO_AMOUNT,
                        expenses = ZERO_AMOUNT,
                        netCashFlow = ZERO_AMOUNT,
                    )
            }
            .toList()

    private fun ZonedDateTime.next(granularity: CashFlowGranularity): ZonedDateTime =
        when (granularity) {
            CashFlowGranularity.HOUR -> plusHours(1)
            CashFlowGranularity.DAY -> plusDays(1)
            CashFlowGranularity.MONTH -> plusMonths(1)
        }

    private data class CashFlowPeriod(
        val granularity: CashFlowGranularity,
        val from: ZonedDateTime,
        val toExclusive: ZonedDateTime,
    )

    private companion object {
        const val SIX_MONTHS_OFFSET = 5L
        const val YEAR_OFFSET = 11L
        val ZERO_AMOUNT: BigDecimal = BigDecimal.ZERO.setScale(MoneyConstants.MONEY_SCALE)
    }
}

package ru.digitalhustle.certis.units.provider

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.provider.RecurringTransactionScheduleProvider
import java.time.LocalDate

class RecurringTransactionScheduleProviderTest {

    private val scheduleProvider = RecurringTransactionScheduleProvider()

    @Test
    fun `should calculate daily and weekly dates using interval`() {
        // given
        val startDate = LocalDate.parse("2026-08-01")

        // when
        val daily = scheduleProvider.nextDate(
            lastRunDate = startDate,
            startDate = startDate,
            frequency = RecurringTransactionFrequency.DAILY,
            intervalCount = 3,
        )
        val weekly = scheduleProvider.nextDate(
            lastRunDate = startDate,
            startDate = startDate,
            frequency = RecurringTransactionFrequency.WEEKLY,
            intervalCount = 2,
        )

        // then
        assertThat(daily).isEqualTo(LocalDate.parse("2026-08-04"))
        assertThat(weekly).isEqualTo(LocalDate.parse("2026-08-15"))
    }

    @Test
    fun `should preserve monthly anchor after short month`() {
        // given
        val startDate = LocalDate.parse("2026-01-30")

        // when
        val february = scheduleProvider.nextDate(
            lastRunDate = startDate,
            startDate = startDate,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
        )
        val march = scheduleProvider.nextDate(
            lastRunDate = february,
            startDate = startDate,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
        )

        // then
        assertThat(february).isEqualTo(LocalDate.parse("2026-02-28"))
        assertThat(march).isEqualTo(LocalDate.parse("2026-03-30"))
    }

    @Test
    fun `should restore leap day on next leap year`() {
        // given
        val startDate = LocalDate.parse("2024-02-29")

        // when
        val nonLeapYear = scheduleProvider.nextDate(
            lastRunDate = startDate,
            startDate = startDate,
            frequency = RecurringTransactionFrequency.YEARLY,
            intervalCount = 1,
        )
        val nextLeapYear = scheduleProvider.nextDate(
            lastRunDate = nonLeapYear,
            startDate = startDate,
            frequency = RecurringTransactionFrequency.YEARLY,
            intervalCount = 3,
        )

        // then
        assertThat(nonLeapYear).isEqualTo(LocalDate.parse("2025-02-28"))
        assertThat(nextLeapYear).isEqualTo(LocalDate.parse("2028-02-29"))
    }
}

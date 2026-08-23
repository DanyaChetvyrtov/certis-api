package ru.digitalhustle.certis.provider

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

@Component
class RecurringTransactionScheduleProvider {

    fun nextDate(
        lastRunDate: LocalDate,
        startDate: LocalDate,
        frequency: RecurringTransactionFrequency,
        intervalCount: Short,
    ): LocalDate =
        when (frequency) {
            RecurringTransactionFrequency.DAILY -> lastRunDate.plusDays(intervalCount.toLong())
            RecurringTransactionFrequency.WEEKLY -> lastRunDate.plusWeeks(intervalCount.toLong())
            RecurringTransactionFrequency.MONTHLY -> nextMonthlyDate(lastRunDate, startDate, intervalCount)
            RecurringTransactionFrequency.YEARLY -> nextYearlyDate(lastRunDate, startDate, intervalCount)
        }

    private fun nextMonthlyDate(
        lastRunDate: LocalDate,
        startDate: LocalDate,
        intervalCount: Short,
    ): LocalDate {
        val targetMonth = YearMonth.from(lastRunDate).plusMonths(intervalCount.toLong())

        return targetMonth.atDay(min(startDate.dayOfMonth, targetMonth.lengthOfMonth()))
    }

    private fun nextYearlyDate(
        lastRunDate: LocalDate,
        startDate: LocalDate,
        intervalCount: Short,
    ): LocalDate {
        val targetMonth = YearMonth.of(
            lastRunDate.year + intervalCount,
            startDate.month,
        )

        return targetMonth.atDay(min(startDate.dayOfMonth, targetMonth.lengthOfMonth()))
    }
}

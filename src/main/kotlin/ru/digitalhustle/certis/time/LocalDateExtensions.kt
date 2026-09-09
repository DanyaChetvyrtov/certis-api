package ru.digitalhustle.certis.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun LocalDate.startOfWeek(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun LocalDate.startOfMonth(): LocalDate =
    withDayOfMonth(1)

fun LocalDate.nextWeekStart(): LocalDate =
    startOfWeek().plusWeeks(1)

fun LocalDate.nextMonthStart(): LocalDate =
    startOfMonth().plusMonths(1)

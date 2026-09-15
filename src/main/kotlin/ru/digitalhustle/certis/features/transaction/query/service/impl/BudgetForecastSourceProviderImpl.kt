package ru.digitalhustle.certis.features.transaction.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.spi.ActualForecastOperation
import ru.digitalhustle.certis.features.budget.spi.BudgetForecastSourceProvider
import ru.digitalhustle.certis.features.budget.spi.BudgetForecastSourceSet
import ru.digitalhustle.certis.features.budget.spi.ForecastSourceCategory
import ru.digitalhustle.certis.features.budget.spi.ForecastSourceFrequency
import ru.digitalhustle.certis.features.budget.spi.ForecastSourceOperationType
import ru.digitalhustle.certis.features.budget.spi.HistoricalCategorySpending
import ru.digitalhustle.certis.features.budget.spi.ProjectedRecurringOccurrence
import ru.digitalhustle.certis.features.transaction.query.model.ForecastActualTransaction
import ru.digitalhustle.certis.features.transaction.query.model.ForecastHistoricalCategoryAmount
import ru.digitalhustle.certis.features.transaction.query.model.ForecastRecurringTemplate
import ru.digitalhustle.certis.features.transaction.query.model.ForecastTransactionCategory
import ru.digitalhustle.certis.features.transaction.query.repository.BudgetForecastSourceQueryRepository
import ru.digitalhustle.certis.scheduler.RecurringTransactionScheduleProvider
import ru.digitalhustle.certis.shared.enums.Currency
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BudgetForecastSourceProviderImpl(
    private val repository: BudgetForecastSourceQueryRepository,
    private val scheduleProvider: RecurringTransactionScheduleProvider,
    private val applicationClock: ApplicationClock,
) : BudgetForecastSourceProvider {

    override fun load(
        userId: UUID,
        currency: Currency,
        targetMonth: YearMonth,
        historyMonths: Int,
    ): BudgetForecastSourceSet {
        val historyStart = targetMonth.minusMonths(historyMonths.toLong())
        val historicalAmounts = repository.findHistoricalCategoryAmounts(
            userId = userId,
            currency = currency,
            from = applicationClock.startOfMonth(historyStart),
            toExclusive = applicationClock.startOfMonth(targetMonth),
            zoneId = applicationClock.zoneId(),
        )
        val historyFrom = historicalAmounts.minOfOrNull(ForecastHistoricalCategoryAmount::month)
            ?: targetMonth.minusMonths(1)
        val monthsUsed = if (historicalAmounts.isEmpty()) {
            0
        } else {
            ChronoUnit.MONTHS.between(historyFrom, targetMonth).toInt()
        }

        return BudgetForecastSourceSet(
            recurringOccurrences = projectRecurring(userId, currency, targetMonth),
            actualOperations = findActualOperations(userId, currency, targetMonth),
            historicalCategories = historicalAmounts.groupBy { it.type to it.category.id }
                .values
                .map(::toHistoricalCategory),
            historyFromMonth = historyFrom,
            historyToMonth = targetMonth.minusMonths(1),
            historyMonthsUsed = monthsUsed,
        )
    }

    private fun projectRecurring(
        userId: UUID,
        currency: Currency,
        targetMonth: YearMonth,
    ): List<ProjectedRecurringOccurrence> {
        val from = targetMonth.atDay(1)
        val toExclusive = targetMonth.plusMonths(1).atDay(1)

        return repository.findRecurringTemplates(userId, currency)
            .flatMap { template -> project(template, from, toExclusive) }
            .sortedWith(compareBy(ProjectedRecurringOccurrence::scheduledFor, ProjectedRecurringOccurrence::templateId))
    }

    private fun project(
        template: ForecastRecurringTemplate,
        from: LocalDate,
        toExclusive: LocalDate,
    ): List<ProjectedRecurringOccurrence> {
        val occurrences = mutableListOf<ProjectedRecurringOccurrence>()
        var scheduledFor = template.nextRunDate
        while (scheduledFor < from) {
            scheduledFor = nextDate(template, scheduledFor)
        }
        while (scheduledFor < toExclusive && (template.endDate == null || scheduledFor <= template.endDate)) {
            occurrences += ProjectedRecurringOccurrence(
                templateId = template.id,
                sourceUpdatedAt = template.updatedAt,
                operationType = ForecastSourceOperationType.valueOf(template.type.name),
                title = template.name,
                category = template.category?.toSourceCategory(),
                scheduledFor = scheduledFor,
                amount = template.amount,
                frequency = ForecastSourceFrequency.valueOf(template.frequency.name),
            )
            scheduledFor = nextDate(template, scheduledFor)
        }
        return occurrences
    }

    private fun nextDate(template: ForecastRecurringTemplate, scheduledFor: LocalDate): LocalDate =
        scheduleProvider.nextDate(
            lastRunDate = scheduledFor,
            startDate = template.startDate,
            frequency = template.frequency,
            intervalCount = template.intervalCount,
        )

    private fun findActualOperations(
        userId: UUID,
        currency: Currency,
        targetMonth: YearMonth,
    ): List<ActualForecastOperation> {
        if (targetMonth > applicationClock.currentMonth()) {
            return emptyList()
        }

        return repository.findActualOperations(
            userId = userId,
            currency = currency,
            from = applicationClock.startOfMonth(targetMonth),
            toExclusive = applicationClock.startOfNextMonth(targetMonth),
            zoneId = applicationClock.zoneId(),
        ).map { transaction -> transaction.toSourceOperation() }
    }

    private fun toHistoricalCategory(items: List<ForecastHistoricalCategoryAmount>): HistoricalCategorySpending {
        val first = items.first()
        return HistoricalCategorySpending(
            operationType = ForecastSourceOperationType.valueOf(first.type.name),
            category = first.category.toSourceCategory(),
            monthlyAmounts = items.sortedBy(ForecastHistoricalCategoryAmount::month)
                .associate { item -> item.month to item.amount },
        )
    }

    private fun ForecastActualTransaction.toSourceOperation(): ActualForecastOperation =
        ActualForecastOperation(
            transactionId = id,
            operationType = ForecastSourceOperationType.valueOf(type.name),
            title = title,
            category = category?.toSourceCategory(),
            occurredOn = occurredOn,
            amount = amount,
            recurringTemplateId = recurringTemplateId,
        )

    private fun ForecastTransactionCategory.toSourceCategory(): ForecastSourceCategory =
        ForecastSourceCategory(id, name, icon, color)
}

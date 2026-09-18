package ru.digitalhustle.certis.features.category.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingBucket
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingCategory
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTime
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTimeFilter
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingPoint
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingSeriesData
import ru.digitalhustle.certis.features.category.query.repository.CategorySpendingOverTimeQueryRepository
import ru.digitalhustle.certis.features.category.query.service.CategorySpendingOverTimeService
import ru.digitalhustle.certis.shared.constants.MoneyConstants
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CategorySpendingOverTimeServiceImpl(
    private val categorySpendingRepository: CategorySpendingOverTimeQueryRepository,
    private val applicationClock: ApplicationClock,
) : CategorySpendingOverTimeService {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getSpendingOverTime(
        userId: UUID,
        filter: CategorySpendingOverTimeFilter,
    ): CategorySpendingOverTime {
        val firstMonth = filter.month.minusMonths(filter.bucketCount.toLong() - 1)
        val queryResult = categorySpendingRepository.findByUserId(
            userId = userId,
            filter = filter,
            from = applicationClock.startOfMonth(firstMonth),
            toExclusive = applicationClock.startOfNextMonth(filter.month),
            timeZone = applicationClock.zoneId(),
        )
        val months = generateSequence(firstMonth) { month -> month.plusMonths(1) }
            .take(filter.bucketCount)
            .toList()
        val buckets = queryResult.buckets.associateBy { bucket ->
            CategoryMonth(bucket.categoryId, bucket.bucketMonth)
        }
        val topSeries = queryResult.topCategories.map { category ->
            categorySeries(category, months, buckets)
        }
        val otherSeries = otherSeries(months, buckets)
        val series = if (otherSeries == null) topSeries else topSeries + otherSeries

        return CategorySpendingOverTime(
            month = filter.month,
            currency = filter.currency,
            type = filter.type,
            totalSum = series.fold(ZERO_AMOUNT) { total, item -> total + item.total },
            series = series,
        )
    }

    private fun categorySeries(
        category: CategorySpendingCategory,
        months: List<YearMonth>,
        buckets: Map<CategoryMonth, CategorySpendingBucket>,
    ): CategorySpendingSeriesData {
        val points = points(category.categoryId, months, buckets)

        return CategorySpendingSeriesData(
            categoryId = category.categoryId,
            categoryName = category.categoryName,
            categoryColor = category.categoryColor,
            total = points.total(),
            points = points,
        )
    }

    private fun otherSeries(
        months: List<YearMonth>,
        buckets: Map<CategoryMonth, CategorySpendingBucket>,
    ): CategorySpendingSeriesData? {
        val points = points(null, months, buckets)
        val total = points.total()

        return if (total.signum() == 0) {
            null
        } else {
            CategorySpendingSeriesData(
                categoryId = null,
                categoryName = OTHER_CATEGORY_NAME,
                categoryColor = null,
                total = total,
                points = points,
            )
        }
    }

    private fun points(
        categoryId: UUID?,
        months: List<YearMonth>,
        buckets: Map<CategoryMonth, CategorySpendingBucket>,
    ): List<CategorySpendingPoint> =
        months.map { month ->
            CategorySpendingPoint(
                bucketMonth = month,
                amount = buckets[CategoryMonth(categoryId, month)]?.amount ?: ZERO_AMOUNT,
            )
        }

    private fun List<CategorySpendingPoint>.total(): BigDecimal =
        fold(ZERO_AMOUNT) { total, point -> total + point.amount }

    private data class CategoryMonth(
        val categoryId: UUID?,
        val month: YearMonth,
    )

    private companion object {
        const val OTHER_CATEGORY_NAME = "Other"
        val ZERO_AMOUNT: BigDecimal = BigDecimal.ZERO.setScale(MoneyConstants.MONEY_SCALE)
    }
}

package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingBucket
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingCategory
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTimeFilter
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingPoint
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingQueryResult
import ru.digitalhustle.certis.features.category.query.repository.CategorySpendingOverTimeQueryRepository
import ru.digitalhustle.certis.features.category.query.service.impl.CategorySpendingOverTimeServiceImpl
import ru.digitalhustle.certis.shared.enums.Currency
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class CategorySpendingOverTimeServiceImplTest {

    private val repository = mock(CategorySpendingOverTimeQueryRepository::class.java)
    private val zoneId = ZoneId.of("Europe/Riga")
    private val applicationClock = ApplicationClock(
        Clock.fixed(Instant.parse("2026-09-15T10:15:30Z"), zoneId),
    )
    private val service = CategorySpendingOverTimeServiceImpl(repository, applicationClock)

    @Test
    fun `should build ordered zero filled series and aggregate other`() {
        // given
        val userId = UUID.randomUUID()
        val foodId = UUID.randomUUID()
        val housingId = UUID.randomUUID()
        val filter = CategorySpendingOverTimeFilter(
            month = SEPTEMBER,
            currency = Currency.RUB,
            type = CategoryType.EXPENSE,
            bucketCount = 6,
            topLimit = 2,
        )
        val queryResult = queryResult(foodId, housingId)
        `when`(
            repository.findByUserId(
                userId = userId,
                filter = filter,
                from = OffsetDateTime.parse("2026-04-01T00:00:00+03:00"),
                toExclusive = OffsetDateTime.parse("2026-10-01T00:00:00+03:00"),
                timeZone = zoneId,
            ),
        ).thenReturn(queryResult)

        // when
        val result = service.getSpendingOverTime(userId, filter)

        // then
        assertThat(result.totalSum).isEqualByComparingTo("350.00")
        assertThat(result.series.map { series -> series.categoryName })
            .containsExactly("Food", "Housing", "Other")
        assertFoodSeries(result.series[0].points)
        assertThat(result.series[2].categoryId).isNull()
        assertThat(result.series[2].categoryColor).isNull()
        assertThat(result.series[2].total).isEqualByComparingTo("80.00")
        assertOtherPoints(result.series[2].points)
    }

    private fun queryResult(
        foodId: UUID,
        housingId: UUID,
    ): CategorySpendingQueryResult =
        CategorySpendingQueryResult(
            topCategories = listOf(
                CategorySpendingCategory(foodId, "Food", "#E6655A"),
                CategorySpendingCategory(housingId, "Housing", "#B08D57"),
            ),
            buckets = listOf(
                CategorySpendingBucket(foodId, APRIL, amount("60.00")),
                CategorySpendingBucket(housingId, MAY, amount("120.00")),
                CategorySpendingBucket(null, JUNE, amount("50.00")),
                CategorySpendingBucket(null, JULY, amount("30.00")),
                CategorySpendingBucket(foodId, SEPTEMBER, amount("90.00")),
            ),
        )

    private fun assertFoodSeries(points: List<CategorySpendingPoint>) {
        assertThat(points.map { point -> point.bucketMonth })
            .containsExactly(APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER)
        assertThat(points.map { point -> point.amount })
            .containsExactlyElementsOf(
                listOf("60.00", "0.00", "0.00", "0.00", "0.00", "90.00").map(::amount),
            )
    }

    private fun assertOtherPoints(points: List<CategorySpendingPoint>) {
        assertThat(points.map { point -> point.amount })
            .containsExactlyElementsOf(
                listOf("0.00", "0.00", "50.00", "30.00", "0.00", "0.00").map(::amount),
            )
    }

    private fun amount(value: String): BigDecimal = BigDecimal(value)

    private companion object {
        val APRIL: YearMonth = YearMonth.of(2026, 4)
        val MAY: YearMonth = YearMonth.of(2026, 5)
        val JUNE: YearMonth = YearMonth.of(2026, 6)
        val JULY: YearMonth = YearMonth.of(2026, 7)
        val AUGUST: YearMonth = YearMonth.of(2026, 8)
        val SEPTEMBER: YearMonth = YearMonth.of(2026, 9)
    }
}

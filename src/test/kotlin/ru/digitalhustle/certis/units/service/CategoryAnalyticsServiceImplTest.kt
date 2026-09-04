package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.repository.CategoryAnalyticsRepository
import ru.digitalhustle.certis.service.domain.impl.CategoryAnalyticsServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class CategoryAnalyticsServiceImplTest {

    private val categoryAnalyticsRepository = mock(CategoryAnalyticsRepository::class.java)
    private val clock = Clock.fixed(
        Instant.parse("2026-09-15T10:15:30Z"),
        ZoneId.of("Europe/Riga"),
    )
    private val categoryAnalyticsService = CategoryAnalyticsServiceImpl(categoryAnalyticsRepository, clock)

    private companion object {
        private val MONTH = YearMonth.of(2026, 9)
        private val MONTH_START = OffsetDateTime.parse("2026-09-01T00:00:00+03:00")
        private val NEXT_MONTH_START = OffsetDateTime.parse("2026-10-01T00:00:00+03:00")
    }

    @Test
    fun `should get analytics using application time zone boundaries`() {
        // given
        val userId = UUID.randomUUID()
        val filter = CategoryAnalyticsFilter(MONTH, Currency.RUB, 4, CategoryType.EXPENSE)
        val analytics = CategoryAnalytics(
            month = MONTH,
            currency = Currency.RUB,
            type = CategoryType.EXPENSE,
            totalTransactionCount = 0,
            categorizedTransactionCount = 0,
            uncategorizedTransactionCount = 0,
            totalSum = BigDecimal.ZERO,
            categorizedSum = BigDecimal.ZERO,
            uncategorizedSum = BigDecimal.ZERO,
            coveragePercentage = null,
            topExpenseCategories = emptyList(),
        )

        `when`(
            categoryAnalyticsRepository.findByUserId(
                userId,
                filter,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(analytics)

        // when
        val result = categoryAnalyticsService.getAnalytics(userId, filter)

        // then
        assertThat(result).isEqualTo(analytics)
    }
}

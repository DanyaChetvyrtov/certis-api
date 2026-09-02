package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.CategoryCardSort
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import ru.digitalhustle.certis.repository.CategoryCardRepository
import ru.digitalhustle.certis.service.domain.impl.CategoryCardServiceImpl
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class CategoryCardServiceImplTest {

    private val categoryCardRepository = mock(CategoryCardRepository::class.java)
    private val clock = Clock.fixed(
        Instant.parse("2026-09-15T10:15:30Z"),
        ZoneId.of("Europe/Riga"),
    )
    private val categoryCardService = CategoryCardServiceImpl(categoryCardRepository, clock)

    private companion object {
        private val MONTH = YearMonth.of(2026, 9)
        private val MONTH_START = OffsetDateTime.parse("2026-09-01T00:00:00+03:00")
        private val NEXT_MONTH_START = OffsetDateTime.parse("2026-10-01T00:00:00+03:00")
    }

    @Test
    fun `should get category cards using application time zone boundaries`() {
        // given
        val userId = UUID.randomUUID()
        val filter = CategoryCardFilter(MONTH, Currency.RUB, 1, 10, CategoryCardSort.NAME)
        val cards = CategoryCards(MONTH, Currency.RUB, emptyList(), 1, 10, 15)

        `when`(
            categoryCardRepository.findByUserId(
                userId,
                filter,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(cards)

        // when
        val result = categoryCardService.getCards(userId, filter)

        // then
        assertThat(result).isEqualTo(cards)
    }
}

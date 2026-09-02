package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import ru.digitalhustle.certis.repository.UncategorizedTransactionRepository
import ru.digitalhustle.certis.service.domain.impl.UncategorizedTransactionServiceImpl
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class UncategorizedTransactionServiceImplTest {

    private val repository = mock(UncategorizedTransactionRepository::class.java)
    private val clock = Clock.fixed(
        Instant.parse("2026-09-15T10:15:30Z"),
        ZoneId.of("Europe/Riga"),
    )
    private val service = UncategorizedTransactionServiceImpl(repository, clock)

    @Test
    fun `should get transactions using application time zone boundaries`() {
        // given
        val userId = UUID.randomUUID()
        val filter = UncategorizedTransactionFilter(
            month = YearMonth.of(2026, 9),
            currency = Currency.RUB,
            type = TransactionType.EXPENSE,
            accountId = null,
            search = null,
            page = 0,
            size = 20,
        )
        val page = UncategorizedTransactionPage(
            month = filter.month,
            currency = filter.currency,
            type = filter.type,
            items = emptyList(),
            page = filter.page,
            size = filter.size,
            totalElements = 0,
        )
        val monthStart = OffsetDateTime.parse("2026-09-01T00:00:00+03:00")
        val nextMonthStart = OffsetDateTime.parse("2026-10-01T00:00:00+03:00")

        `when`(repository.findByUserId(userId, filter, monthStart, nextMonthStart))
            .thenReturn(page)

        // when
        val result = service.getAllByUserId(userId, filter)

        // then
        assertThat(result).isEqualTo(page)
        verify(repository).findByUserId(userId, filter, monthStart, nextMonthStart)
    }
}

package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import ru.digitalhustle.certis.repository.UncategorizedTransactionRepository
import ru.digitalhustle.certis.service.domain.UncategorizedTransactionService
import java.time.Clock
import java.util.UUID

@Service
class UncategorizedTransactionServiceImpl(
    private val uncategorizedTransactionRepository: UncategorizedTransactionRepository,
    private val clock: Clock,
) : UncategorizedTransactionService {

    override fun getAllByUserId(
        userId: UUID,
        filter: UncategorizedTransactionFilter,
    ): UncategorizedTransactionPage {
        val monthStart = filter.month
            .atDay(1)
            .atStartOfDay(clock.zone)
            .toOffsetDateTime()

        val nextMonthStart = filter.month
            .plusMonths(1)
            .atDay(1)
            .atStartOfDay(clock.zone)
            .toOffsetDateTime()

        return uncategorizedTransactionRepository.findByUserId(
            userId = userId,
            filter = filter,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        )
    }
}

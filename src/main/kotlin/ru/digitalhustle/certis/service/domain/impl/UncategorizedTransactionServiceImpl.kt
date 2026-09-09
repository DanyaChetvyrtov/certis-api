package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import ru.digitalhustle.certis.repository.UncategorizedTransactionRepository
import ru.digitalhustle.certis.service.domain.UncategorizedTransactionService
import ru.digitalhustle.certis.time.ApplicationClock
import java.util.UUID

@Service
class UncategorizedTransactionServiceImpl(
    private val uncategorizedTransactionRepository: UncategorizedTransactionRepository,
    private val applicationClock: ApplicationClock,
) : UncategorizedTransactionService {

    override fun getAllByUserId(
        userId: UUID,
        filter: UncategorizedTransactionFilter,
    ): UncategorizedTransactionPage {
        val monthStart = applicationClock.startOfMonth(filter.month)
        val nextMonthStart = applicationClock.startOfNextMonth(filter.month)

        return uncategorizedTransactionRepository.findByUserId(
            userId = userId,
            filter = filter,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        )
    }
}

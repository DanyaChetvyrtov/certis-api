package ru.digitalhustle.certis.features.transaction.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.transaction.query.model.UncategorizedTransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.UncategorizedTransactionPage
import ru.digitalhustle.certis.features.transaction.query.repository.UncategorizedTransactionQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.UncategorizedTransactionService
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class UncategorizedTransactionServiceImpl(
    private val uncategorizedTransactionRepository: UncategorizedTransactionQueryRepository,
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

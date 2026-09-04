package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.repository.CategoryAnalyticsRepository
import ru.digitalhustle.certis.service.domain.CategoryAnalyticsService
import java.time.Clock
import java.util.UUID

@Service
class CategoryAnalyticsServiceImpl(
    private val categoryAnalyticsRepository: CategoryAnalyticsRepository,
    private val clock: Clock,
) : CategoryAnalyticsService {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getAnalytics(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
    ): CategoryAnalytics {
        val monthStart = filter.month.atDay(1).atStartOfDay(clock.zone).toOffsetDateTime()
        val nextMonthStart = filter.month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toOffsetDateTime()

        return categoryAnalyticsRepository.findByUserId(
            userId = userId,
            filter = filter,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        )
    }
}

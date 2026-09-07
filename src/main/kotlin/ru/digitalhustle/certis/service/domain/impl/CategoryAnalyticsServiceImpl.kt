package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.repository.CategoryAnalyticsRepository
import ru.digitalhustle.certis.service.domain.CategoryAnalyticsService
import ru.digitalhustle.certis.time.ApplicationClock
import java.util.UUID

@Service
class CategoryAnalyticsServiceImpl(
    private val categoryAnalyticsRepository: CategoryAnalyticsRepository,
    private val applicationClock: ApplicationClock,
) : CategoryAnalyticsService {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getAnalytics(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
    ): CategoryAnalytics =
        categoryAnalyticsRepository.findByUserId(
            userId = userId,
            filter = filter,
            monthStart = applicationClock.startOfMonth(filter.month),
            nextMonthStart = applicationClock.startOfNextMonth(filter.month),
        )
}

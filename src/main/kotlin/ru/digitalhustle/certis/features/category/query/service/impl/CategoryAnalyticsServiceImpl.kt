package ru.digitalhustle.certis.features.category.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.query.model.CategoryAnalytics
import ru.digitalhustle.certis.features.category.query.model.CategoryAnalyticsFilter
import ru.digitalhustle.certis.features.category.query.repository.CategoryAnalyticsQueryRepository
import ru.digitalhustle.certis.features.category.query.service.CategoryAnalyticsService
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CategoryAnalyticsServiceImpl(
    private val categoryAnalyticsRepository: CategoryAnalyticsQueryRepository,
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

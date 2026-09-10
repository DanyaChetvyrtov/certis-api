package ru.digitalhustle.certis.features.category.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.query.repository.CategoryUsageQueryRepository
import ru.digitalhustle.certis.features.category.query.service.CategoryUsageQueryService
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CategoryUsageQueryServiceImpl(
    private val categoryUsageRepository: CategoryUsageQueryRepository,
    private val applicationClock: ApplicationClock,
) : CategoryUsageQueryService {

    override fun isRequired(
        categoryId: UUID,
        userId: UUID,
    ): Boolean =
        categoryUsageRepository.existsInSchedulableRecurringTemplate(categoryId, userId) ||
            categoryUsageRepository.existsInCurrentOrFutureBudget(
                categoryId = categoryId,
                userId = userId,
                currentMonth = applicationClock.currentMonth().atDay(1),
            )
}

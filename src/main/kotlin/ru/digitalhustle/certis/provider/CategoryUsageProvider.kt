package ru.digitalhustle.certis.provider

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.repository.CategoryUsageRepository
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Component
class CategoryUsageProvider(
    private val categoryUsageRepository: CategoryUsageRepository,
    private val clock: Clock,
) {

    fun isRequired(
        categoryId: UUID,
        userId: UUID,
    ): Boolean =
        categoryUsageRepository.existsInSchedulableRecurringTemplate(categoryId, userId) ||
            categoryUsageRepository.existsInCurrentOrFutureBudget(
                categoryId = categoryId,
                userId = userId,
                currentMonth = LocalDate.now(clock).withDayOfMonth(1),
            )
}

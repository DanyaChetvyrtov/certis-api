package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import ru.digitalhustle.certis.repository.CategoryCardRepository
import ru.digitalhustle.certis.service.domain.CategoryCardService
import ru.digitalhustle.certis.time.ApplicationClock
import java.util.UUID

@Service
class CategoryCardServiceImpl(
    private val categoryCardRepository: CategoryCardRepository,
    private val applicationClock: ApplicationClock,
) : CategoryCardService {

    override fun getCards(
        userId: UUID,
        filter: CategoryCardFilter,
    ): CategoryCards =
        categoryCardRepository.findByUserId(
            userId = userId,
            filter = filter,
            monthStart = applicationClock.startOfMonth(filter.month),
            nextMonthStart = applicationClock.startOfNextMonth(filter.month),
        )
}

package ru.digitalhustle.certis.features.category.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.query.model.CategoryCardFilter
import ru.digitalhustle.certis.features.category.query.model.CategoryCards
import ru.digitalhustle.certis.features.category.query.repository.CategoryCardQueryRepository
import ru.digitalhustle.certis.features.category.query.service.CategoryCardService
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CategoryCardServiceImpl(
    private val categoryCardRepository: CategoryCardQueryRepository,
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

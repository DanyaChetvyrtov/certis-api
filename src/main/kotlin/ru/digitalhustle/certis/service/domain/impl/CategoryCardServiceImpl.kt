package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import ru.digitalhustle.certis.repository.CategoryCardRepository
import ru.digitalhustle.certis.service.domain.CategoryCardService
import java.time.Clock
import java.util.UUID

@Service
class CategoryCardServiceImpl(
    private val categoryCardRepository: CategoryCardRepository,
    private val clock: Clock,
) : CategoryCardService {

    override fun getCards(
        userId: UUID,
        filter: CategoryCardFilter,
    ): CategoryCards {
        val monthStart = filter.month.atDay(1).atStartOfDay(clock.zone).toOffsetDateTime()
        val nextMonthStart = filter.month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toOffsetDateTime()

        return categoryCardRepository.findByUserId(
            userId = userId,
            filter = filter,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        )
    }
}

package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import java.util.UUID

interface CategoryCardService {

    fun getCards(userId: UUID, filter: CategoryCardFilter): CategoryCards
}

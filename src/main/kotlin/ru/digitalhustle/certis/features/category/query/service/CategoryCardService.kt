package ru.digitalhustle.certis.features.category.query.service

import ru.digitalhustle.certis.features.category.query.model.CategoryCardFilter
import ru.digitalhustle.certis.features.category.query.model.CategoryCards
import java.util.UUID

interface CategoryCardService {

    fun getCards(userId: UUID, filter: CategoryCardFilter): CategoryCards
}

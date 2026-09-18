package ru.digitalhustle.certis.features.category.query.service

import java.util.UUID

interface CategoryUsageQueryService {

    fun isRequired(categoryId: UUID, userId: UUID): Boolean
}

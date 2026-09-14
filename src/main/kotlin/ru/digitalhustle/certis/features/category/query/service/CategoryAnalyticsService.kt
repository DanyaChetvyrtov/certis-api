package ru.digitalhustle.certis.features.category.query.service

import ru.digitalhustle.certis.features.category.query.model.CategoryAnalytics
import ru.digitalhustle.certis.features.category.query.model.CategoryAnalyticsFilter
import java.util.UUID

interface CategoryAnalyticsService {

    fun getAnalytics(userId: UUID, filter: CategoryAnalyticsFilter): CategoryAnalytics
}

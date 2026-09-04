package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import java.util.UUID

interface CategoryAnalyticsService {

    fun getAnalytics(userId: UUID, filter: CategoryAnalyticsFilter): CategoryAnalytics
}

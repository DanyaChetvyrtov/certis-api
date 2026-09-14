package ru.digitalhustle.certis.features.category.query.service

import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTime
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTimeFilter
import java.util.UUID

interface CategorySpendingOverTimeService {

    fun getSpendingOverTime(
        userId: UUID,
        filter: CategorySpendingOverTimeFilter,
    ): CategorySpendingOverTime
}

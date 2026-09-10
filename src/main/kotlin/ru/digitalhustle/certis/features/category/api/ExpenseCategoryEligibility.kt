package ru.digitalhustle.certis.features.category.api

import java.util.UUID

fun interface ExpenseCategoryEligibility {

    fun areAllActive(userId: UUID, categoryIds: Collection<UUID>): Boolean
}

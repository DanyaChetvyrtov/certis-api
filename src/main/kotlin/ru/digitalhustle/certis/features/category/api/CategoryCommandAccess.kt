package ru.digitalhustle.certis.features.category.api

import java.util.UUID

interface CategoryCommandAccess {

    fun getByIdForShare(id: UUID, userId: UUID): CategorySnapshot

    fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<CategorySnapshot>
}

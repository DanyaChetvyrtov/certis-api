package ru.digitalhustle.certis.features.category.api

import java.util.UUID

fun interface DefaultCategoryProvisioner {

    fun createDefaults(userId: UUID)
}

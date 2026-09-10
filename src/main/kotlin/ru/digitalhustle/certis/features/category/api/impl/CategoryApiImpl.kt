package ru.digitalhustle.certis.features.category.api.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.api.CategoryCommandAccess
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.features.category.api.DefaultCategoryProvisioner
import ru.digitalhustle.certis.features.category.api.ExpenseCategoryEligibility
import ru.digitalhustle.certis.features.category.command.service.CategoryService
import ru.digitalhustle.certis.features.category.model.Category
import java.util.UUID

@Service
class CategoryApiImpl(
    private val categoryService: CategoryService,
) : CategoryCommandAccess,
    DefaultCategoryProvisioner,
    ExpenseCategoryEligibility {

    @Transactional(propagation = Propagation.MANDATORY)
    override fun getByIdForShare(id: UUID, userId: UUID): CategorySnapshot =
        categoryService.getByIdForShare(id, userId).toSnapshot()

    @Transactional(propagation = Propagation.MANDATORY)
    override fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<CategorySnapshot> =
        categoryService.getAllByIdsForShare(ids, userId).map { it.toSnapshot() }

    override fun createDefaults(userId: UUID): Unit = categoryService.createDefaults(userId)

    override fun areAllActive(userId: UUID, categoryIds: Collection<UUID>): Boolean {
        val uniqueCategoryIds = categoryIds.toSet()
        return categoryService.countActiveExpenseCategories(userId, uniqueCategoryIds) == uniqueCategoryIds.size
    }

    private fun Category.toSnapshot(): CategorySnapshot =
        CategorySnapshot(
            id = id,
            type = type,
            archivedAt = archivedAt,
        )
}

package ru.digitalhustle.certis.features.category.command.service.impl

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.category.command.model.NewCategory
import ru.digitalhustle.certis.features.category.command.model.UpdateCategoryData
import ru.digitalhustle.certis.features.category.command.repository.CategoryRepository
import ru.digitalhustle.certis.features.category.command.service.CategoryService
import ru.digitalhustle.certis.features.category.command.util.CategoryNormalizer
import ru.digitalhustle.certis.features.category.command.util.DefaultCategoryProvider
import ru.digitalhustle.certis.features.category.exceptions.CategoryArchivedException
import ru.digitalhustle.certis.features.category.model.Category
import ru.digitalhustle.certis.features.category.model.CategoryPreview
import ru.digitalhustle.certis.features.category.model.toPreview
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository,
    private val defaultCategoryProvider: DefaultCategoryProvider,
    private val applicationClock: ApplicationClock,
) : CategoryService {

    override fun getByIdForShare(
        id: UUID,
        userId: UUID,
    ): Category =
        categoryRepository.findByIdAndUserIdForShare(id, userId)
            ?: throw NotFoundException.entity("Category")

    override fun getAllByIdsForShare(
        ids: Collection<UUID>,
        userId: UUID,
    ): List<Category> {
        val categories = categoryRepository.findAllByIdsAndUserIdForShare(ids, userId)

        if (categories.size != ids.toSet().size) {
            throw NotFoundException.entity("Category")
        }

        return categories
    }

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Category =
        categoryRepository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw NotFoundException.entity("Category")

    override fun save(category: NewCategory): CategoryPreview = translateNameConflict {
        categoryRepository.insert(CategoryNormalizer.normalize(category).toEntity()).toPreview()
    }

    override fun createDefaults(userId: UUID): Unit = translateNameConflict {
        categoryRepository.insertAll(
            defaultCategoryProvider.getByUserId(userId)
                .map { category -> category.toEntity() },
        )
    }

    override fun update(category: UpdateCategoryData): CategoryPreview = translateNameConflict {
        val updatedCategory = categoryRepository.updateActive(CategoryNormalizer.normalize(category))
            ?: throwUpdateFailure(category.id, category.userId)

        updatedCategory.toPreview()
    }

    override fun restore(
        id: UUID,
        userId: UUID,
    ): Unit = translateNameConflict {
        val restored = categoryRepository.restore(id, userId)

        if (!restored) {
            getCategory(id, userId)
        }
    }

    override fun archive(
        id: UUID,
        userId: UUID,
    ) {
        val archived = categoryRepository.archive(
            id = id,
            userId = userId,
            archivedAt = applicationClock.now(),
        )

        if (!archived) {
            getCategory(id, userId)
        }
    }

    private fun getCategory(id: UUID, userId: UUID): Category =
        categoryRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Category")

    private fun throwUpdateFailure(
        id: UUID,
        userId: UUID,
    ): Nothing {
        val category = getCategory(id, userId)

        if (category.archivedAt != null) {
            throw CategoryArchivedException(ErrorMessages.CATEGORY_ARCHIVED)
        }

        throw NotFoundException.entity("Category")
    }

    private fun <T> translateNameConflict(action: () -> T): T =
        try {
            action()
        } catch (_: DuplicateKeyException) {
            throw EntityAlreadyExistsException.entity("Category", "name")
        }

    private fun NewCategory.toEntity(): Category =
        Category(
            id = UUID.randomUUID(),
            userId = userId,
            name = name,
            type = type,
            icon = icon,
            color = color,
            archivedAt = null,
        )

    override fun countActiveExpenseCategories(userId: UUID, categoryIds: Collection<UUID>): Int =
        categoryRepository.countActiveExpenseCategories(userId, categoryIds)
}

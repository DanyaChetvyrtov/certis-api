package ru.digitalhustle.certis.service.domain.impl

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.CategoryArchivedException
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.mapper.toPreview
import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.repository.CategoryRepository
import ru.digitalhustle.certis.service.domain.CategoryService
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository,
    private val clock: Clock,
) : CategoryService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): CategoryPreview = getCategory(id, userId).toPreview()

    override fun getAllByUserId(userId: UUID): List<CategoryPreview> =
        categoryRepository.findAllByUserId(userId).map(Category::toPreview)

    override fun save(category: NewCategory): CategoryPreview = translateNameConflict {
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = category.userId,
                name = category.name,
                type = category.type,
                icon = category.icon,
                color = category.color,
                archivedAt = null,
            ),
        ).toPreview()
    }

    override fun update(category: UpdateCategoryData): CategoryPreview = translateNameConflict {
        val updatedCategory = categoryRepository.updateActive(category)
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
            archivedAt = OffsetDateTime.now(clock),
        )

        if (!archived) {
            getCategory(id, userId)
        }
    }

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

    private fun getCategory(id: UUID, userId: UUID): Category =
        categoryRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Category")

    private fun <T> translateNameConflict(action: () -> T): T =
        try {
            action()
        } catch (_: DuplicateKeyException) {
            throw EntityAlreadyExistsException.entity("Category", "name")
        }
}

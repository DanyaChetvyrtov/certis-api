package ru.digitalhustle.certis.features.category.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.application.service.CategoryApplicationService
import ru.digitalhustle.certis.features.category.command.model.NewCategory
import ru.digitalhustle.certis.features.category.command.model.UpdateCategoryData
import ru.digitalhustle.certis.features.category.command.service.CategoryService
import ru.digitalhustle.certis.features.category.model.CategoryPreview
import ru.digitalhustle.certis.features.category.query.service.CategoryUsageQueryService
import ru.digitalhustle.certis.features.category.validator.CategoryArchiveValidator
import java.util.UUID

@Service
class CategoryApplicationServiceImpl(
    private val categoryService: CategoryService,
    private val categoryUsageQueryService: CategoryUsageQueryService,
    private val categoryArchiveValidator: CategoryArchiveValidator,
) : CategoryApplicationService {

    override fun save(category: NewCategory): CategoryPreview = categoryService.save(category)

    override fun update(category: UpdateCategoryData): CategoryPreview = categoryService.update(category)

    override fun restore(
        id: UUID,
        userId: UUID,
    ): Unit = categoryService.restore(id, userId)

    @Transactional
    override fun archive(
        id: UUID,
        userId: UUID,
    ) {
        val category = categoryService.getByIdForUpdate(id, userId)

        if (category.archivedAt != null) {
            return
        }
        categoryArchiveValidator.validate(categoryUsageQueryService.isRequired(id, userId))

        categoryService.archive(id, userId)
    }
}

package ru.digitalhustle.certis.features.category.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.category.model.Category
import ru.digitalhustle.certis.features.category.model.CategoryPreview
import ru.digitalhustle.certis.features.category.model.toPreview
import ru.digitalhustle.certis.features.category.query.repository.CategoryQueryRepository
import ru.digitalhustle.certis.features.category.query.service.CategoryQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CategoryQueryServiceImpl(
    private val categoryRepository: CategoryQueryRepository,
) : CategoryQueryService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): CategoryPreview = getCategory(id, userId).toPreview()

    override fun getAllByUserId(userId: UUID): List<CategoryPreview> =
        categoryRepository.findAllByUserId(userId).map(Category::toPreview)

    private fun getCategory(id: UUID, userId: UUID): Category =
        categoryRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Category")
}

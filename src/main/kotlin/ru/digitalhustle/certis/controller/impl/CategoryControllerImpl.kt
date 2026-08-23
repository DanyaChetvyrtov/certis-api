package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.CategoryController
import ru.digitalhustle.certis.dto.CategoryDto
import ru.digitalhustle.certis.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.mapper.CategoryMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.transaction.CategoryAggregator
import java.util.UUID

@RestController
class CategoryControllerImpl(
    private val categoryAggregator: CategoryAggregator,
    private val categoryMapper: CategoryMapper,
) : CategoryController {

    override fun getCategories(jwtDetails: JwtDetails): List<CategoryDto> =
        categoryAggregator.getAllByUserId(jwtDetails.id)
            .map(categoryMapper::convert)

    override fun getCategoryById(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryAggregator.getById(categoryId, jwtDetails.id),
        )

    override fun createCategory(
        createCategoryRq: CreateCategoryRq,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryAggregator.save(
                categoryMapper.convert(createCategoryRq, jwtDetails.id),
            ),
        )

    override fun updateCategory(
        categoryId: UUID,
        updateCategoryRq: UpdateCategoryRq,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryAggregator.update(
                categoryMapper.convert(updateCategoryRq, categoryId, jwtDetails.id),
            ),
        )

    override fun restoreCategory(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = categoryAggregator.restore(categoryId, jwtDetails.id)

    override fun archiveCategory(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = categoryAggregator.archive(categoryId, jwtDetails.id)
}

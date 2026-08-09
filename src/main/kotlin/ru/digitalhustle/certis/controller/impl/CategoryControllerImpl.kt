package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.CategoryController
import ru.digitalhustle.certis.dto.CategoryDto
import ru.digitalhustle.certis.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.mapper.CategoryMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.domain.CategoryService
import java.util.UUID

@RestController
class CategoryControllerImpl(
    private val categoryService: CategoryService,
    private val categoryMapper: CategoryMapper,
) : CategoryController {

    override fun getCategories(jwtDetails: JwtDetails): List<CategoryDto> =
        categoryService.getAllByUserId(jwtDetails.id)
            .map(categoryMapper::convert)

    override fun getCategoryById(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryService.getById(categoryId, jwtDetails.id),
        )

    override fun createCategory(
        createCategoryRq: CreateCategoryRq,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryService.save(
                categoryMapper.convert(createCategoryRq, jwtDetails.id),
            ),
        )

    override fun updateCategory(
        categoryId: UUID,
        updateCategoryRq: UpdateCategoryRq,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryService.update(
                categoryMapper.convert(updateCategoryRq, categoryId, jwtDetails.id),
            ),
        )

    override fun restoreCategory(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = categoryService.restore(categoryId, jwtDetails.id)

    override fun archiveCategory(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = categoryService.archive(categoryId, jwtDetails.id)
}

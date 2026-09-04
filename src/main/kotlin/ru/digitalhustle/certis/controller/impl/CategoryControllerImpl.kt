package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.CategoryController
import ru.digitalhustle.certis.dto.CategoryDto
import ru.digitalhustle.certis.dto.request.CategoryAnalyticsRq
import ru.digitalhustle.certis.dto.request.CategoryCardPageRq
import ru.digitalhustle.certis.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.dto.response.CategoryAnalyticsRs
import ru.digitalhustle.certis.dto.response.CategoryCardsRs
import ru.digitalhustle.certis.dto.response.CategoryOptionRs
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.mapper.CategoryAnalyticsMapper
import ru.digitalhustle.certis.mapper.CategoryMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.transaction.CategoryAggregator
import java.util.UUID

@RestController
class CategoryControllerImpl(
    private val categoryMapper: CategoryMapper,
    private val categoryAnalyticsMapper: CategoryAnalyticsMapper,
    private val categoryAggregator: CategoryAggregator,
) : CategoryController {

    override fun getCategories(
        pageRq: CategoryCardPageRq,
        jwtDetails: JwtDetails,
    ): CategoryCardsRs =
        categoryMapper.convert(
            categoryAggregator.getCards(
                jwtDetails.id,
                categoryMapper.convert(pageRq),
            ),
        )

    override fun getCategoryAnalytics(
        analyticsRq: CategoryAnalyticsRq,
        jwtDetails: JwtDetails,
    ): CategoryAnalyticsRs =
        categoryAnalyticsMapper.convert(
            categoryAggregator.getAnalytics(
                jwtDetails.id,
                categoryAnalyticsMapper.convert(analyticsRq),
            ),
        )

    override fun getCategoryOptions(
        type: CategoryType,
        jwtDetails: JwtDetails,
    ): List<CategoryOptionRs> =
        categoryMapper.convert(
            categoryAggregator.getOptions(jwtDetails.id, type),
        )

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

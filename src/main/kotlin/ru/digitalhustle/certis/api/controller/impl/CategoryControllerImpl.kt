package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.CategoryController
import ru.digitalhustle.certis.api.dto.CategoryDto
import ru.digitalhustle.certis.api.dto.request.CategoryAnalyticsRq
import ru.digitalhustle.certis.api.dto.request.CategoryCardPageRq
import ru.digitalhustle.certis.api.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.api.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.api.dto.response.CategoryAnalyticsRs
import ru.digitalhustle.certis.api.dto.response.CategoryCardsRs
import ru.digitalhustle.certis.api.dto.response.CategoryOptionsRs
import ru.digitalhustle.certis.api.mapper.CategoryAnalyticsMapper
import ru.digitalhustle.certis.api.mapper.CategoryMapper
import ru.digitalhustle.certis.features.category.application.service.CategoryApplicationService
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.query.service.CategoryAnalyticsService
import ru.digitalhustle.certis.features.category.query.service.CategoryCardService
import ru.digitalhustle.certis.features.category.query.service.CategoryOptionService
import ru.digitalhustle.certis.features.category.query.service.CategoryQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class CategoryControllerImpl(
    private val categoryQueryService: CategoryQueryService,
    private val categoryCardService: CategoryCardService,
    private val categoryAnalyticsService: CategoryAnalyticsService,
    private val categoryOptionService: CategoryOptionService,
    private val categoryMapper: CategoryMapper,
    private val categoryAnalyticsMapper: CategoryAnalyticsMapper,
    private val categoryApplicationService: CategoryApplicationService,
) : CategoryController {

    override fun getCategories(
        pageRq: CategoryCardPageRq,
        jwtDetails: JwtDetails,
    ): CategoryCardsRs =
        categoryMapper.convert(
            categoryCardService.getCards(
                jwtDetails.id,
                categoryMapper.convert(pageRq),
            ),
        )

    override fun getCategoryAnalytics(
        analyticsRq: CategoryAnalyticsRq,
        jwtDetails: JwtDetails,
    ): CategoryAnalyticsRs =
        categoryAnalyticsMapper.convert(
            categoryAnalyticsService.getAnalytics(
                jwtDetails.id,
                categoryAnalyticsMapper.convert(analyticsRq),
            ),
        )

    override fun getCategoryOptions(
        type: CategoryType,
        jwtDetails: JwtDetails,
    ): CategoryOptionsRs =
        CategoryOptionsRs(
            categoryMapper.convert(
                categoryOptionService.getOptions(jwtDetails.id, type),
            ),
        )

    override fun getCategoryById(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryQueryService.getById(categoryId, jwtDetails.id),
        )

    override fun createCategory(
        createCategoryRq: CreateCategoryRq,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryApplicationService.save(
                categoryMapper.convert(createCategoryRq, jwtDetails.id),
            ),
        )

    override fun updateCategory(
        categoryId: UUID,
        updateCategoryRq: UpdateCategoryRq,
        jwtDetails: JwtDetails,
    ): CategoryDto =
        categoryMapper.convert(
            categoryApplicationService.update(
                categoryMapper.convert(updateCategoryRq, categoryId, jwtDetails.id),
            ),
        )

    override fun restoreCategory(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = categoryApplicationService.restore(categoryId, jwtDetails.id)

    override fun archiveCategory(
        categoryId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = categoryApplicationService.archive(categoryId, jwtDetails.id)
}

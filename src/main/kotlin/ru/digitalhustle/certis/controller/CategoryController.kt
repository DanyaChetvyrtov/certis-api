package ru.digitalhustle.certis.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.CategoryDto
import ru.digitalhustle.certis.dto.request.CategoryAnalyticsRq
import ru.digitalhustle.certis.dto.request.CategoryCardPageRq
import ru.digitalhustle.certis.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.dto.response.CategoryAnalyticsRs
import ru.digitalhustle.certis.dto.response.CategoryCardsRs
import ru.digitalhustle.certis.dto.response.CategoryOptionRs
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.security.JwtDetails
import java.util.UUID

@RequestMapping(PathConstants.CATEGORIES)
interface CategoryController {

    @GetMapping
    fun getCategories(
        @Valid @ModelAttribute pageRq: CategoryCardPageRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): CategoryCardsRs

    @GetMapping(PathConstants.CATEGORY_ANALYTICS)
    fun getCategoryAnalytics(
        @Valid @ModelAttribute analyticsRq: CategoryAnalyticsRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): CategoryAnalyticsRs

    @GetMapping(PathConstants.CATEGORY_OPTIONS)
    fun getCategoryOptions(
        @RequestParam type: CategoryType,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): List<CategoryOptionRs>

    @GetMapping(PathConstants.CATEGORY_ID)
    fun getCategoryById(
        @PathVariable categoryId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): CategoryDto

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @Valid @RequestBody createCategoryRq: CreateCategoryRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): CategoryDto

    @PutMapping(PathConstants.CATEGORY_ID)
    fun updateCategory(
        @PathVariable categoryId: UUID,
        @Valid @RequestBody updateCategoryRq: UpdateCategoryRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): CategoryDto

    @PostMapping(PathConstants.CATEGORY_RESTORE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun restoreCategory(
        @PathVariable categoryId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )

    @DeleteMapping(PathConstants.CATEGORY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun archiveCategory(
        @PathVariable categoryId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )
}

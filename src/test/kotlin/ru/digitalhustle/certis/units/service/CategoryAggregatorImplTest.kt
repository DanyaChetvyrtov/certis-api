package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryCardSort
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.CategoryInUseException
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import ru.digitalhustle.certis.model.category.CategoryOption
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.service.domain.CategoryAnalyticsService
import ru.digitalhustle.certis.service.domain.CategoryCardService
import ru.digitalhustle.certis.service.domain.CategoryOptionService
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.transaction.impl.CategoryAggregatorImpl
import ru.digitalhustle.certis.util.validation.CategoryValidator
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

class CategoryAggregatorImplTest {

    private val categoryService = mock(CategoryService::class.java)
    private val categoryCardService = mock(CategoryCardService::class.java)
    private val categoryAnalyticsService = mock(CategoryAnalyticsService::class.java)
    private val categoryOptionService = mock(CategoryOptionService::class.java)
    private val categoryValidator = mock(CategoryValidator::class.java)
    private val categoryAggregator = CategoryAggregatorImpl(
        categoryService,
        categoryCardService,
        categoryAnalyticsService,
        categoryOptionService,
        categoryValidator,
    )

    @Test
    fun `should get category cards for month`() {
        // given
        val userId = UUID.randomUUID()
        val month = YearMonth.of(2026, 9)
        val filter = CategoryCardFilter(month, Currency.RUB, 0, 20, CategoryCardSort.AMOUNT_DESC)
        val cards = CategoryCards(month, Currency.RUB, emptyList(), 0, 20, 0)

        `when`(categoryCardService.getCards(userId, filter)).thenReturn(cards)

        // when
        val result = categoryAggregator.getCards(userId, filter)

        // then
        assertThat(result).isEqualTo(cards)
    }

    @Test
    fun `should get category analytics for month`() {
        // given
        val userId = UUID.randomUUID()
        val month = YearMonth.of(2026, 9)
        val filter = CategoryAnalyticsFilter(month, Currency.RUB, 4, CategoryType.EXPENSE)
        val analytics = CategoryAnalytics(
            month = month,
            currency = Currency.RUB,
            type = CategoryType.EXPENSE,
            totalTransactionCount = 0,
            categorizedTransactionCount = 0,
            uncategorizedTransactionCount = 0,
            totalSum = BigDecimal.ZERO,
            categorizedSum = BigDecimal.ZERO,
            uncategorizedSum = BigDecimal.ZERO,
            coveragePercentage = null,
            topExpenseCategories = emptyList(),
        )

        `when`(categoryAnalyticsService.getAnalytics(userId, filter)).thenReturn(analytics)

        // when
        val result = categoryAggregator.getAnalytics(userId, filter)

        // then
        assertThat(result).isEqualTo(analytics)
    }

    @Test
    fun `should get category options`() {
        // given
        val userId = UUID.randomUUID()
        val options = listOf(
            CategoryOption(UUID.randomUUID(), "Food", "utensils", "#E58E4E"),
        )

        `when`(categoryOptionService.getOptions(userId, CategoryType.EXPENSE)).thenReturn(options)

        // when
        val result = categoryAggregator.getOptions(userId, CategoryType.EXPENSE)

        // then
        assertThat(result).isEqualTo(options)
    }

    @Test
    fun `should archive unused category after locking it`() {
        // given
        val category = createCategory()

        `when`(categoryService.getByIdForUpdate(category.id, category.userId))
            .thenReturn(category)
        `when`(categoryValidator.isRequired(category.id, category.userId))
            .thenReturn(false)

        // when
        categoryAggregator.archive(category.id, category.userId)

        // then
        verify(categoryService).getByIdForUpdate(category.id, category.userId)
        verify(categoryValidator).isRequired(category.id, category.userId)
        verify(categoryService).archive(category.id, category.userId)
    }

    @Test
    fun `should reject archive when category is still in use`() {
        // given
        val category = createCategory()

        `when`(categoryService.getByIdForUpdate(category.id, category.userId))
            .thenReturn(category)
        `when`(categoryValidator.isRequired(category.id, category.userId))
            .thenReturn(true)

        // when, then
        assertThatThrownBy {
            categoryAggregator.archive(category.id, category.userId)
        }
            .isInstanceOf(CategoryInUseException::class.java)
            .hasMessage(ErrorMessages.CATEGORY_IN_USE)

        verify(categoryService, never()).archive(category.id, category.userId)
    }

    @Test
    fun `should archive category idempotently without checking usage`() {
        // given
        val category = createCategory(archivedAt = OffsetDateTime.now())

        `when`(categoryService.getByIdForUpdate(category.id, category.userId))
            .thenReturn(category)

        // when
        categoryAggregator.archive(category.id, category.userId)

        // then
        verifyNoInteractions(categoryValidator)
        verify(categoryService, never()).archive(category.id, category.userId)
    }

    private fun createCategory(archivedAt: OffsetDateTime? = null): Category =
        Category(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = "Food",
            type = CategoryType.EXPENSE,
            icon = "utensils",
            color = "#E58E4E",
            archivedAt = archivedAt,
        )
}

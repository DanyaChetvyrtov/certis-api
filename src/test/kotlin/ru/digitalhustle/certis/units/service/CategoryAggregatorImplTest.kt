package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.exception.custom.CategoryInUseException
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.provider.CategoryUsageProvider
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.transaction.impl.CategoryAggregatorImpl
import java.time.OffsetDateTime
import java.util.UUID

class CategoryAggregatorImplTest {

    private val categoryService = mock(CategoryService::class.java)
    private val categoryUsageProvider = mock(CategoryUsageProvider::class.java)
    private val categoryAggregator = CategoryAggregatorImpl(categoryService, categoryUsageProvider)

    @Test
    fun `should archive unused category after locking it`() {
        // given
        val category = createCategory()

        `when`(categoryService.getByIdForUpdate(category.id, category.userId))
            .thenReturn(category)
        `when`(categoryUsageProvider.isRequired(category.id, category.userId))
            .thenReturn(false)

        // when
        categoryAggregator.archive(category.id, category.userId)

        // then
        verify(categoryService).getByIdForUpdate(category.id, category.userId)
        verify(categoryUsageProvider).isRequired(category.id, category.userId)
        verify(categoryService).archive(category.id, category.userId)
    }

    @Test
    fun `should reject archive when category is still in use`() {
        // given
        val category = createCategory()

        `when`(categoryService.getByIdForUpdate(category.id, category.userId))
            .thenReturn(category)
        `when`(categoryUsageProvider.isRequired(category.id, category.userId))
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
        verifyNoInteractions(categoryUsageProvider)
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

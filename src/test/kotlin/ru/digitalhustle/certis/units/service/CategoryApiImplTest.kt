package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.features.category.api.impl.CategoryApiImpl
import ru.digitalhustle.certis.features.category.command.service.CategoryService
import java.util.UUID

class CategoryApiImplTest {

    private val categoryService = mock(CategoryService::class.java)
    private val categoryApi = CategoryApiImpl(categoryService)

    @Test
    fun `should accept active expense categories`() {
        // given
        val userId = UUID.randomUUID()
        val categoryIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        `when`(categoryService.countActiveExpenseCategories(userId, categoryIds.toSet()))
            .thenReturn(categoryIds.size)

        // when
        val eligible = categoryApi.areAllActive(userId, categoryIds)

        // then
        assertThat(eligible).isTrue()
    }

    @Test
    fun `should reject collection containing ineligible category`() {
        // given
        val userId = UUID.randomUUID()
        val categoryIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        `when`(categoryService.countActiveExpenseCategories(userId, categoryIds.toSet()))
            .thenReturn(categoryIds.size - 1)

        // when
        val eligible = categoryApi.areAllActive(userId, categoryIds)

        // then
        assertThat(eligible).isFalse()
    }
}

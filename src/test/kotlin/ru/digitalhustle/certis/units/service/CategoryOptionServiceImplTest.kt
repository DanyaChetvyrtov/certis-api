package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.category.CategoryOption
import ru.digitalhustle.certis.repository.CategoryOptionRepository
import ru.digitalhustle.certis.service.domain.impl.CategoryOptionServiceImpl
import java.util.UUID

class CategoryOptionServiceImplTest {

    private val categoryOptionRepository = mock(CategoryOptionRepository::class.java)
    private val categoryOptionService = CategoryOptionServiceImpl(categoryOptionRepository)

    @Test
    fun `should get active category options`() {
        // given
        val userId = UUID.randomUUID()
        val options = listOf(
            CategoryOption(UUID.randomUUID(), "Food", "utensils", "#E58E4E"),
        )

        `when`(categoryOptionRepository.findActiveByUserIdAndType(userId, CategoryType.EXPENSE))
            .thenReturn(options)

        // when
        val result = categoryOptionService.getOptions(userId, CategoryType.EXPENSE)

        // then
        assertThat(result).isEqualTo(options)
    }
}

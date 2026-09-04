package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.exception.custom.CategoryArchivedException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.provider.DefaultCategoryProvider
import ru.digitalhustle.certis.repository.CategoryRepository
import ru.digitalhustle.certis.service.domain.impl.CategoryServiceImpl
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CategoryServiceImplTest {

    private val categoryRepository = mock(CategoryRepository::class.java)
    private val defaultCategoryProvider = DefaultCategoryProvider()
    private val clock = Clock.fixed(Instant.parse("2026-08-08T20:00:00Z"), ZoneOffset.UTC)
    private val categoryService = CategoryServiceImpl(categoryRepository, defaultCategoryProvider, clock)

    private companion object {
        private const val DEFAULT_CATEGORY_COUNT = 11
        private const val NAME = "Groceries"
        private const val UPDATED_NAME = "Food"
        private const val ICON = "shopping-cart"
        private const val UPDATED_ICON = "utensils"
        private const val COLOR = "#10B981"
        private const val UPDATED_COLOR = "#B08D57"
    }

    @Test
    fun `should get category`() {
        // given
        val category = createCategory()

        `when`(categoryRepository.findByIdAndUserId(category.id, category.userId))
            .thenReturn(category)

        // when
        val result = categoryService.getById(category.id, category.userId)

        // then
        assertAll(
            { assertThat(result.id).isEqualTo(category.id) },
            { assertThat(result.name).isEqualTo(category.name) },
            { assertThat(result.type).isEqualTo(category.type) },
            { assertThat(result.icon).isEqualTo(category.icon) },
            { assertThat(result.color).isEqualTo(category.color) },
            { assertThat(result.archivedAt).isNull() },
        )
    }

    @Test
    fun `should throw not found when category does not belong to user`() {
        // given
        val categoryId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(categoryRepository.findByIdAndUserId(categoryId, userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            categoryService.getById(categoryId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should get category with shared lock`() {
        // given
        val category = createCategory()

        `when`(categoryRepository.findByIdAndUserIdForShare(category.id, category.userId))
            .thenReturn(category)

        // when
        val result = categoryService.getByIdForShare(category.id, category.userId)

        // then
        assertThat(result).isEqualTo(category)
    }

    @Test
    fun `should get requested categories with shared lock`() {
        // given
        val userId = UUID.randomUUID()
        val categories = listOf(
            createCategory(userId = userId),
            createCategory(userId = userId),
        )
        val categoryIds = categories.mapTo(linkedSetOf(), Category::id)

        `when`(categoryRepository.findAllByIdsAndUserIdForShare(categoryIds, userId))
            .thenReturn(categories)

        // when
        val result = categoryService.getAllByIdsForShare(categoryIds, userId)

        // then
        assertThat(result).isEqualTo(categories)
    }

    @Test
    fun `should throw not found when shared category batch is incomplete`() {
        // given
        val userId = UUID.randomUUID()
        val category = createCategory(userId = userId)
        val categoryIds = setOf(category.id, UUID.randomUUID())

        `when`(categoryRepository.findAllByIdsAndUserIdForShare(categoryIds, userId))
            .thenReturn(listOf(category))

        // when, then
        assertThatThrownBy {
            categoryService.getAllByIdsForShare(categoryIds, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should get category with exclusive lock`() {
        // given
        val category = createCategory()

        `when`(categoryRepository.findByIdAndUserIdForUpdate(category.id, category.userId))
            .thenReturn(category)

        // when
        val result = categoryService.getByIdForUpdate(category.id, category.userId)

        // then
        assertThat(result).isEqualTo(category)
    }

    @Test
    fun `should get all user categories`() {
        // given
        val userId = UUID.randomUUID()
        val categories = listOf(
            createCategory(userId = userId),
            createCategory(userId = userId, archivedAt = OffsetDateTime.now(clock)),
        )

        `when`(categoryRepository.findAllByUserId(userId))
            .thenReturn(categories)

        // when
        val result = categoryService.getAllByUserId(userId)

        // then
        assertThat(result.map { it.id }).containsExactlyElementsOf(categories.map { it.id })
    }

    @Test
    fun `should save category`() {
        // given
        val newCategory = createNewCategory().copy(
            name = "  $NAME  ",
            icon = "  $ICON  ",
        )
        val categoryCaptor = ArgumentCaptor.forClass(Category::class.java)

        `when`(categoryRepository.insert(captureCategory(categoryCaptor)))
            .thenAnswer { categoryCaptor.value }

        // when
        val result = categoryService.save(newCategory)

        // then
        assertAll(
            { assertThat(result.name).isEqualTo(NAME) },
            { assertThat(result.type).isEqualTo(newCategory.type) },
            { assertThat(result.icon).isEqualTo(ICON) },
            { assertThat(result.color).isEqualTo(newCategory.color) },
            { assertThat(result.archivedAt).isNull() },
            { assertThat(categoryCaptor.value.userId).isEqualTo(newCategory.userId) },
        )
    }

    @Test
    fun `should create default categories for user`() {
        // given
        val userId = UUID.randomUUID()
        var capturedCategories = emptyList<Category>()

        doAnswer { invocation ->
            capturedCategories = invocation.getArgument<Collection<Category>>(0).toList()
            null
        }.`when`(categoryRepository).insertAll(anyCollection<Category>())

        // when
        categoryService.createDefaults(userId)

        // then
        assertAll(
            { assertThat(capturedCategories).hasSize(DEFAULT_CATEGORY_COUNT) },
            { assertThat(capturedCategories).allMatch { category -> category.userId == userId } },
            { assertThat(capturedCategories).allMatch { category -> category.archivedAt == null } },
            {
                assertThat(
                    capturedCategories.filter { category -> category.type == CategoryType.EXPENSE }.map(Category::name),
                )
                    .containsExactly("Food", "Transport", "Housing", "Utilities", "Health", "Entertainment", "Other")
            },
            {
                assertThat(
                    capturedCategories.filter { category -> category.type == CategoryType.INCOME }.map(Category::name),
                )
                    .containsExactly("Salary", "Bonus", "Investment", "Other")
            },
        )
    }

    @Test
    fun `should update active category without changing type`() {
        // given
        val category = createCategory()
        val updateData = createUpdateCategoryData(category.id, category.userId).copy(
            name = "  $UPDATED_NAME  ",
            icon = "  $UPDATED_ICON  ",
        )
        val normalizedUpdateData = updateData.copy(
            name = UPDATED_NAME,
            icon = UPDATED_ICON,
        )
        val updatedCategory = category.copy(
            name = normalizedUpdateData.name,
            icon = normalizedUpdateData.icon,
            color = updateData.color,
        )

        `when`(categoryRepository.updateActive(normalizedUpdateData))
            .thenReturn(updatedCategory)

        // when
        val result = categoryService.update(updateData)

        // then
        assertAll(
            { assertThat(result.name).isEqualTo(UPDATED_NAME) },
            { assertThat(result.type).isEqualTo(category.type) },
            { assertThat(result.icon).isEqualTo(UPDATED_ICON) },
            { assertThat(result.color).isEqualTo(UPDATED_COLOR) },
            { assertThat(result.archivedAt).isNull() },
        )
        verify(categoryRepository, never())
            .findByIdAndUserId(category.id, category.userId)
    }

    @Test
    fun `should reject update for archived category`() {
        // given
        val category = createCategory(archivedAt = OffsetDateTime.now(clock))
        val updateData = createUpdateCategoryData(category.id, category.userId)

        `when`(categoryRepository.updateActive(updateData))
            .thenReturn(null)
        `when`(categoryRepository.findByIdAndUserId(category.id, category.userId))
            .thenReturn(category)

        // when, then
        assertThatThrownBy {
            categoryService.update(updateData)
        }
            .isInstanceOf(CategoryArchivedException::class.java)
            .hasMessage(ErrorMessages.CATEGORY_ARCHIVED)
    }

    @Test
    fun `should throw not found when updating missing category`() {
        // given
        val updateData = createUpdateCategoryData()

        `when`(categoryRepository.updateActive(updateData))
            .thenReturn(null)
        `when`(categoryRepository.findByIdAndUserId(updateData.id, updateData.userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            categoryService.update(updateData)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should restore category`() {
        // given
        val category = createCategory(archivedAt = OffsetDateTime.now(clock))

        `when`(categoryRepository.restore(category.id, category.userId))
            .thenReturn(true)

        // when
        categoryService.restore(category.id, category.userId)

        // then
        verify(categoryRepository).restore(category.id, category.userId)
        verify(categoryRepository, never())
            .findByIdAndUserId(category.id, category.userId)
    }

    @Test
    fun `should restore category idempotently`() {
        // given
        val category = createCategory()

        `when`(categoryRepository.restore(category.id, category.userId))
            .thenReturn(false)
        `when`(categoryRepository.findByIdAndUserId(category.id, category.userId))
            .thenReturn(category)

        // when
        categoryService.restore(category.id, category.userId)

        // then
        verify(categoryRepository).findByIdAndUserId(category.id, category.userId)
    }

    @Test
    fun `should throw not found when restoring missing category`() {
        // given
        val categoryId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(categoryRepository.restore(categoryId, userId))
            .thenReturn(false)
        `when`(categoryRepository.findByIdAndUserId(categoryId, userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            categoryService.restore(categoryId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should archive category`() {
        // given
        val category = createCategory()

        `when`(categoryRepository.archive(category.id, category.userId, OffsetDateTime.now(clock)))
            .thenReturn(true)

        // when
        categoryService.archive(category.id, category.userId)

        // then
        verify(categoryRepository).archive(category.id, category.userId, OffsetDateTime.now(clock))
        verify(categoryRepository, never())
            .findByIdAndUserId(category.id, category.userId)
    }

    @Test
    fun `should archive category idempotently`() {
        // given
        val category = createCategory(archivedAt = OffsetDateTime.now(clock))

        `when`(categoryRepository.archive(category.id, category.userId, OffsetDateTime.now(clock)))
            .thenReturn(false)
        `when`(categoryRepository.findByIdAndUserId(category.id, category.userId))
            .thenReturn(category)

        // when
        categoryService.archive(category.id, category.userId)

        // then
        verify(categoryRepository).findByIdAndUserId(category.id, category.userId)
    }

    @Test
    fun `should throw not found when archiving missing category`() {
        // given
        val categoryId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(categoryRepository.archive(categoryId, userId, OffsetDateTime.now(clock)))
            .thenReturn(false)
        `when`(categoryRepository.findByIdAndUserId(categoryId, userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            categoryService.archive(categoryId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    private fun createNewCategory(
        userId: UUID = UUID.randomUUID(),
    ): NewCategory =
        NewCategory(
            userId = userId,
            name = NAME,
            type = CategoryType.EXPENSE,
            icon = ICON,
            color = COLOR,
        )

    private fun createUpdateCategoryData(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
    ): UpdateCategoryData =
        UpdateCategoryData(
            id = id,
            userId = userId,
            name = UPDATED_NAME,
            icon = UPDATED_ICON,
            color = UPDATED_COLOR,
        )

    private fun createCategory(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        archivedAt: OffsetDateTime? = null,
    ): Category =
        Category(
            id = id,
            userId = userId,
            name = NAME,
            type = CategoryType.EXPENSE,
            icon = ICON,
            color = COLOR,
            archivedAt = archivedAt,
        )

    private fun captureCategory(captor: ArgumentCaptor<Category>): Category {
        captor.capture()
        return createCategory()
    }
}

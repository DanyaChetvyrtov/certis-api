package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.User
import java.time.OffsetDateTime
import java.util.UUID

class CategoryControllerIT : AbstractIntegrationTest() {

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val NAME = "Groceries"
        private const val UPDATED_NAME = "Food"
        private const val ICON = "shopping-cart"
        private const val UPDATED_ICON = "utensils"
        private const val COLOR = "#10B981"
        private const val UPDATED_COLOR = "#B08D57"
        private const val CATEGORY_NAME_CONFLICT = "Category with such name already exists"
    }

    @Test
    fun `should require authentication`() {
        // when
        mvc.perform(get(PathConstants.CATEGORIES))
            // then
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should create category for authenticated user`() {
        // given
        val user = userFixture.createInDb()

        // when
        val result = mvc.perform(
            post(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createCategoryRequest())),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value(NAME))
            .andExpect(jsonPath("$.type").value(CategoryType.EXPENSE.name))
            .andExpect(jsonPath("$.icon").value(ICON))
            .andExpect(jsonPath("$.color").value(COLOR))
            .andExpect(jsonPath("$.archivedAt").doesNotExist())
            .andReturn()

        val categoryId = UUID.fromString(
            objectMapper.readTree(result.response.contentAsByteArray)["id"].asText(),
        )
        val category = categoryRepository.findByIdAndUserId(categoryId, user.id)

        assertThat(category).isNotNull()
        assertThat(category?.userId).isEqualTo(user.id)
    }

    @Test
    fun `should reject duplicate active category`() {
        // given
        val user = userFixture.createInDb()
        createCategory(user.id)

        // when
        mvc.perform(
            post(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createCategoryRequest())),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(CATEGORY_NAME_CONFLICT))

        assertThat(categoryRepository.findAllByUserId(user.id)).hasSize(1)
    }

    @Test
    fun `should reject duplicate category with normalized name`() {
        // given
        val user = userFixture.createInDb()
        createCategory(user.id)
        val request = createCategoryRequest().copy(name = " groceries ")

        // when
        mvc.perform(
            post(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(CATEGORY_NAME_CONFLICT))
    }

    @Test
    fun `should allow same category for different users`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        createCategory(owner.id)

        // when
        mvc.perform(
            post(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(anotherUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createCategoryRequest())),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value(NAME))
    }

    @Test
    fun `should allow same category name for different types`() {
        // given
        val user = userFixture.createInDb()
        createCategory(user.id, type = CategoryType.EXPENSE)
        val request = createCategoryRequest().copy(type = CategoryType.INCOME)

        // when
        mvc.perform(
            post(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value(NAME))
            .andExpect(jsonPath("$.type").value(CategoryType.INCOME.name))
    }

    @Test
    fun `should reject invalid category`() {
        // given
        val user = userFixture.createInDb()
        val request = CreateCategoryRq(
            name = " ",
            type = CategoryType.EXPENSE,
            icon = " ",
            color = "green",
        )

        // when
        mvc.perform(
            post(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.message").value(ErrorMessages.VALIDATION_FAILED))
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.icon").exists())
            .andExpect(jsonPath("$.errors.color").exists())
    }

    @Test
    fun `should get category`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)

        // when
        mvc.perform(
            get("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(category.id.toString()))
            .andExpect(jsonPath("$.name").value(category.name))
            .andExpect(jsonPath("$.type").value(category.type.name))
            .andExpect(jsonPath("$.icon").value(category.icon))
            .andExpect(jsonPath("$.color").value(category.color))
    }

    @Test
    fun `should not expose another user's category`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val category = createCategory(owner.id)

        // when
        mvc.perform(
            get("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(anotherUser)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Category not found"))
    }

    @Test
    fun `should not mutate another user's category`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val category = createCategory(owner.id)

        // when, then
        mvc.perform(
            put("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(anotherUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(updateCategoryRequest())),
        ).andExpect(status().isNotFound)

        mvc.perform(
            delete("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(anotherUser)),
        ).andExpect(status().isNotFound)

        mvc.perform(
            post("${PathConstants.CATEGORIES}/${category.id}/restore")
                .cookie(accessTokenCookie(anotherUser)),
        ).andExpect(status().isNotFound)

        val unchangedCategory = categoryRepository.findByIdAndUserId(category.id, owner.id)

        assertThat(unchangedCategory?.name).isEqualTo(category.name)
        assertThat(unchangedCategory?.archivedAt).isNull()
    }

    @Test
    fun `should get only authenticated user's categories including archived ones`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val activeCategory = createCategory(user.id, name = "Food")
        val archivedCategory = createCategory(
            userId = user.id,
            name = "Old category",
            archivedAt = OffsetDateTime.now(),
        )
        createCategory(anotherUser.id, name = "Hidden")

        // when
        mvc.perform(
            get(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(
                jsonPath("$[*].id").value(
                    containsInAnyOrder(
                        activeCategory.id.toString(),
                        archivedCategory.id.toString(),
                    ),
                ),
            )
    }

    @Test
    fun `should update active category`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        val request = updateCategoryRequest()

        // when
        mvc.perform(
            put("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(UPDATED_NAME))
            .andExpect(jsonPath("$.type").value(CategoryType.EXPENSE.name))
            .andExpect(jsonPath("$.icon").value(UPDATED_ICON))
            .andExpect(jsonPath("$.color").value(UPDATED_COLOR))
            .andExpect(jsonPath("$.archivedAt").doesNotExist())

        val updatedCategory = categoryRepository.findByIdAndUserId(category.id, user.id)

        assertThat(updatedCategory?.type).isEqualTo(category.type)
    }

    @Test
    fun `should reject update to duplicate active category`() {
        // given
        val user = userFixture.createInDb()
        createCategory(user.id)
        val categoryToUpdate = createCategory(user.id, name = "Transport")
        val request = updateCategoryRequest().copy(name = NAME)

        // when
        mvc.perform(
            put("${PathConstants.CATEGORIES}/${categoryToUpdate.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(CATEGORY_NAME_CONFLICT))

        assertThat(categoryRepository.findByIdAndUserId(categoryToUpdate.id, user.id)?.name)
            .isEqualTo(categoryToUpdate.name)
    }

    @Test
    fun `should reject update for archived category`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id, archivedAt = OffsetDateTime.now())

        // when
        mvc.perform(
            put("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(updateCategoryRequest())),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.CATEGORY_ARCHIVED))
    }

    @Test
    fun `should restore archived category`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id, archivedAt = OffsetDateTime.now())

        // when
        mvc.perform(
            post("${PathConstants.CATEGORIES}/${category.id}/restore")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(categoryRepository.findByIdAndUserId(category.id, user.id)?.archivedAt)
            .isNull()
    }

    @Test
    fun `should reject restore when active duplicate exists`() {
        // given
        val user = userFixture.createInDb()
        createCategory(user.id)
        val archivedCategory = createCategory(
            userId = user.id,
            archivedAt = OffsetDateTime.now(),
        )

        // when
        mvc.perform(
            post("${PathConstants.CATEGORIES}/${archivedCategory.id}/restore")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(CATEGORY_NAME_CONFLICT))

        assertThat(categoryRepository.findByIdAndUserId(archivedCategory.id, user.id)?.archivedAt)
            .isNotNull()
    }

    @Test
    fun `should restore active category idempotently`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)

        // when
        mvc.perform(
            post("${PathConstants.CATEGORIES}/${category.id}/restore")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(categoryRepository.findByIdAndUserId(category.id, user.id)?.archivedAt)
            .isNull()
    }

    @Test
    fun `should archive category without deleting it`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)

        // when
        mvc.perform(
            delete("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        val archivedCategory = categoryRepository.findByIdAndUserId(category.id, user.id)

        assertThat(archivedCategory).isNotNull()
        assertThat(archivedCategory?.archivedAt).isNotNull()
    }

    @Test
    fun `should archive category idempotently`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)

        mvc.perform(
            delete("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNoContent)

        val firstArchivedAt = categoryRepository.findByIdAndUserId(category.id, user.id)?.archivedAt

        // when
        mvc.perform(
            delete("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(categoryRepository.findByIdAndUserId(category.id, user.id)?.archivedAt)
            .isEqualTo(firstArchivedAt)
    }

    private fun createCategoryRequest(): CreateCategoryRq =
        CreateCategoryRq(
            name = NAME,
            type = CategoryType.EXPENSE,
            icon = ICON,
            color = COLOR,
        )

    private fun updateCategoryRequest(): UpdateCategoryRq =
        UpdateCategoryRq(
            name = UPDATED_NAME,
            icon = UPDATED_ICON,
            color = UPDATED_COLOR,
        )

    private fun createCategory(
        userId: UUID,
        name: String = NAME,
        type: CategoryType = CategoryType.EXPENSE,
        archivedAt: OffsetDateTime? = null,
    ): Category =
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = type,
                icon = ICON,
                color = COLOR,
                archivedAt = archivedAt,
            ),
        )

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

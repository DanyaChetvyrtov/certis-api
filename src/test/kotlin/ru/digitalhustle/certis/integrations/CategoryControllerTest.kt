package ru.digitalhustle.certis.integrations

import com.fasterxml.jackson.databind.JsonNode
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.jooq.generated.Tables
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
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.CategoryCardSort
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Budget
import ru.digitalhustle.certis.model.entity.BudgetCategory
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class CategoryControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val MAX_ICON_LENGTH = 50
        private const val NAME = "Groceries"
        private const val UPDATED_NAME = "Food"
        private const val ICON = "shopping-cart"
        private const val UPDATED_ICON = "utensils"
        private const val COLOR = "#10B981"
        private const val UPDATED_COLOR = "#B08D57"
        private const val CATEGORY_NAME_CONFLICT = "Category with such name already exists"
        private const val MONTH = "2026-09"
    }

    @Test
    fun `should require authentication`() {
        // when
        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name),
        )
            // then
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should create category for authenticated user`() {
        // given
        val user = userFixture.createInDb()
        val request = createCategoryRequest().copy(
            name = "  $NAME  ",
            icon = "  $ICON  ",
        )

        // when
        val result = mvc.perform(
            post(PathConstants.CATEGORIES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
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
            icon = "x".repeat(MAX_ICON_LENGTH + 1),
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
                .param("month", MONTH)
                .param("currency", Currency.USD.name)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value(MONTH))
            .andExpect(jsonPath("$.currency").value(Currency.USD.name))
            .andExpect(jsonPath("$.categories.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(
                jsonPath("$.categories[*].id").value(
                    containsInAnyOrder(
                        activeCategory.id.toString(),
                        archivedCategory.id.toString(),
                    ),
                ),
            )
    }

    @Test
    fun `should return only active owned category options of requested type`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "category-options-other@test.com")
        }
        val food = createCategory(user.id, name = "food")
        val transport = createCategory(user.id, name = "Transport")
        createCategory(user.id, name = "Archived", archivedAt = OffsetDateTime.now())
        createCategory(user.id, name = "Salary", type = CategoryType.INCOME)
        createCategory(anotherUser.id, name = "Hidden")

        // when
        mvc.perform(
            get("${PathConstants.CATEGORIES}${PathConstants.CATEGORY_OPTIONS}")
                .param("type", CategoryType.EXPENSE.name)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(food.id.toString()))
            .andExpect(jsonPath("$[0].name").value(food.name))
            .andExpect(jsonPath("$[0].icon").value(food.icon))
            .andExpect(jsonPath("$[0].color").value(food.color))
            .andExpect(jsonPath("$[0].type").doesNotExist())
            .andExpect(jsonPath("$[0].archivedAt").doesNotExist())
            .andExpect(jsonPath("$[1].id").value(transport.id.toString()))
    }

    @Test
    fun `should validate category options parameters and authentication`() {
        // given
        val user = userFixture.createInDb()
        val path = "${PathConstants.CATEGORIES}${PathConstants.CATEGORY_OPTIONS}"

        // when, then
        mvc.perform(
            get(path)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(path)
                .param("type", CategoryType.EXPENSE.name),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should calculate monthly category cards in requested currency`() {
        // given
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.USD) }
        val groceries = createCategory(user.id, name = "Groceries")
        val housing = createCategory(user.id, name = "Housing")
        val education = createCategory(user.id, name = "Education")
        val salary = createCategory(user.id, name = "Salary", type = CategoryType.INCOME)
        val rubAccount = createAccount(user.id, Currency.RUB)
        val eurAccount = createAccount(user.id, Currency.EUR)

        createTransaction(rubAccount, groceries, "40.00", "2026-09-01T00:00:00Z")
        createTransaction(rubAccount, groceries, "60.00", "2026-09-30T23:59:59Z")
        createTransaction(rubAccount, housing, "100.00", "2026-09-15T12:00:00Z")
        createTransaction(rubAccount, salary, "300.00", "2026-09-10T12:00:00Z")
        createTransaction(eurAccount, groceries, "999.00", "2026-09-12T12:00:00Z")
        createTransaction(rubAccount, groceries, "70.00", "2026-08-31T23:59:59Z")
        createTransaction(rubAccount, groceries, "80.00", "2026-10-01T00:00:00Z")
        createTransaction(
            account = rubAccount,
            category = groceries,
            amount = "90.00",
            occurredAt = "2026-09-20T12:00:00Z",
            deleted = true,
        )

        // when
        val result = mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value(MONTH))
            .andExpect(jsonPath("$.currency").value(Currency.RUB.name))
            .andExpect(jsonPath("$.categories.length()").value(4))
            .andReturn()

        val response = objectMapper.readTree(result.response.contentAsByteArray)
        val cardsByName = response["categories"].associateBy { card -> card["name"].asText() }

        assertCategoryCard(cardsByName.getValue("Groceries"), 2, "100.00", "50.00")
        assertCategoryCard(cardsByName.getValue("Housing"), 1, "100.00", "50.00")
        assertCategoryCard(cardsByName.getValue("Education"), 0, "0.00", "0.00")
        assertCategoryCard(cardsByName.getValue("Salary"), 1, "300.00", "100.00")
        assertThat(cardsByName.getValue("Education").has("archivedAt")).isTrue()
        assertThat(cardsByName.getValue("Education")["archivedAt"].isNull).isTrue()

        // when
        val eurResult = mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .param("currency", Currency.EUR.name)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value(MONTH))
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))
            .andExpect(jsonPath("$.categories.length()").value(4))
            .andReturn()

        val eurResponse = objectMapper.readTree(eurResult.response.contentAsByteArray)
        val eurCardsByName = eurResponse["categories"].associateBy { card -> card["name"].asText() }

        assertCategoryCard(eurCardsByName.getValue("Groceries"), 1, "999.00", "100.00")
        assertCategoryCard(eurCardsByName.getValue("Housing"), 0, "0.00", "0.00")
        assertCategoryCard(eurCardsByName.getValue("Education"), 0, "0.00", "0.00")
        assertCategoryCard(eurCardsByName.getValue("Salary"), 0, "0.00", "0.00")
    }

    @Test
    fun `should paginate and sort category cards`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, Currency.RUB)
        val alpha = createCategory(user.id, name = "Alpha")
        val beta = createCategory(user.id, name = "Beta")
        val gamma = createCategory(user.id, name = "Gamma")
        createCategory(user.id, name = "Zulu")

        createTransaction(account, alpha, "50.00", "2026-09-10T12:00:00Z")
        createTransaction(account, beta, "200.00", "2026-09-10T12:00:00Z")
        createTransaction(account, gamma, "100.00", "2026-09-10T12:00:00Z")

        // when
        val firstPage = getCategoryCards(user, page = 0, size = 2)
        val secondPage = getCategoryCards(user, page = 1, size = 2)
        val sortedByName = getCategoryCards(user, sort = CategoryCardSort.NAME)
        val sortedByAmountAsc = getCategoryCards(user, sort = CategoryCardSort.AMOUNT_ASC)

        // then
        assertThat(firstPage["categories"].map { card -> card["name"].asText() })
            .containsExactly("Beta", "Gamma")
        assertThat(firstPage["page"].asInt()).isZero()
        assertThat(firstPage["size"].asInt()).isEqualTo(2)
        assertThat(firstPage["totalElements"].asLong()).isEqualTo(4)
        assertThat(firstPage["totalPages"].asInt()).isEqualTo(2)
        assertCategoryCard(firstPage["categories"][0], 1, "200.00", "57.14")
        assertThat(secondPage["categories"].map { card -> card["name"].asText() })
            .containsExactly("Alpha", "Zulu")
        assertThat(sortedByName["categories"].map { card -> card["name"].asText() })
            .containsExactly("Alpha", "Beta", "Gamma", "Zulu")
        assertThat(sortedByAmountAsc["categories"].map { card -> card["name"].asText() })
            .containsExactly("Zulu", "Alpha", "Gamma", "Beta")
    }

    @Test
    fun `should require valid category card parameters`() {
        // given
        val user = userFixture.createInDb()

        // when, then
        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("currency", Currency.RUB.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", "2026-13")
                .param("currency", Currency.RUB.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .param("currency", "GBP")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("page", "-1")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("size", "101")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(PathConstants.CATEGORIES)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("sort", "UNKNOWN")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should update active category`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        val request = updateCategoryRequest().copy(
            name = "  $UPDATED_NAME  ",
            icon = "  $UPDATED_ICON  ",
        )

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
    fun `should reject archive for category used by active recurring template`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        val account = createAccount(user.id)
        createRecurringTemplate(account, category, RecurringTransactionTemplateStatus.ACTIVE)

        // when
        mvc.perform(
            delete("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.CATEGORY_IN_USE))

        assertThat(categoryRepository.findByIdAndUserId(category.id, user.id)?.archivedAt)
            .isNull()
    }

    @Test
    fun `should reject archive for category used by non-ended budget`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        createBudgetCategory(category, LocalDate.now(Clock.systemUTC()).withDayOfMonth(1))

        // when
        mvc.perform(
            delete("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.CATEGORY_IN_USE))

        assertThat(categoryRepository.findByIdAndUserId(category.id, user.id)?.archivedAt)
            .isNull()
    }

    @Test
    fun `should archive category with only terminal and historical dependencies`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        val account = createAccount(user.id)
        createRecurringTemplate(account, category, RecurringTransactionTemplateStatus.COMPLETED)
        createBudgetCategory(
            category,
            LocalDate.now(Clock.systemUTC()).withDayOfMonth(1).minusMonths(1),
        )

        // when
        mvc.perform(
            delete("${PathConstants.CATEGORIES}/${category.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(categoryRepository.findByIdAndUserId(category.id, user.id)?.archivedAt)
            .isNotNull()
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

    private fun createAccount(
        userId: UUID,
        currency: Currency = Currency.EUR,
    ): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = "Main card",
                type = AccountType.CARD,
                openingBalance = BigDecimal("1000.00"),
                currency = currency,
                createdAt = OffsetDateTime.now(),
                closedAt = null,
            ),
        )

    private fun createTransaction(
        account: Account,
        category: Category,
        amount: String,
        occurredAt: String,
        deleted: Boolean = false,
    ): Transaction {
        val timestamp = OffsetDateTime.parse(occurredAt)

        return transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = category.id,
                recurringTransactionTemplateId = null,
                type = TransactionType.valueOf(category.type.name),
                amount = BigDecimal(amount),
                merchant = null,
                note = null,
                scheduledFor = null,
                occurredAt = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp,
                deletedAt = if (deleted) timestamp.plusHours(1) else null,
            ),
        )
    }

    private fun assertCategoryCard(
        card: JsonNode,
        transactionCount: Int,
        amount: String,
        percentage: String,
    ) {
        assertThat(card["monthlyTransactionCount"].asInt()).isEqualTo(transactionCount)
        assertThat(card["monthlyAmount"].decimalValue()).isEqualByComparingTo(amount)
        assertThat(card["monthlySharePercentage"].decimalValue()).isEqualByComparingTo(percentage)
    }

    private fun getCategoryCards(
        user: User,
        page: Int = 0,
        size: Int = 20,
        sort: CategoryCardSort? = null,
    ): JsonNode {
        val request = get(PathConstants.CATEGORIES)
            .param("month", MONTH)
            .param("currency", Currency.RUB.name)
            .param("page", page.toString())
            .param("size", size.toString())

        sort?.let { request.param("sort", it.name) }

        val result = mvc.perform(request.cookie(accessTokenCookie(user)))
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsByteArray)
    }

    private fun createRecurringTemplate(
        account: Account,
        category: Category,
        status: RecurringTransactionTemplateStatus,
    ) {
        val today = LocalDate.now(Clock.systemUTC())
        val now = OffsetDateTime.now()
        val template = RecurringTransactionTemplate(
            id = UUID.randomUUID(),
            userId = account.userId,
            accountId = account.id,
            categoryId = category.id,
            name = "Monthly payment",
            type = TransactionType.EXPENSE,
            amount = BigDecimal("50.00"),
            merchant = null,
            note = null,
            status = status,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1.toShort(),
            startDate = today,
            endDate = null,
            lastRunDate = null,
            nextRunDate = if (status == RecurringTransactionTemplateStatus.COMPLETED) null else today,
            createdAt = now,
            updatedAt = now,
        )

        dsl.insertInto(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .set(dsl.newRecord(Tables.RECURRING_TRANSACTION_TEMPLATES, template))
            .execute()
    }

    private fun createBudgetCategory(
        category: Category,
        budgetMonth: LocalDate,
    ) {
        val now = OffsetDateTime.now()
        val budget = Budget(
            id = UUID.randomUUID(),
            userId = category.userId,
            budgetMonth = budgetMonth,
            plannedIncome = BigDecimal("500.00"),
            savingsTarget = BigDecimal("100.00"),
            currency = Currency.EUR,
            createdAt = now,
            updatedAt = now,
        )
        val budgetCategory = BudgetCategory(
            id = UUID.randomUUID(),
            userId = category.userId,
            budgetId = budget.id,
            categoryId = category.id,
            categoryType = CategoryType.EXPENSE,
            limitAmount = BigDecimal("200.00"),
            expenseType = BudgetExpenseType.VARIABLE,
        )

        dsl.insertInto(Tables.BUDGETS)
            .set(dsl.newRecord(Tables.BUDGETS, budget))
            .execute()
        dsl.insertInto(Tables.BUDGET_CATEGORIES)
            .set(dsl.newRecord(Tables.BUDGET_CATEGORIES, budgetCategory))
            .execute()
    }

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

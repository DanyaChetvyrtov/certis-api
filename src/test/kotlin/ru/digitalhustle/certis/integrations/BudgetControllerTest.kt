package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.SaveBudgetAllocationRq
import ru.digitalhustle.certis.dto.request.SaveBudgetRq
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class BudgetControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val BUDGET_MONTH = "2026-08"
        private val PLANNED_INCOME = BigDecimal("1000.00")
        private val SAVINGS_TARGET = BigDecimal("200.00")
        private val LIMIT_AMOUNT = BigDecimal("100.00")
    }

    @Test
    fun `should save monthly budget and calculate same-currency spending`() {
        // given
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val category = createCategory(user.id)
        val rubAccount = createAccount(user.id, Currency.RUB, "Ruble card")
        val eurAccount = createAccount(user.id, Currency.EUR, "Euro card")
        createTransaction(rubAccount, category, BigDecimal("90.00"), "2026-08-10T10:00:00Z")
        createTransaction(eurAccount, category, BigDecimal("70.00"), "2026-08-11T10:00:00Z")
        createTransaction(rubAccount, category, BigDecimal("30.00"), "2026-07-31T10:00:00Z")

        // when
        val result = mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveBudgetRequest(category.id))),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.month").value(BUDGET_MONTH))
            .andExpect(jsonPath("$.monthlyIncome").value(PLANNED_INCOME.toDouble()))
            .andExpect(jsonPath("$.savingsTarget").value(SAVINGS_TARGET.toDouble()))
            .andExpect(jsonPath("$.currency").value(Currency.RUB.name))
            .andExpect(jsonPath("$.allocations.length()").value(1))
            .andExpect(jsonPath("$.allocations[0].categoryId").value(category.id.toString()))
            .andExpect(jsonPath("$.allocations[0].categoryName").value(category.name))
            .andExpect(jsonPath("$.allocations[0].type").value(BudgetExpenseType.VARIABLE.name))
            .andExpect(jsonPath("$.allocations[0].limit").value(LIMIT_AMOUNT.toDouble()))
            .andExpect(jsonPath("$.allocations[0].spent").value(90.0))
            .andExpect(jsonPath("$.allocations[0].status").value(BudgetAllocationStatus.NEAR_LIMIT.name))
            .andReturn()

        val budgetId = UUID.fromString(objectMapper.readTree(result.response.contentAsByteArray)["id"].asText())
        val budget = budgetRepository.findByUserIdAndMonth(user.id, java.time.LocalDate.parse("2026-08-01"))

        assertThat(budget?.id).isEqualTo(budgetId)
        assertThat(
            dsl.fetchCount(
                Tables.BUDGET_CATEGORIES,
                Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(budgetId),
            ),
        ).isEqualTo(1)
    }

    @Test
    fun `should derive currency from preference and preserve historical value`() {
        // given
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.EUR) }
        val category = createCategory(user.id)
        val legacyRequest = saveBudgetRequestWithCurrency(category.id, Currency.RUB)

        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(legacyRequest),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))

        userRepository.save(user.copy(preferredCurrency = Currency.USD))

        // when, then
        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(legacyRequest),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))

        assertThat(
            requireNotNull(
                budgetRepository.findByUserIdAndMonth(
                    user.id,
                    java.time.LocalDate.parse("2026-08-01"),
                ),
            ).currency,
        ).isEqualTo(Currency.EUR)
    }

    @Test
    fun `should update existing monthly budget instead of creating duplicate`() {
        // given
        val user = userFixture.createInDb()
        val firstCategory = createCategory(user.id, "Groceries")
        val secondCategory = createCategory(user.id, "Transport")

        val firstResult = mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveBudgetRequest(firstCategory.id))),
        ).andExpect(status().isOk).andReturn()
        val budgetId = UUID.fromString(objectMapper.readTree(firstResult.response.contentAsByteArray)["id"].asText())

        // when
        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        saveBudgetRequest(
                            categoryId = secondCategory.id,
                            monthlyIncome = BigDecimal("1200.00"),
                        ),
                    ),
                ),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(budgetId.toString()))
            .andExpect(jsonPath("$.monthlyIncome").value(1200.0))
            .andExpect(jsonPath("$.allocations.length()").value(1))
            .andExpect(jsonPath("$.allocations[0].categoryId").value(secondCategory.id.toString()))

        assertThat(dsl.fetchCount(Tables.BUDGETS, Tables.BUDGETS.USER_ID.eq(user.id))).isEqualTo(1)
        assertThat(
            dsl.fetchCount(
                Tables.BUDGET_CATEGORIES,
                Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(budgetId)
                    .and(Tables.BUDGET_CATEGORIES.CATEGORY_ID.eq(firstCategory.id)),
            ),
        ).isZero()
    }

    @Test
    fun `should get own budget and hide another user's budget`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb { copy(email = "another-user@test.com") }
        val category = createCategory(owner.id)

        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveBudgetRequest(category.id))),
        ).andExpect(status().isOk)

        // when, then
        mvc.perform(
            get("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(owner)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value(BUDGET_MONTH))

        mvc.perform(
            get("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(anotherUser)),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Budget not found"))
    }

    @Test
    fun `should reject allocations and savings exceeding income without persisting budget`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        val request = saveBudgetRequest(
            categoryId = category.id,
            monthlyIncome = BigDecimal("250.00"),
        )

        // when
        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value(ErrorMessages.BUDGET_ALLOCATIONS_EXCEED_INCOME))

        assertThat(dsl.fetchCount(Tables.BUDGETS, Tables.BUDGETS.USER_ID.eq(user.id))).isZero()
    }

    @Test
    fun `should reject duplicate and archived allocation categories`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        val duplicateRequest = saveBudgetRequest(category.id).copy(
            allocations = listOf(
                allocationRequest(category.id),
                allocationRequest(category.id),
            ),
        )

        // when, then
        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(duplicateRequest)),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value(ErrorMessages.BUDGET_DUPLICATE_CATEGORIES))

        categoryRepository.archive(category.id, user.id, OffsetDateTime.now())

        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveBudgetRequest(category.id))),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value(ErrorMessages.BUDGET_CATEGORY_INVALID))
    }

    @Test
    fun `should reject malformed month and invalid monetary values`() {
        // given
        val user = userFixture.createInDb()
        val category = createCategory(user.id)
        val invalidRequest = saveBudgetRequest(category.id).copy(
            monthlyIncome = BigDecimal("-1.00"),
        )

        // when, then
        mvc.perform(
            put("${PathConstants.BUDGETS}/2026-13")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveBudgetRequest(category.id))),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            put("${PathConstants.BUDGETS}/$BUDGET_MONTH")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(invalidRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.monthlyIncome").exists())
    }

    private fun saveBudgetRequest(
        categoryId: UUID,
        monthlyIncome: BigDecimal = PLANNED_INCOME,
    ): SaveBudgetRq =
        SaveBudgetRq(
            monthlyIncome = monthlyIncome,
            savingsTarget = SAVINGS_TARGET,
            allocations = listOf(allocationRequest(categoryId)),
        )

    private fun saveBudgetRequestWithCurrency(
        categoryId: UUID,
        currency: Currency,
    ): String =
        """
        {
          "monthlyIncome": $PLANNED_INCOME,
          "savingsTarget": $SAVINGS_TARGET,
          "currency": "${currency.name}",
          "allocations": [
            {
              "categoryId": "$categoryId",
              "type": "${BudgetExpenseType.VARIABLE.name}",
              "limit": $LIMIT_AMOUNT
            }
          ]
        }
        """.trimIndent()

    private fun allocationRequest(categoryId: UUID): SaveBudgetAllocationRq =
        SaveBudgetAllocationRq(
            categoryId = categoryId,
            type = BudgetExpenseType.VARIABLE,
            limit = LIMIT_AMOUNT,
        )

    private fun createCategory(
        userId: UUID,
        name: String = "Groceries",
    ): Category =
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = CategoryType.EXPENSE,
                icon = "shopping-cart",
                color = "#10B981",
                archivedAt = null,
            ),
        )

    private fun createAccount(
        userId: UUID,
        currency: Currency,
        name: String,
    ): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = AccountType.CARD,
                openingBalance = BigDecimal.ZERO,
                currency = currency,
                createdAt = OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                closedAt = null,
            ),
        )

    private fun createTransaction(
        account: Account,
        category: Category,
        amount: BigDecimal,
        occurredAt: String,
    ) {
        val timestamp = OffsetDateTime.parse(occurredAt)

        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = category.id,
                recurringTransactionTemplateId = null,
                type = TransactionType.EXPENSE,
                amount = amount,
                merchant = null,
                note = null,
                scheduledFor = null,
                occurredAt = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp,
                deletedAt = null,
            ),
        )
    }

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

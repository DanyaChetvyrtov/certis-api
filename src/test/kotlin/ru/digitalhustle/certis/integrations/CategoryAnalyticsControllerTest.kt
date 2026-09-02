package ru.digitalhustle.certis.integrations

import com.fasterxml.jackson.databind.JsonNode
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class CategoryAnalyticsControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val MONTH = "2026-09"
        private const val ICON = "wallet"
        private val ANALYTICS_PATH = PathConstants.CATEGORIES + PathConstants.CATEGORY_ANALYTICS
        private val OCCURRED_AT = OffsetDateTime.parse("2026-09-15T12:00:00Z")
    }

    @Test
    fun `should require authentication`() {
        // when
        mvc.perform(
            get(ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", CategoryType.EXPENSE.name),
        )
            // then
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should calculate expense analytics with requested filters and top limit`() {
        // given
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.USD) }
        val groceries = createExpenseAnalyticsScenario(user)

        // when
        val response = getAnalytics(user, CategoryType.EXPENSE, topLimit = 1)

        // then
        assertThat(response["month"].asText()).isEqualTo(MONTH)
        assertThat(response["currency"].asText()).isEqualTo(Currency.RUB.name)
        assertThat(response["type"].asText()).isEqualTo(CategoryType.EXPENSE.name)
        assertThat(response["totalTransactionCount"].asInt()).isEqualTo(4)
        assertThat(response["categorizedTransactionCount"].asInt()).isEqualTo(3)
        assertThat(response["uncategorizedTransactionCount"].asInt()).isEqualTo(1)
        assertThat(response["totalSum"].decimalValue()).isEqualByComparingTo("200.00")
        assertThat(response["categorizedSum"].decimalValue()).isEqualByComparingTo("150.00")
        assertThat(response["uncategorizedSum"].decimalValue()).isEqualByComparingTo("50.00")
        assertThat(response["coveragePercentage"].decimalValue()).isEqualByComparingTo("75.00")
        assertThat(response["topExpenseCategories"].size()).isEqualTo(1)
        assertTopCategory(response["topExpenseCategories"][0], groceries, "100.00", "50.00")
    }

    @Test
    fun `should calculate analytics for income type`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Income account", Currency.RUB)
        val salary = createCategory(user.id, "Salary", CategoryType.INCOME, "#10B981")
        val expense = createCategory(user.id, "Food", CategoryType.EXPENSE, "#E6655A")
        createTransaction(account, TransactionType.INCOME, "300.00", salary)
        createTransaction(account, TransactionType.INCOME, "100.00")
        createTransaction(account, TransactionType.EXPENSE, "900.00", expense)

        // when
        val response = getAnalytics(user, CategoryType.INCOME)

        // then
        assertThat(response["type"].asText()).isEqualTo(CategoryType.INCOME.name)
        assertThat(response["totalTransactionCount"].asInt()).isEqualTo(2)
        assertThat(response["categorizedTransactionCount"].asInt()).isEqualTo(1)
        assertThat(response["uncategorizedTransactionCount"].asInt()).isEqualTo(1)
        assertThat(response["totalSum"].decimalValue()).isEqualByComparingTo("400.00")
        assertThat(response["categorizedSum"].decimalValue()).isEqualByComparingTo("300.00")
        assertThat(response["uncategorizedSum"].decimalValue()).isEqualByComparingTo("100.00")
        assertThat(response["coveragePercentage"].decimalValue()).isEqualByComparingTo("75.00")
        assertThat(response["topExpenseCategories"].size()).isEqualTo(1)
        assertTopCategory(response["topExpenseCategories"][0], salary, "300.00", "75.00")
    }

    @Test
    fun `should return null coverage when there are no transactions`() {
        // given
        val user = userFixture.createInDb()

        // when
        val response = getAnalytics(user, CategoryType.EXPENSE)

        // then
        assertThat(response["totalTransactionCount"].asInt()).isZero()
        assertThat(response["categorizedTransactionCount"].asInt()).isZero()
        assertThat(response["uncategorizedTransactionCount"].asInt()).isZero()
        assertThat(response["totalSum"].decimalValue()).isEqualByComparingTo("0.00")
        assertThat(response["categorizedSum"].decimalValue()).isEqualByComparingTo("0.00")
        assertThat(response["uncategorizedSum"].decimalValue()).isEqualByComparingTo("0.00")
        assertThat(response["coveragePercentage"].isNull).isTrue()
        assertThat(response["topExpenseCategories"].isEmpty).isTrue()
    }

    @Test
    fun `should reject invalid analytics parameters`() {
        // given
        val user = userFixture.createInDb()

        // when, then
        performAnalytics(user, topLimit = "0")
            .andExpect(status().isBadRequest)

        performAnalytics(user, topLimit = "101")
            .andExpect(status().isBadRequest)

        mvc.perform(
            get(ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", "TRANSFER")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should require month currency and type`() {
        // given
        val user = userFixture.createInDb()

        // when, then
        mvc.perform(
            get(ANALYTICS_PATH)
                .param("currency", Currency.RUB.name)
                .param("type", CategoryType.EXPENSE.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(ANALYTICS_PATH)
                .param("month", MONTH)
                .param("type", CategoryType.EXPENSE.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)
    }

    private fun createExpenseAnalyticsScenario(user: User): Category {
        val groceries = createCategory(user.id, "Groceries", CategoryType.EXPENSE, "#E6655A")
        val housing = createCategory(user.id, "Housing", CategoryType.EXPENSE, "#B08D57")
        val salary = createCategory(user.id, "Salary", CategoryType.INCOME, "#10B981")
        val rubAccount = createAccount(user.id, "RUB card", Currency.RUB)
        val rubSavings = createAccount(user.id, "RUB savings", Currency.RUB)
        val eurAccount = createAccount(user.id, "EUR card", Currency.EUR)

        createTransaction(rubAccount, TransactionType.EXPENSE, "80.00", groceries)
        createTransaction(rubAccount, TransactionType.EXPENSE, "20.00", groceries)
        createTransaction(rubAccount, TransactionType.EXPENSE, "50.00", housing)
        createTransaction(rubAccount, TransactionType.EXPENSE, "50.00")
        createTransaction(rubAccount, TransactionType.INCOME, "300.00", salary)
        createTransaction(eurAccount, TransactionType.EXPENSE, "500.00", groceries)
        createTransaction(
            rubAccount,
            TransactionType.EXPENSE,
            "700.00",
            groceries,
            occurredAt = OffsetDateTime.parse("2026-10-01T00:00:00Z"),
        )
        createTransaction(rubAccount, TransactionType.EXPENSE, "900.00", groceries, deleted = true)
        createTransferPosting(user, rubAccount, rubSavings)
        createAnotherUserTransaction()

        return groceries
    }

    private fun createAnotherUserTransaction() {
        val anotherUser = userFixture.createInDb { copy(email = "analytics-other-user@test.com") }
        val account = createAccount(anotherUser.id, "Other card", Currency.RUB)
        val category = createCategory(anotherUser.id, "Groceries", CategoryType.EXPENSE, "#E6655A")

        createTransaction(account, TransactionType.EXPENSE, "1000.00", category)
    }

    private fun createCategory(
        userId: UUID,
        name: String,
        type: CategoryType,
        color: String,
    ): Category =
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = type,
                icon = ICON,
                color = color,
                archivedAt = null,
            ),
        )

    private fun createAccount(
        userId: UUID,
        name: String,
        currency: Currency,
    ): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = AccountType.CARD,
                openingBalance = BigDecimal("1000.00"),
                currency = currency,
                createdAt = OCCURRED_AT,
                closedAt = null,
            ),
        )

    private fun createTransaction(
        account: Account,
        type: TransactionType,
        amount: String,
        category: Category? = null,
        occurredAt: OffsetDateTime = OCCURRED_AT,
        deleted: Boolean = false,
        transferId: UUID? = null,
    ): Transaction =
        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = category?.id,
                recurringTransactionTemplateId = null,
                type = type,
                amount = BigDecimal(amount),
                merchant = null,
                note = null,
                scheduledFor = null,
                occurredAt = occurredAt,
                createdAt = occurredAt,
                updatedAt = occurredAt,
                deletedAt = if (deleted) occurredAt.plusHours(1) else null,
                transferId = transferId,
            ),
        )

    private fun createTransferPosting(
        user: User,
        sourceAccount: Account,
        destinationAccount: Account,
    ) {
        val transfer = transferRepository.insert(
            Transfer(
                id = UUID.randomUUID(),
                userId = user.id,
                sourceAccountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                currency = Currency.RUB,
                amount = BigDecimal("1000.00"),
                note = null,
                occurredAt = OCCURRED_AT,
                createdAt = OCCURRED_AT,
            ),
        )

        createTransaction(
            account = sourceAccount,
            type = TransactionType.EXPENSE,
            amount = "1000.00",
            transferId = transfer.id,
        )
    }

    private fun getAnalytics(
        user: User,
        type: CategoryType,
        topLimit: Int? = null,
    ): JsonNode {
        val result = performAnalytics(user, type.name, topLimit?.toString())
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsByteArray)
    }

    private fun performAnalytics(
        user: User,
        type: String = CategoryType.EXPENSE.name,
        topLimit: String? = null,
    ) =
        mvc.perform(
            get(ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", type)
                .apply { topLimit?.let { param("topLimit", it) } }
                .cookie(accessTokenCookie(user)),
        )

    private fun assertTopCategory(
        item: JsonNode,
        category: Category,
        amount: String,
        percentage: String,
    ) {
        assertThat(item["categoryId"].asText()).isEqualTo(category.id.toString())
        assertThat(item["name"].asText()).isEqualTo(category.name)
        assertThat(item["color"].asText()).isEqualTo(category.color)
        assertThat(item["amount"].decimalValue()).isEqualByComparingTo(amount)
        assertThat(item["sharePercentage"].decimalValue()).isEqualByComparingTo(percentage)
    }

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

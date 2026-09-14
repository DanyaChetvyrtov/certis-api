package ru.digitalhustle.certis.integrations

import com.fasterxml.jackson.databind.JsonNode
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.features.account.enums.AccountType
import ru.digitalhustle.certis.features.account.model.Account
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.model.Category
import ru.digitalhustle.certis.features.security.constants.SecurityConstants
import ru.digitalhustle.certis.features.security.model.User
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.features.transaction.model.Transfer
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class CategorySpendingOverTimeControllerTest : AbstractIntegrationTest() {

    @Test
    fun `should require authentication`() {
        // when
        mvc.perform(
            get(SPENDING_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", TransactionType.EXPENSE.name),
        )
            // then
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return top category series and aggregate remaining spending into other`() {
        // given
        val user = userFixture.createInDb()
        val food = createExpenseScenario(user)

        // when
        val response = getSpending(user, bucketCount = 6, topLimit = 2)

        // then
        assertThat(response["month"].asText()).isEqualTo(MONTH)
        assertThat(response["currency"].asText()).isEqualTo(Currency.RUB.name)
        assertThat(response["type"].asText()).isEqualTo(TransactionType.EXPENSE.name)
        assertThat(response["totalSum"].decimalValue()).isEqualByComparingTo("350.00")
        assertThat(response["series"].size()).isEqualTo(3)
        assertSeries(
            item = response["series"][0],
            category = food,
            total = "150.00",
            amounts = listOf("60.00", "0.00", "0.00", "0.00", "0.00", "90.00"),
        )
        assertThat(response["series"][1]["categoryName"].asText()).isEqualTo("Housing")
        assertThat(response["series"][1]["total"].decimalValue()).isEqualByComparingTo("120.00")
        assertOtherSeries(response["series"][2])
    }

    @Test
    fun `should use defaults and return uncategorized spending as other`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "RUB card", Currency.RUB)
        createTransaction(
            account = account,
            type = TransactionType.EXPENSE,
            amount = "25.00",
            occurredAt = OffsetDateTime.parse("2026-09-10T10:00:00Z"),
        )

        // when
        val response = getSpending(user)

        // then
        assertThat(response["totalSum"].decimalValue()).isEqualByComparingTo("25.00")
        assertThat(response["series"].size()).isEqualTo(1)
        val other = response["series"][0]
        assertThat(other["categoryId"].isNull).isTrue()
        assertThat(other["categoryName"].asText()).isEqualTo("Other")
        assertThat(other["categoryColor"].isNull).isTrue()
        assertThat(other["points"].size()).isEqualTo(6)
        assertThat(other["points"].map { point -> point["bucketMonth"].asText() })
            .containsExactly("2026-04", "2026-05", "2026-06", "2026-07", "2026-08", "2026-09")
    }

    @Test
    fun `should derive totals from rounded monthly points`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Fractional RUB card", Currency.RUB)
        val category = createCategory(user.id, "Fractional spending", "#8892B0")
        createTransaction(account, TransactionType.EXPENSE, "0.0049", category, date("2026-04-10"))
        createTransaction(account, TransactionType.EXPENSE, "0.0049", category, date("2026-05-10"))

        // when
        val response = getSpending(user, bucketCount = 6, topLimit = 1)

        // then
        assertThat(response["totalSum"].decimalValue()).isEqualByComparingTo("0.00")
        assertThat(response["series"].size()).isEqualTo(1)
        val series = response["series"][0]
        assertThat(series["total"].decimalValue()).isEqualByComparingTo("0.00")
        assertAmounts(series, listOf("0.00", "0.00", "0.00", "0.00", "0.00", "0.00"))
    }

    @Test
    fun `should reject invalid parameters`() {
        // given
        val user = userFixture.createInDb()

        // when, then
        performSpending(user, bucketCount = "0").andExpect(status().isBadRequest)
        performSpending(user, bucketCount = "25").andExpect(status().isBadRequest)
        performSpending(user, topLimit = "0").andExpect(status().isBadRequest)
        performSpending(user, topLimit = "11").andExpect(status().isBadRequest)
        performSpending(user, type = "TRANSFER").andExpect(status().isBadRequest)
    }

    @Test
    fun `should require month currency and type`() {
        // given
        val user = userFixture.createInDb()

        // when, then
        mvc.perform(
            get(SPENDING_PATH)
                .param("currency", Currency.RUB.name)
                .param("type", TransactionType.EXPENSE.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)
        mvc.perform(
            get(SPENDING_PATH)
                .param("month", MONTH)
                .param("type", TransactionType.EXPENSE.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)
        mvc.perform(
            get(SPENDING_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isBadRequest)
    }

    private fun createExpenseScenario(user: User): Category {
        val food = createCategory(user.id, "Food", "#E6655A")
        val housing = createCategory(user.id, "Housing", "#B08D57")
        val transport = createCategory(user.id, "Transport", "#5890E6")
        val rubAccount = createAccount(user.id, "RUB card", Currency.RUB)
        val rubSavings = createAccount(user.id, "RUB savings", Currency.RUB)
        val eurAccount = createAccount(user.id, "EUR card", Currency.EUR)

        createTransaction(rubAccount, TransactionType.EXPENSE, "60.00", food, date("2026-04-10"))
        createTransaction(rubAccount, TransactionType.EXPENSE, "120.00", housing, date("2026-05-10"))
        createTransaction(rubAccount, TransactionType.EXPENSE, "50.00", transport, date("2026-06-10"))
        createTransaction(rubAccount, TransactionType.EXPENSE, "30.00", occurredAt = date("2026-07-10"))
        createTransaction(rubAccount, TransactionType.EXPENSE, "90.00", food, date("2026-09-10"))

        createTransaction(rubAccount, TransactionType.EXPENSE, "500.00", food, date("2026-03-31"))
        createTransaction(rubAccount, TransactionType.EXPENSE, "700.00", food, date("2026-10-01"))
        createTransaction(rubAccount, TransactionType.INCOME, "300.00", occurredAt = date("2026-08-10"))
        createTransaction(eurAccount, TransactionType.EXPENSE, "400.00", food, date("2026-08-10"))
        createTransaction(rubAccount, TransactionType.EXPENSE, "900.00", food, date("2026-08-10"), deleted = true)
        createTransferPosting(user, rubAccount, rubSavings)
        createAnotherUserTransaction()

        return food
    }

    private fun createAnotherUserTransaction() {
        val user = userFixture.createInDb { copy(email = "spending-other-user@test.com") }
        val account = createAccount(user.id, "Other RUB card", Currency.RUB)
        val category = createCategory(user.id, "Food", "#E6655A")
        createTransaction(account, TransactionType.EXPENSE, "1100.00", category, date("2026-08-10"))
    }

    private fun createCategory(
        userId: UUID,
        name: String,
        color: String,
    ): Category =
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = CategoryType.EXPENSE,
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
                createdAt = date("2026-01-01"),
                closedAt = null,
            ),
        )

    private fun createTransaction(
        account: Account,
        type: TransactionType,
        amount: String,
        category: Category? = null,
        occurredAt: OffsetDateTime,
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
        val occurredAt = date("2026-08-10")
        val transfer = transferRepository.insert(
            Transfer(
                id = UUID.randomUUID(),
                userId = user.id,
                sourceAccountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                currency = Currency.RUB,
                amount = BigDecimal("1000.00"),
                note = null,
                occurredAt = occurredAt,
                createdAt = occurredAt,
            ),
        )
        createTransaction(
            account = sourceAccount,
            type = TransactionType.EXPENSE,
            amount = "1000.00",
            occurredAt = occurredAt,
            transferId = transfer.id,
        )
    }

    private fun getSpending(
        user: User,
        bucketCount: Int? = null,
        topLimit: Int? = null,
    ): JsonNode {
        val result = performSpending(
            user = user,
            bucketCount = bucketCount?.toString(),
            topLimit = topLimit?.toString(),
        ).andExpect(status().isOk).andReturn()

        return objectMapper.readTree(result.response.contentAsByteArray)
    }

    private fun performSpending(
        user: User,
        bucketCount: String? = null,
        topLimit: String? = null,
        type: String = TransactionType.EXPENSE.name,
    ) =
        mvc.perform(
            get(SPENDING_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", type)
                .apply { bucketCount?.let { param("bucketCount", it) } }
                .apply { topLimit?.let { param("topLimit", it) } }
                .cookie(accessTokenCookie(user)),
        )

    private fun assertSeries(
        item: JsonNode,
        category: Category,
        total: String,
        amounts: List<String>,
    ) {
        assertThat(item["categoryId"].asText()).isEqualTo(category.id.toString())
        assertThat(item["categoryName"].asText()).isEqualTo(category.name)
        assertThat(item["categoryColor"].asText()).isEqualTo(category.color)
        assertThat(item["total"].decimalValue()).isEqualByComparingTo(total)
        assertAmounts(item, amounts)
    }

    private fun assertOtherSeries(item: JsonNode) {
        assertThat(item["categoryId"].isNull).isTrue()
        assertThat(item["categoryName"].asText()).isEqualTo("Other")
        assertThat(item["categoryColor"].isNull).isTrue()
        assertThat(item["total"].decimalValue()).isEqualByComparingTo("80.00")
        assertAmounts(item, listOf("0.00", "0.00", "50.00", "30.00", "0.00", "0.00"))
    }

    private fun assertAmounts(
        item: JsonNode,
        expectedAmounts: List<String>,
    ) {
        val actualAmounts = item["points"].map { point -> point["amount"].decimalValue() }
        assertThat(actualAmounts).hasSize(expectedAmounts.size)
        expectedAmounts.forEachIndexed { index, expectedAmount ->
            assertThat(actualAmounts[index]).isEqualByComparingTo(expectedAmount)
        }
    }

    private fun date(value: String): OffsetDateTime = OffsetDateTime.parse("${value}T12:00:00Z")

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )

    private companion object {
        const val MONTH = "2026-09"
        const val ICON = "wallet"
        val SPENDING_PATH: String = PathConstants.CATEGORIES + PathConstants.CATEGORY_SPENDING_OVER_TIME
    }
}

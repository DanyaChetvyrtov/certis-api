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
import ru.digitalhustle.certis.enums.CashFlowGranularity
import ru.digitalhustle.certis.enums.CashFlowRange
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransactionAnalyticsControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val MONTH = "2026-09"
        private const val ANCHOR_DATE = "2026-09-04"
        private const val TIME_ZONE = "Europe/Moscow"
        private val MONTHLY_ANALYTICS_PATH = PathConstants.TRANSACTIONS + PathConstants.TRANSACTION_ANALYTICS_MONTHLY
        private val CASH_FLOW_ANALYTICS_PATH =
            PathConstants.TRANSACTIONS + PathConstants.TRANSACTION_ANALYTICS_CASH_FLOW
        private val OCCURRED_AT = OffsetDateTime.parse("2026-09-15T12:00:00Z")
    }

    @Test
    fun `should require authentication`() {
        // when
        mvc.perform(
            get(MONTHLY_ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name),
        )
            // then
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should calculate monthly analytics for requested currency`() {
        // given
        val user = userFixture.createInDb()
        createAnalyticsScenario(user)

        // when
        val result = mvc.perform(
            get(MONTHLY_ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andReturn()
        val response = objectMapper.readTree(result.response.contentAsByteArray)

        assertThat(response["month"].asText()).isEqualTo(MONTH)
        assertThat(response["currency"].asText()).isEqualTo(Currency.RUB.name)
        assertMonthlyTotal(response["income"], 2, "120000.00")
        assertMonthlyTotal(response["expenses"], 2, "47500.00")
        assertThat(response["netCashFlow"].decimalValue()).isEqualByComparingTo("72500.00")
    }

    @Test
    fun `should return zero totals when there are no transactions`() {
        // given
        val user = userFixture.createInDb()

        // when
        val result = mvc.perform(
            get(MONTHLY_ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andReturn()
        val response = objectMapper.readTree(result.response.contentAsByteArray)

        assertMonthlyTotal(response["income"], 0, "0.00")
        assertMonthlyTotal(response["expenses"], 0, "0.00")
        assertThat(response["netCashFlow"].decimalValue()).isEqualByComparingTo("0.00")
    }

    @Test
    fun `should reject missing or invalid analytics parameters`() {
        // given
        val user = userFixture.createInDb()
        val cookie = accessTokenCookie(user)

        // when, then
        mvc.perform(
            get(MONTHLY_ANALYTICS_PATH)
                .param("currency", Currency.RUB.name)
                .cookie(cookie),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(MONTHLY_ANALYTICS_PATH)
                .param("month", MONTH)
                .cookie(cookie),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(MONTHLY_ANALYTICS_PATH)
                .param("month", "2026-13")
                .param("currency", Currency.RUB.name)
                .cookie(cookie),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            get(MONTHLY_ANALYTICS_PATH)
                .param("month", MONTH)
                .param("currency", "GBP")
                .cookie(cookie),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should calculate cash flow in requested time zone and fill empty buckets`() {
        // given
        val user = userFixture.createInDb()
        createAnalyticsScenario(user)
        val historyAccount = createAccount(user.id, "RUB history", Currency.RUB)
        createTransaction(
            account = historyAccount,
            type = TransactionType.INCOME,
            amount = "500.00",
            occurredAt = OffsetDateTime.parse("2026-03-31T21:30:00Z"),
        )

        // when
        val result = mvc.perform(
            cashFlowRequest(CashFlowRange.SIX_MONTHS)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andReturn()
        val response = objectMapper.readTree(result.response.contentAsByteArray)

        assertThat(response["range"].asText()).isEqualTo(CashFlowRange.SIX_MONTHS.name)
        assertThat(response["currency"].asText()).isEqualTo(Currency.RUB.name)
        assertThat(response["granularity"].asText()).isEqualTo(CashFlowGranularity.MONTH.name)
        assertThat(response["from"].asText()).isEqualTo("2026-04-01T00:00:00+03:00")
        assertThat(response["toExclusive"].asText()).isEqualTo("2026-10-01T00:00:00+03:00")
        assertCashFlowAmounts(response["totals"], "120500.00", "47500.00", "73000.00")

        val points = response["points"]
        assertThat(points).hasSize(6)
        assertThat(points.map { point -> point["bucketStart"].asText() }).containsExactly(
            "2026-04-01T00:00:00+03:00",
            "2026-05-01T00:00:00+03:00",
            "2026-06-01T00:00:00+03:00",
            "2026-07-01T00:00:00+03:00",
            "2026-08-01T00:00:00+03:00",
            "2026-09-01T00:00:00+03:00",
        )
        assertCashFlowAmounts(points[0], "500.00", "0.00", "500.00")
        assertCashFlowAmounts(points[1], "0.00", "0.00", "0.00")
        assertCashFlowAmounts(points[5], "120000.00", "47500.00", "72500.00")
    }

    @Test
    fun `should return all zero hourly buckets when cash flow has no transactions`() {
        // given
        val user = userFixture.createInDb()

        // when
        val result = mvc.perform(
            cashFlowRequest(CashFlowRange.DAY)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andReturn()
        val response = objectMapper.readTree(result.response.contentAsByteArray)

        assertThat(response["granularity"].asText()).isEqualTo(CashFlowGranularity.HOUR.name)
        assertThat(response["points"]).hasSize(24)
        assertThat(response["points"].first()["bucketStart"].asText())
            .isEqualTo("2026-09-04T00:00:00+03:00")
        assertCashFlowAmounts(response["totals"], "0.00", "0.00", "0.00")
        response["points"].forEach { point ->
            assertCashFlowAmounts(point, "0.00", "0.00", "0.00")
        }
    }

    @Test
    fun `should require authentication for cash flow analytics`() {
        // when
        mvc.perform(cashFlowRequest(CashFlowRange.MONTH))
            // then
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject missing or invalid cash flow parameters`() {
        // given
        val user = userFixture.createInDb()
        val cookie = accessTokenCookie(user)

        // when, then
        mvc.perform(
            get(CASH_FLOW_ANALYTICS_PATH)
                .param("currency", Currency.RUB.name)
                .param("anchorDate", ANCHOR_DATE)
                .param("timeZone", TIME_ZONE)
                .cookie(cookie),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            cashFlowRequest(range = "QUARTER")
                .cookie(cookie),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            cashFlowRequest(
                range = CashFlowRange.MONTH.name,
                anchorDate = "2026-13-40",
            )
                .cookie(cookie),
        ).andExpect(status().isBadRequest)

        mvc.perform(
            cashFlowRequest(
                range = CashFlowRange.MONTH.name,
                timeZone = "Invalid/Zone",
            )
                .cookie(cookie),
        ).andExpect(status().isBadRequest)
    }

    private fun createAnalyticsScenario(user: User) {
        val rubAccount = createAccount(user.id, "RUB card", Currency.RUB)
        val rubSavings = createAccount(user.id, "RUB savings", Currency.RUB)
        val eurAccount = createAccount(user.id, "EUR card", Currency.EUR)

        createTransaction(rubAccount, TransactionType.INCOME, "100000.00")
        createTransaction(rubAccount, TransactionType.INCOME, "20000.00")
        createTransaction(rubAccount, TransactionType.EXPENSE, "45000.00")
        createTransaction(rubSavings, TransactionType.EXPENSE, "2500.00")

        createTransaction(eurAccount, TransactionType.INCOME, "99999.00")
        createTransaction(
            rubAccount,
            TransactionType.EXPENSE,
            "999.00",
            occurredAt = OffsetDateTime.parse("2026-10-01T00:00:00Z"),
        )
        createTransaction(rubAccount, TransactionType.EXPENSE, "888.00", deleted = true)
        createTransferPostings(user, rubAccount, rubSavings)
        createAnotherUserTransaction()
    }

    private fun createAnotherUserTransaction() {
        val user = userFixture.createInDb { copy(email = "analytics-other-user@test.com") }
        val account = createAccount(user.id, "Other RUB card", Currency.RUB)

        createTransaction(account, TransactionType.INCOME, "50000.00")
    }

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
        occurredAt: OffsetDateTime = OCCURRED_AT,
        deleted: Boolean = false,
        transferId: UUID? = null,
    ): Transaction =
        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = null,
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

    private fun createTransferPostings(
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

        createTransaction(sourceAccount, TransactionType.EXPENSE, "1000.00", transferId = transfer.id)
        createTransaction(destinationAccount, TransactionType.INCOME, "1000.00", transferId = transfer.id)
    }

    private fun assertMonthlyTotal(
        total: JsonNode,
        transactionCount: Int,
        amount: String,
    ) {
        assertThat(total["transactionCount"].asInt()).isEqualTo(transactionCount)
        assertThat(total["amount"].decimalValue()).isEqualByComparingTo(amount)
    }

    private fun assertCashFlowAmounts(
        amounts: JsonNode,
        income: String,
        expenses: String,
        netCashFlow: String,
    ) {
        assertThat(amounts["income"].decimalValue()).isEqualByComparingTo(income)
        assertThat(amounts["expenses"].decimalValue()).isEqualByComparingTo(expenses)
        assertThat(amounts["netCashFlow"].decimalValue()).isEqualByComparingTo(netCashFlow)
    }

    private fun cashFlowRequest(range: CashFlowRange) = cashFlowRequest(range.name)

    private fun cashFlowRequest(
        range: String,
        anchorDate: String = ANCHOR_DATE,
        timeZone: String = TIME_ZONE,
    ) =
        get(CASH_FLOW_ANALYTICS_PATH)
            .param("range", range)
            .param("currency", Currency.RUB.name)
            .param("anchorDate", anchorDate)
            .param("timeZone", timeZone)

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

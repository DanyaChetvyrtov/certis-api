package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.dto.BudgetForecastOperationType
import ru.digitalhustle.certis.api.dto.request.BudgetForecastManualAdjustmentRq
import ru.digitalhustle.certis.api.dto.request.ConfirmBudgetForecastRq
import ru.digitalhustle.certis.api.dto.request.CreateBudgetPlanRq
import ru.digitalhustle.certis.api.dto.response.BudgetForecastPreviewRs
import ru.digitalhustle.certis.api.dto.response.BudgetForecastRs
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.features.account.enums.AccountType
import ru.digitalhustle.certis.features.account.model.Account
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.model.Category
import ru.digitalhustle.certis.features.security.model.User
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

class BudgetForecastControllerTest : AbstractIntegrationTest() {

    @Test
    @Suppress("LongMethod")
    fun `should preview confirm read and stale a forecast`() {
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val account = createAccount(user)
        val salaryCategory = createCategory(user, "Salary", CategoryType.INCOME)
        val rentCategory = createCategory(user, "Rent", CategoryType.EXPENSE)
        val manualCategory = createCategory(user, "Groceries", CategoryType.EXPENSE)
        val salary = createRecurring(user, account, salaryCategory, "Salary", "185000.00", 5)
        createRecurring(user, account, rentCategory, "Rent", "55000.00", 1)
        val plan = createPlan(user)

        val previewResult = mvc.perform(
            get(forecastPath(plan.id, PathConstants.BUDGET_PLAN_FORECAST_PREVIEW))
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.planId").value(plan.id.toString()))
            .andExpect(jsonPath("$.basedOnPlanVersion").value(0))
            .andExpect(jsonPath("$.summary.forecastIncome").value(185000.0))
            .andExpect(jsonPath("$.summary.forecastExpenses").value(55000.0))
            .andExpect(jsonPath("$.items.length()").value(2))
        val preview = getBody(previewResult, BudgetForecastPreviewRs::class.java)
        assertThat(
            dsl.fetchCount(
                Tables.BUDGET_FORECAST_REVISIONS,
                Tables.BUDGET_FORECAST_REVISIONS.PLAN_ID.eq(plan.id),
            ),
        ).isZero()

        val confirmedResult = mvc.perform(
            put(forecastPath(plan.id, PathConstants.BUDGET_PLAN_FORECAST))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        ConfirmBudgetForecastRq(
                            expectedVersion = 0,
                            sourceFingerprint = preview.sourceFingerprint,
                            manualAdjustments = listOf(
                                BudgetForecastManualAdjustmentRq(
                                    clientId = UUID.randomUUID(),
                                    operationType = BudgetForecastOperationType.EXPENSE,
                                    title = "Groceries adjustment",
                                    categoryId = manualCategory.id,
                                    expectedDate = null,
                                    amount = BigDecimal("10000.00"),
                                ),
                            ),
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.revision").value(1))
            .andExpect(jsonPath("$.status").value("CURRENT"))
            .andExpect(jsonPath("$.planVersion").value(1))
            .andExpect(jsonPath("$.summary.forecastSavings").value(120000.0))
            .andExpect(jsonPath("$.items.length()").value(3))
        val confirmed = getBody(confirmedResult, BudgetForecastRs::class.java)
        assertThat(confirmed.inputFingerprint).startsWith("sha256:")
        assertThat(
            dsl.fetchCount(
                Tables.BUDGET_FORECAST_REVISIONS,
                Tables.BUDGET_FORECAST_REVISIONS.PLAN_ID.eq(plan.id),
            ),
        ).isEqualTo(1)

        mvc.perform(
            get(forecastPath(plan.id, PathConstants.BUDGET_PLAN_FORECAST))
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CURRENT"))
            .andExpect(jsonPath("$.summary.forecastSavings").value(120000.0))

        recurringTransactionTemplateRepository.update(
            salary.copy(
                amount = BigDecimal("190000.00"),
                updatedAt = salary.updatedAt.plusSeconds(1),
            ),
        )
        mvc.perform(
            get(forecastPath(plan.id, PathConstants.BUDGET_PLAN_FORECAST))
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("STALE"))
    }

    private fun createPlan(user: User): BudgetPlanRs {
        val result = mvc.perform(
            post(PathConstants.BUDGET_PLANS)
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "forecast-plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        CreateBudgetPlanRq(YearMonth.parse(TARGET_MONTH), Currency.RUB),
                    ),
                ),
        ).andExpect(status().isCreated)
        return getBody(result, BudgetPlanRs::class.java)
    }

    private fun createAccount(user: User): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = user.id,
                name = "Primary",
                type = AccountType.BANK,
                openingBalance = BigDecimal.ZERO,
                currency = Currency.RUB,
                createdAt = NOW,
                closedAt = null,
            ),
        )

    private fun createCategory(user: User, name: String, type: CategoryType): Category =
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = user.id,
                name = name,
                type = type,
                icon = "wallet",
                color = "#10B981",
                archivedAt = null,
            ),
        )

    private fun createRecurring(
        user: User,
        account: Account,
        category: Category,
        name: String,
        amount: String,
        day: Int,
    ): RecurringTransactionTemplate {
        val scheduledFor = LocalDate.parse("$TARGET_MONTH-${day.toString().padStart(2, '0')}")
        return recurringTransactionTemplateRepository.insert(
            RecurringTransactionTemplate(
                id = UUID.randomUUID(),
                userId = user.id,
                accountId = account.id,
                categoryId = category.id,
                name = name,
                type = TransactionType.valueOf(category.type.name),
                amount = BigDecimal(amount),
                merchant = null,
                note = null,
                status = RecurringTransactionTemplateStatus.ACTIVE,
                frequency = RecurringTransactionFrequency.MONTHLY,
                intervalCount = 1,
                startDate = scheduledFor,
                endDate = null,
                lastRunDate = null,
                nextRunDate = scheduledFor,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        )
    }

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(ACCESS_TOKEN_COOKIE, jwtTokenProvider.createAccessToken(user.id, user.email))

    private fun forecastPath(planId: UUID, template: String): String =
        PathConstants.BUDGET_PLANS + template.replace("{planId}", planId.toString())

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val TARGET_MONTH = "2026-10"
        private val NOW = OffsetDateTime.parse("2026-09-15T12:00:00Z")
    }
}

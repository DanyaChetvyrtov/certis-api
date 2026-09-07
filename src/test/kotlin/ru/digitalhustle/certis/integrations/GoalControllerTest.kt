package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.CreateGoalContributionRq
import ru.digitalhustle.certis.dto.request.CreateGoalRq
import ru.digitalhustle.certis.dto.request.GoalContributionPlanRq
import ru.digitalhustle.certis.dto.request.GoalPlanPreviewRq
import ru.digitalhustle.certis.dto.request.InitialGoalContributionRq
import ru.digitalhustle.certis.dto.request.UpdateGoalRq
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalContributionPlanType
import ru.digitalhustle.certis.enums.GoalStatus
import ru.digitalhustle.certis.enums.GoalTransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

class GoalControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private val TARGET_MONTH = YearMonth.now(Clock.systemUTC()).plusMonths(5)
    }

    @Test
    fun `should preview recommended contribution plan`() {
        val user = userFixture.createInDb()
        val targetMonth = YearMonth.now(Clock.systemUTC()).plusMonths(2)

        mvc.perform(
            post(PathConstants.GOALS + PathConstants.GOAL_PLAN_PREVIEW)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        GoalPlanPreviewRq(
                            targetAmount = BigDecimal("100.00"),
                            initialAmount = BigDecimal("40.00"),
                            currency = Currency.EUR,
                            targetMonth = targetMonth,
                            contributionPlan = GoalContributionPlanRq(GoalContributionPlanType.RECOMMENDED),
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.remainingAmount").value(60.0))
            .andExpect(jsonPath("$.progressPercentage").value(40.0))
            .andExpect(jsonPath("$.contributionMonths").value(3))
            .andExpect(jsonPath("$.recommendedMonthlyAmount").value(20.0))
            .andExpect(jsonPath("$.selectedMonthlyAmount").value(20.0))
            .andExpect(jsonPath("$.targetMonth").value(targetMonth.toString()))
    }

    @Test
    fun `should create goal with account backed initial contribution and expose it in queries`() {
        val user = userFixture.createInDb()
        val account = createAccount(user.id)

        val result = mvc.perform(
            post(PathConstants.GOALS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createGoalRequest(account.id))),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Emergency fund"))
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))
            .andExpect(jsonPath("$.targetAmount").value(200.0))
            .andExpect(jsonPath("$.savedAmount").value(50.0))
            .andExpect(jsonPath("$.remainingAmount").value(150.0))
            .andExpect(jsonPath("$.status").value(GoalStatus.ACTIVE.name))
            .andReturn()
        val goalId = UUID.fromString(objectMapper.readTree(result.response.contentAsByteArray)["id"].asText())

        mvc.perform(
            get(PathConstants.GOALS)
                .queryParam("currency", Currency.EUR.name)
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(goalId.toString()))
            .andExpect(jsonPath("$.statusCounts.active").value(1))

        mvc.perform(
            get("${PathConstants.GOALS}/$goalId/contributions")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].accountId").value(account.id.toString()))
            .andExpect(jsonPath("$.items[0].type").value(GoalTransactionType.CONTRIBUTION.name))

        assertAccountBalance(account.id, user, BigDecimal("450.00"))
    }

    @Test
    fun `should add contribution idempotently achieve goal and reopen it with refund`() {
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val goalId = createGoal(user, account)
        val contributionRequest = CreateGoalContributionRq(
            accountId = account.id,
            amount = BigDecimal("150.00"),
            note = "Finish goal",
        )

        val firstResult = mvc.perform(
            post("${PathConstants.GOALS}/$goalId/contributions")
                .header("Idempotency-Key", "finish-emergency-fund")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(contributionRequest)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.goal.savedAmount").value(200.0))
            .andExpect(jsonPath("$.goal.status").value(GoalStatus.ACHIEVED.name))
            .andReturn()
        val contributionId = UUID.fromString(
            objectMapper.readTree(firstResult.response.contentAsByteArray)["contribution"]["id"].asText(),
        )

        mvc.perform(
            post("${PathConstants.GOALS}/$goalId/contributions")
                .header("Idempotency-Key", "finish-emergency-fund")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(contributionRequest)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.contribution.id").value(contributionId.toString()))

        assertThat(
            dsl.selectCount()
                .from(Tables.GOAL_TRANSACTIONS)
                .where(Tables.GOAL_TRANSACTIONS.GOAL_ID.eq(goalId))
                .fetchOne(0, Int::class.java),
        ).isEqualTo(2)
        assertAccountBalance(account.id, user, BigDecimal("300.00"))

        mvc.perform(
            delete("${PathConstants.GOALS}/$goalId/contributions/$contributionId")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.savedAmount").value(50.0))
            .andExpect(jsonPath("$.status").value(GoalStatus.ACTIVE.name))

        val refund = goalTransactionRepository.findRefundByContributionIdAndUserId(contributionId, user.id)
        assertThat(refund?.type).isEqualTo(GoalTransactionType.REFUND)
        assertThat(refund?.amount).isEqualByComparingTo("150.00")
        assertAccountBalance(account.id, user, BigDecimal("450.00"))
    }

    @Test
    fun `should update and soft delete goal`() {
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val goalId = createGoal(user, account)

        mvc.perform(
            patch("${PathConstants.GOALS}/$goalId")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        UpdateGoalRq(
                            name = "Safety reserve",
                            targetAmount = BigDecimal("250.00"),
                            contributionPlan = GoalContributionPlanRq(
                                type = GoalContributionPlanType.CUSTOM,
                                monthlyAmount = BigDecimal("75.00"),
                            ),
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Safety reserve"))
            .andExpect(jsonPath("$.targetAmount").value(250.0))
            .andExpect(jsonPath("$.contributionPlan.monthlyAmount").value(75.0))

        mvc.perform(
            delete("${PathConstants.GOALS}/$goalId")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNoContent)

        assertThat(goalRepository.findByIdAndUserId(goalId, user.id)?.status).isEqualTo(GoalStatus.CANCELLED)
        assertThat(goalRepository.findByIdAndUserId(goalId, user.id)?.archivedAt).isNotNull()
    }

    @Test
    fun `should calculate overview from goals and net monthly contributions`() {
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        createGoal(user, account)

        mvc.perform(
            get(PathConstants.GOALS + PathConstants.GOAL_OVERVIEW)
                .queryParam("currency", Currency.EUR.name)
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))
            .andExpect(jsonPath("$.summary.totalSavedAmount").value(50.0))
            .andExpect(jsonPath("$.summary.contributedThisMonthAmount").value(50.0))
            .andExpect(jsonPath("$.summary.plannedMonthlyAmount").value(50.0))
            .andExpect(jsonPath("$.summary.monthlyPlanCompletionPercentage").value(100.0))
            .andExpect(jsonPath("$.summary.activeGoalCount").value(1))
            .andExpect(jsonPath("$.nearestTarget.goalName").value("Emergency fund"))
            .andExpect(jsonPath("$.currentMonth.contributions[0].amount").value(50.0))
    }

    @Test
    fun `should not allow contribution from account in another currency`() {
        val user = userFixture.createInDb()
        val euroAccount = createAccount(user.id)
        val goalId = createGoal(user, euroAccount)
        val dollarAccount = createAccount(user.id, Currency.USD)

        mvc.perform(
            post("${PathConstants.GOALS}/$goalId/contributions")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        CreateGoalContributionRq(
                            accountId = dollarAccount.id,
                            amount = BigDecimal("10.00"),
                        ),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Goal and account currencies must match"))
    }

    private fun createGoal(
        user: User,
        account: Account,
    ): UUID {
        val result = mvc.perform(
            post(PathConstants.GOALS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createGoalRequest(account.id))),
        ).andExpect(status().isCreated).andReturn()

        return UUID.fromString(objectMapper.readTree(result.response.contentAsByteArray)["id"].asText())
    }

    private fun createGoalRequest(accountId: UUID): CreateGoalRq =
        CreateGoalRq(
            name = "Emergency fund",
            targetAmount = BigDecimal("200.00"),
            currency = Currency.EUR,
            targetMonth = TARGET_MONTH,
            contributionPlan = GoalContributionPlanRq(
                type = GoalContributionPlanType.CUSTOM,
                monthlyAmount = BigDecimal("50.00"),
            ),
            initialContribution = InitialGoalContributionRq(
                accountId = accountId,
                amount = BigDecimal("50.00"),
                note = "Initial savings",
            ),
            icon = "emergency",
            color = "#10B981",
        )

    private fun createAccount(
        userId: UUID,
        currency: Currency = Currency.EUR,
    ): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = "Savings source",
                type = AccountType.BANK,
                openingBalance = BigDecimal("500.00"),
                currency = currency,
                createdAt = OffsetDateTime.now(),
                closedAt = null,
            ),
        )

    private fun assertAccountBalance(
        accountId: UUID,
        user: User,
        expectedBalance: BigDecimal,
    ) {
        mvc.perform(
            get("${PathConstants.ACCOUNTS}/$accountId")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(expectedBalance.toDouble()))
    }

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.dto.BudgetPlanningErrorCode
import ru.digitalhustle.certis.api.dto.request.CreateBudgetPlanRq
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.features.budget.model.Budget
import ru.digitalhustle.certis.features.security.model.User
import ru.digitalhustle.certis.shared.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

class BudgetPlanningControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val MONTH = "2026-10"
        private const val IDEMPOTENCY_KEY = "october-plan"
        private val BUDGET_MONTH: LocalDate = LocalDate.parse("$MONTH-01")
        private val NOW: OffsetDateTime = OffsetDateTime.parse("2026-09-15T12:00:00Z")
    }

    @Test
    fun `should create plan idempotently and expose it through current and id lookups`() {
        // given
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val baselineBudget = budgetRepository.insert(
            Budget(
                id = UUID.randomUUID(),
                userId = user.id,
                budgetMonth = BUDGET_MONTH,
                plannedIncome = BigDecimal("185000.00"),
                savingsTarget = BigDecimal("30000.00"),
                currency = Currency.RUB,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        )

        // when
        val created = createPlan(user, IDEMPOTENCY_KEY)
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.month").value(MONTH))
            .andExpect(jsonPath("$.currency").value(Currency.RUB.name))
            .andExpect(jsonPath("$.revision").value(1))
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.currentStep").value("FORECAST"))
            .andExpect(jsonPath("$.forecast.status").value("MISSING"))
            .andExpect(jsonPath("$.forecast.summary").isEmpty)
            .andExpect(jsonPath("$.constraints.status").value("MISSING"))
            .andExpect(jsonPath("$.constraints.feasibility").isEmpty)
            .andExpect(jsonPath("$.currentOptimization").isEmpty)
            .andExpect(jsonPath("$.capabilities.canEditForecast").value(true))
            .andExpect(jsonPath("$.capabilities.canEditConstraints").value(false))
            .andExpect(jsonPath("$.capabilities.canRunOptimization").value(false))
            .andExpect(jsonPath("$.capabilities.canApply").value(false))
            .andExpect(jsonPath("$.capabilities.canCancel").value(true))
        val plan = getBody(created, BudgetPlanRs::class.java)

        val replayed = getBody(
            createPlan(user, IDEMPOTENCY_KEY).andExpect(status().isCreated),
            BudgetPlanRs::class.java,
        )
        assertThat(replayed.id).isEqualTo(plan.id)
        assertThat(dsl.fetchCount(Tables.BUDGET_PLANS, Tables.BUDGET_PLANS.USER_ID.eq(user.id))).isEqualTo(1)
        assertBaselineBudget(plan.id, baselineBudget.id)

        mvc.perform(
            get("${PathConstants.BUDGET_PLANS}${PathConstants.BUDGET_PLAN_CURRENT}")
                .cookie(accessTokenCookie(user))
                .param("month", MONTH)
                .param("currency", Currency.RUB.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(plan.id.toString()))

        mvc.perform(
            get("${PathConstants.BUDGET_PLANS}/${plan.id}")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(plan.id.toString()))
    }

    @Test
    fun `should isolate plan lookup by owner`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb { copy(email = "another-planner@test.com") }
        val plan = getBody(
            createPlan(owner, IDEMPOTENCY_KEY).andExpect(status().isCreated),
            BudgetPlanRs::class.java,
        )

        // when, then
        mvc.perform(
            get("${PathConstants.BUDGET_PLANS}/${plan.id}")
                .cookie(accessTokenCookie(anotherUser)),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(BudgetPlanningErrorCode.BUDGET_PLAN_NOT_FOUND.name))
            .andExpect(jsonPath("$.details.planId").value(plan.id.toString()))
    }

    @Test
    fun `should return all scope revisions newest first`() {
        // given
        val user = userFixture.createInDb()
        val firstPlan = getBody(
            createPlan(user, "first-plan").andExpect(status().isCreated),
            BudgetPlanRs::class.java,
        )
        dsl.update(Tables.BUDGET_PLANS)
            .set(Tables.BUDGET_PLANS.STATUS, "CANCELLED")
            .set(Tables.BUDGET_PLANS.CANCELLED_AT, firstPlan.createdAt.plusSeconds(1))
            .set(Tables.BUDGET_PLANS.UPDATED_AT, firstPlan.createdAt.plusSeconds(1))
            .where(Tables.BUDGET_PLANS.ID.eq(firstPlan.id))
            .execute()
        val secondPlan = getBody(
            createPlan(user, "second-plan").andExpect(status().isCreated),
            BudgetPlanRs::class.java,
        )

        // when, then
        mvc.perform(
            get(PathConstants.BUDGET_PLANS)
                .cookie(accessTokenCookie(user))
                .param("month", MONTH)
                .param("currency", Currency.RUB.name),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value(secondPlan.id.toString()))
            .andExpect(jsonPath("$.items[0].revision").value(2))
            .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
            .andExpect(jsonPath("$.items[1].id").value(firstPlan.id.toString()))
            .andExpect(jsonPath("$.items[1].revision").value(1))
            .andExpect(jsonPath("$.items[1].status").value("CANCELLED"))

        assertThat(
            dsl.select(Tables.BUDGET_PLANS.PREVIOUS_PLAN_ID)
                .from(Tables.BUDGET_PLANS)
                .where(Tables.BUDGET_PLANS.ID.eq(secondPlan.id))
                .fetchSingle(Tables.BUDGET_PLANS.PREVIOUS_PLAN_ID),
        ).isEqualTo(firstPlan.id)
    }

    @Test
    fun `should reject active scope and idempotency key reuse`() {
        // given
        val user = userFixture.createInDb()
        createPlan(user, IDEMPOTENCY_KEY).andExpect(status().isCreated)

        // when, then
        createPlan(user, "another-key")
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value(BudgetPlanningErrorCode.ACTIVE_BUDGET_PLAN_EXISTS.name))

        mvc.perform(
            post(PathConstants.BUDGET_PLANS)
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        CreateBudgetPlanRq(
                            month = YearMonth.parse("2026-11"),
                            currency = Currency.RUB,
                        ),
                    ),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value(BudgetPlanningErrorCode.IDEMPOTENCY_KEY_REUSED.name))
    }

    @Test
    fun `should return not found for missing current plan and require authentication`() {
        // given
        val user = userFixture.createInDb()

        // when, then
        mvc.perform(
            get("${PathConstants.BUDGET_PLANS}${PathConstants.BUDGET_PLAN_CURRENT}")
                .cookie(accessTokenCookie(user))
                .param("month", MONTH)
                .param("currency", Currency.RUB.name),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(BudgetPlanningErrorCode.BUDGET_PLAN_NOT_FOUND.name))

        mvc.perform(
            get(PathConstants.BUDGET_PLANS)
                .param("month", MONTH)
                .param("currency", Currency.RUB.name),
        ).andExpect(status().isUnauthorized)
    }

    private fun createPlan(
        user: User,
        idempotencyKey: String,
    ) =
        mvc.perform(
            post(PathConstants.BUDGET_PLANS)
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        CreateBudgetPlanRq(
                            month = YearMonth.parse(MONTH),
                            currency = Currency.RUB,
                        ),
                    ),
                ),
        )

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )

    private fun assertBaselineBudget(
        planId: UUID,
        baselineBudgetId: UUID,
    ) {
        val actualBaselineBudgetId = dsl.select(Tables.BUDGET_PLANS.BASELINE_BUDGET_ID)
            .from(Tables.BUDGET_PLANS)
            .where(Tables.BUDGET_PLANS.ID.eq(planId))
            .fetchSingle(Tables.BUDGET_PLANS.BASELINE_BUDGET_ID)

        assertThat(actualBaselineBudgetId).isEqualTo(baselineBudgetId)
    }
}

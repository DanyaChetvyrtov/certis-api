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
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.SaveBudgetAllocationRq
import ru.digitalhustle.certis.dto.request.SaveBudgetRq
import ru.digitalhustle.certis.dto.response.BudgetOptimizationRs
import ru.digitalhustle.certis.dto.response.BudgetRs
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.BudgetOptimizationReason
import ru.digitalhustle.certis.enums.BudgetOptimizationStatus
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

class BudgetOptimizationControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val BUDGET_MONTH = "2026-08"
        private const val BUDGET_PATH = "${PathConstants.BUDGETS}/$BUDGET_MONTH"
        private const val OPTIMIZATIONS_PATH = "$BUDGET_PATH/optimizations"
    }

    @Test
    fun `should generate get and atomically apply optimization`() {
        // given
        val scenario = prepareOptimizationScenario()

        // when
        val generated = generateOptimization(scenario.user)

        // then
        assertGeneratedOptimization(generated, scenario)
        assertLatestOptimization(generated.id, scenario.user, BudgetOptimizationStatus.PROPOSED)

        val appliedBudget = getBody(
            mvc.perform(
                post("$OPTIMIZATIONS_PATH/${generated.id}/apply")
                    .cookie(accessTokenCookie(scenario.user)),
            ).andExpect(status().isOk),
            BudgetRs::class.java,
        )

        assertAppliedLimits(appliedBudget, scenario)
        assertLatestOptimization(generated.id, scenario.user, BudgetOptimizationStatus.APPLIED)

        mvc.perform(
            post("$OPTIMIZATIONS_PATH/${generated.id}/apply")
                .cookie(accessTokenCookie(scenario.user)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.BUDGET_OPTIMIZATION_NOT_PROPOSED))
    }

    @Test
    fun `should reject stale optimization and keep proposal unapplied`() {
        // given
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val account = createAccount(user.id)
        val category = createCategory(user.id, "Groceries")
        saveBudget(
            user,
            listOf(allocation(category.id, BudgetExpenseType.VARIABLE, "400.0000")),
        )
        val optimization = generateOptimization(user)

        createTransaction(account, category, "10.0000")

        // when, then
        mvc.perform(
            post("$OPTIMIZATIONS_PATH/${optimization.id}/apply")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.BUDGET_OPTIMIZATION_STALE))

        assertThat(
            dsl.select(Tables.BUDGET_OPTIMIZATIONS.STATUS)
                .from(Tables.BUDGET_OPTIMIZATIONS)
                .where(Tables.BUDGET_OPTIMIZATIONS.ID.eq(optimization.id))
                .fetchSingle(Tables.BUDGET_OPTIMIZATIONS.STATUS),
        ).isEqualTo(BudgetOptimizationStatus.PROPOSED.name)
    }

    @Test
    fun `should isolate dismiss and read by owner`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb { copy(email = "optimization-other@test.com") }
        val category = createCategory(owner.id, "Groceries")
        saveBudget(
            owner,
            listOf(allocation(category.id, BudgetExpenseType.VARIABLE, "400.0000")),
        )
        val firstOptimization = generateOptimization(owner)
        val optimization = generateOptimization(owner)
        assertThat(
            dsl.select(Tables.BUDGET_OPTIMIZATIONS.STATUS)
                .from(Tables.BUDGET_OPTIMIZATIONS)
                .where(Tables.BUDGET_OPTIMIZATIONS.ID.eq(firstOptimization.id))
                .fetchSingle(Tables.BUDGET_OPTIMIZATIONS.STATUS),
        ).isEqualTo(BudgetOptimizationStatus.DISMISSED.name)

        // when, then
        mvc.perform(
            get("$OPTIMIZATIONS_PATH/latest")
                .cookie(accessTokenCookie(anotherUser)),
        ).andExpect(status().isNotFound)

        mvc.perform(
            post("$OPTIMIZATIONS_PATH/${optimization.id}/dismiss")
                .cookie(accessTokenCookie(anotherUser)),
        ).andExpect(status().isNotFound)

        mvc.perform(
            post("$OPTIMIZATIONS_PATH/${optimization.id}/dismiss")
                .cookie(accessTokenCookie(owner)),
        ).andExpect(status().isNoContent)

        mvc.perform(
            get("$OPTIMIZATIONS_PATH/latest")
                .cookie(accessTokenCookie(owner)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(BudgetOptimizationStatus.DISMISSED.name))

        mvc.perform(
            post("$OPTIMIZATIONS_PATH/${optimization.id}/apply")
                .cookie(accessTokenCookie(owner)),
        ).andExpect(status().isConflict)
    }

    private fun saveBudget(
        user: User,
        allocations: List<SaveBudgetAllocationRq>,
    ) {
        mvc.perform(
            put(BUDGET_PATH)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        SaveBudgetRq(
                            monthlyIncome = BigDecimal("2000.0000"),
                            savingsTarget = BigDecimal("200.0000"),
                            allocations = allocations,
                        ),
                    ),
                ),
        ).andExpect(status().isOk)
    }

    private fun generateOptimization(user: User): BudgetOptimizationRs =
        getBody(
            mvc.perform(
                post(OPTIMIZATIONS_PATH)
                    .cookie(accessTokenCookie(user)),
            ).andExpect(status().isCreated),
            BudgetOptimizationRs::class.java,
        )

    private fun prepareOptimizationScenario(): OptimizationScenario {
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val scenario = OptimizationScenario(
            user = user,
            rent = createCategory(user.id, "Rent"),
            groceries = createCategory(user.id, "Groceries"),
            transport = createCategory(user.id, "Transport"),
            dining = createCategory(user.id, "Dining"),
        )
        val account = createAccount(user.id)

        createScenarioTransactions(account, scenario)
        saveBudget(
            user,
            listOf(
                allocation(scenario.rent.id, BudgetExpenseType.FIXED, "200.0000"),
                allocation(scenario.groceries.id, BudgetExpenseType.VARIABLE, "400.0000"),
                allocation(scenario.transport.id, BudgetExpenseType.VARIABLE, "100.0000"),
                allocation(scenario.dining.id, BudgetExpenseType.VARIABLE, "100.0000"),
            ),
        )

        return scenario
    }

    private fun createScenarioTransactions(
        account: Account,
        scenario: OptimizationScenario,
    ) {
        createTransaction(account, scenario.rent, "210.0000")
        createTransaction(account, scenario.groceries, "100.0000")
        createTransaction(account, scenario.transport, "120.0000")
        createTransaction(account, scenario.dining, "98.0000")
    }

    private fun allocation(
        categoryId: UUID,
        type: BudgetExpenseType,
        limit: String,
    ): SaveBudgetAllocationRq =
        SaveBudgetAllocationRq(
            categoryId = categoryId,
            type = type,
            limit = BigDecimal(limit),
        )

    private fun createCategory(
        userId: UUID,
        name: String,
    ): Category =
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = CategoryType.EXPENSE,
                icon = "wallet",
                color = "#10B981",
                archivedAt = null,
            ),
        )

    private fun createAccount(userId: UUID): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = "Ruble card",
                type = AccountType.CARD,
                openingBalance = BigDecimal.ZERO,
                currency = Currency.RUB,
                createdAt = OffsetDateTime.parse("2026-08-01T08:00:00Z"),
                closedAt = null,
            ),
        )

    private fun createTransaction(
        account: Account,
        category: Category,
        amount: String,
    ) {
        val timestamp = OffsetDateTime.parse("2026-08-10T10:00:00Z")

        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = category.id,
                recurringTransactionTemplateId = null,
                type = TransactionType.EXPENSE,
                amount = BigDecimal(amount),
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

    private fun assertRecommendation(
        optimization: BudgetOptimizationRs,
        categoryId: UUID,
        expectedLimit: String,
        expectedReason: BudgetOptimizationReason,
    ) {
        val recommendation = optimization.allocations.single { it.categoryId == categoryId }

        assertThat(recommendation.recommendedLimit).isEqualByComparingTo(expectedLimit)
        assertThat(recommendation.reason).isEqualTo(expectedReason)
    }

    private fun assertGeneratedOptimization(
        optimization: BudgetOptimizationRs,
        scenario: OptimizationScenario,
    ) {
        assertThat(optimization.status).isEqualTo(BudgetOptimizationStatus.PROPOSED)
        assertThat(optimization.algorithmVersion).isEqualTo("rule-based-v1")
        assertThat(optimization.savingsBefore).isEqualByComparingTo("1200.0000")
        assertThat(optimization.savingsAfter).isEqualByComparingTo("1247.2000")
        assertThat(optimization.additionalSavings).isEqualByComparingTo("47.2000")
        assertRecommendation(
            optimization,
            scenario.rent.id,
            "200.0000",
            BudgetOptimizationReason.FIXED_PRESERVED,
        )
        assertRecommendation(
            optimization,
            scenario.groceries.id,
            "325.0000",
            BudgetOptimizationReason.LOW_UTILIZATION_REDUCTION,
        )
        assertRecommendation(
            optimization,
            scenario.transport.id,
            "120.0000",
            BudgetOptimizationReason.OVERSPENDING_REALLOCATION,
        )
        assertRecommendation(
            optimization,
            scenario.dining.id,
            "107.8000",
            BudgetOptimizationReason.NEAR_LIMIT_REALLOCATION,
        )
    }

    private fun assertAppliedLimits(
        budget: BudgetRs,
        scenario: OptimizationScenario,
    ) {
        val appliedLimits = budget.allocations.associate { allocation ->
            allocation.categoryId to allocation.limit
        }

        assertThat(appliedLimits[scenario.rent.id]).isEqualByComparingTo("200.0000")
        assertThat(appliedLimits[scenario.groceries.id]).isEqualByComparingTo("325.0000")
        assertThat(appliedLimits[scenario.transport.id]).isEqualByComparingTo("120.0000")
        assertThat(appliedLimits[scenario.dining.id]).isEqualByComparingTo("107.8000")
    }

    private fun assertLatestOptimization(
        optimizationId: UUID,
        user: User,
        expectedStatus: BudgetOptimizationStatus,
    ) {
        val result = mvc.perform(
            get("$OPTIMIZATIONS_PATH/latest")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(optimizationId.toString()))
            .andExpect(jsonPath("$.status").value(expectedStatus.name))

        if (expectedStatus == BudgetOptimizationStatus.APPLIED) {
            result.andExpect(jsonPath("$.appliedAt").isNotEmpty())
        }
    }

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )

    private data class OptimizationScenario(
        val user: User,
        val rent: Category,
        val groceries: Category,
        val transport: Category,
        val dining: Category,
    )
}

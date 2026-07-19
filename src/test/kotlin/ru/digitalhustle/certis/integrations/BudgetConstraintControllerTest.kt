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
import ru.digitalhustle.certis.api.dto.BudgetAllocationType
import ru.digitalhustle.certis.api.dto.BudgetConstraintRole
import ru.digitalhustle.certis.api.dto.BudgetFundingLevel
import ru.digitalhustle.certis.api.dto.BudgetPriority
import ru.digitalhustle.certis.api.dto.request.ApplyBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.request.BudgetCategoryConstraintRq
import ru.digitalhustle.certis.api.dto.request.BudgetFundingLevelRq
import ru.digitalhustle.certis.api.dto.request.ConfirmBudgetForecastRq
import ru.digitalhustle.certis.api.dto.request.CreateBudgetPlanRq
import ru.digitalhustle.certis.api.dto.request.DismissBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.request.GenerateBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.request.SaveBudgetConstraintsRq
import ru.digitalhustle.certis.api.dto.response.BudgetConstraintSetRs
import ru.digitalhustle.certis.api.dto.response.BudgetForecastPreviewRs
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationApplyRs
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationRunRs
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.features.account.enums.AccountType
import ru.digitalhustle.certis.features.account.model.Account
import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import ru.digitalhustle.certis.features.budget.model.Budget
import ru.digitalhustle.certis.features.budget.model.BudgetCategory
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

@Suppress("LargeClass", "TooManyFunctions")
class BudgetConstraintControllerTest : AbstractIntegrationTest() {

    @Test
    @Suppress("LongMethod")
    fun `should generate read replay and dismiss an exact budget optimization`() {
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val account = createAccount(user)
        val salaryCategory = createCategory(user, "Salary", CategoryType.INCOME)
        val rentCategory = createCategory(user, "Rent", CategoryType.EXPENSE)
        val groceriesCategory = createCategory(user, "Groceries", CategoryType.EXPENSE)
        createBaselineBudget(user, rentCategory, groceriesCategory)
        createRecurring(user, account, salaryCategory, "Salary", "185000.00", 5)
        createRecurring(user, account, rentCategory, "Rent", "55000.00", 1)
        val plan = createPlan(user)
        confirmForecast(user, plan)
        val suggestedResult = mvc.perform(
            get(constraintsPath(plan.id)).cookie(accessTokenCookie(user)),
        ).andExpect(status().isOk)
        val suggested = getBody(suggestedResult, BudgetConstraintSetRs::class.java)
        val confirmedResult = mvc.perform(
            put(constraintsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveRequest(suggested))),
        ).andExpect(status().isOk)
        val confirmed = getBody(confirmedResult, BudgetConstraintSetRs::class.java)
        val request = GenerateBudgetOptimizationRq(
            expectedVersion = confirmed.planVersion,
            forecastRevision = confirmed.basedOnForecastRevision,
            constraintsRevision = requireNotNull(confirmed.revision),
            targetSavingsAmount = confirmed.savingsFloorAmount,
        )

        val generatedResult = mvc.perform(
            post(optimizationsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "optimization-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("GENERATED"))
            .andExpect(jsonPath("$.algorithmVersion").value("mckp-v1"))
            .andExpect(jsonPath("$.planVersion").value(3))
            .andExpect(jsonPath("$.decisions.length()").value(2))
            .andExpect(jsonPath("$.constraintChecks.length()").value(3))
        val generated = getBody(generatedResult, BudgetOptimizationRunRs::class.java)

        mvc.perform(
            post(optimizationsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "optimization-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(generated.id.toString()))
            .andExpect(jsonPath("$.planVersion").value(3))

        mvc.perform(get(latestOptimizationPath(plan.id)).cookie(accessTokenCookie(user)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(generated.id.toString()))

        mvc.perform(
            put(dismissalPath(plan.id, generated.id))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(DismissBudgetOptimizationRq(expectedVersion = 3))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DISMISSED"))
            .andExpect(jsonPath("$.currentStep").value("OPTIMIZE"))
            .andExpect(jsonPath("$.planVersion").value(4))

        mvc.perform(get(optimizationPath(plan.id, generated.id)).cookie(accessTokenCookie(user)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DISMISSED"))
        assertThat(dsl.fetchCount(Tables.BUDGET_OPTIMIZATION_RUNS)).isEqualTo(1)
        assertThat(dsl.fetchCount(Tables.BUDGET_OPTIMIZATION_DECISIONS)).isEqualTo(2)
    }

    @Test
    @Suppress("LongMethod")
    fun `should apply an optimization idempotently without changing recurring templates`() {
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val account = createAccount(user)
        val salaryCategory = createCategory(user, "Salary", CategoryType.INCOME)
        val rentCategory = createCategory(user, "Rent", CategoryType.EXPENSE)
        val groceriesCategory = createCategory(user, "Groceries", CategoryType.EXPENSE)
        createBaselineBudget(user, rentCategory, groceriesCategory)
        createRecurring(user, account, salaryCategory, "Salary", "185000.00", 5)
        createRecurring(user, account, rentCategory, "Rent", "55000.00", 1)
        val plan = createPlan(user)
        confirmForecast(user, plan)
        val suggestedResult = mvc.perform(
            get(constraintsPath(plan.id)).cookie(accessTokenCookie(user)),
        ).andExpect(status().isOk)
        val suggested = getBody(suggestedResult, BudgetConstraintSetRs::class.java)
        val confirmedResult = mvc.perform(
            put(constraintsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveRequest(suggested))),
        ).andExpect(status().isOk)
        val confirmed = getBody(confirmedResult, BudgetConstraintSetRs::class.java)
        val generationRequest = GenerateBudgetOptimizationRq(
            expectedVersion = confirmed.planVersion,
            forecastRevision = confirmed.basedOnForecastRevision,
            constraintsRevision = requireNotNull(confirmed.revision),
            targetSavingsAmount = confirmed.savingsFloorAmount,
        )
        val generatedResult = mvc.perform(
            post(optimizationsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "optimization-to-apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(generationRequest)),
        ).andExpect(status().isCreated)
        val generated = getBody(generatedResult, BudgetOptimizationRunRs::class.java)
        val applyRequest = ApplyBudgetOptimizationRq(expectedVersion = generated.planVersion)

        val appliedResult = mvc.perform(
            put(applicationPath(plan.id, generated.id))
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "apply-optimization-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(applyRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.plan.status").value("APPLIED"))
            .andExpect(jsonPath("$.plan.currentStep").value("APPLIED"))
            .andExpect(jsonPath("$.plan.version").value(4))
            .andExpect(jsonPath("$.optimization.status").value("APPLIED"))
            .andExpect(jsonPath("$.budget.sourceOptimizationId").value(generated.id.toString()))
            .andExpect(jsonPath("$.budget.allocations.length()").value(2))
        val applied = getBody(appliedResult, BudgetOptimizationApplyRs::class.java)

        mvc.perform(
            put(applicationPath(plan.id, generated.id))
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "apply-optimization-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(applyRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.budget.id").value(applied.budget.id.toString()))
            .andExpect(jsonPath("$.plan.version").value(4))

        assertThat(
            dsl.select(Tables.BUDGETS.SOURCE_OPTIMIZATION_ID)
                .from(Tables.BUDGETS)
                .where(Tables.BUDGETS.ID.eq(applied.budget.id))
                .fetchSingle(Tables.BUDGETS.SOURCE_OPTIMIZATION_ID),
        ).isEqualTo(generated.id)
        assertThat(dsl.fetchCount(Tables.BUDGET_CATEGORIES)).isEqualTo(2)
        assertThat(dsl.fetchCount(Tables.RECURRING_TRANSACTION_TEMPLATES)).isEqualTo(2)
    }

    @Test
    fun `should reject a stale optimization without partially applying a budget`() {
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val scenario = generatedScenario(user, "stale-optimization")
        dsl.update(Tables.RECURRING_TRANSACTION_TEMPLATES)
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.AMOUNT, BigDecimal("56000.00"))
            .set(Tables.RECURRING_TRANSACTION_TEMPLATES.UPDATED_AT, NOW.plusHours(1))
            .where(
                Tables.RECURRING_TRANSACTION_TEMPLATES.USER_ID.eq(user.id)
                    .and(Tables.RECURRING_TRANSACTION_TEMPLATES.NAME.eq("Rent")),
            )
            .execute()

        mvc.perform(
            put(applicationPath(scenario.plan.id, scenario.optimization.id))
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "stale-application")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        ApplyBudgetOptimizationRq(expectedVersion = scenario.optimization.planVersion),
                    ),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("OPTIMIZATION_STALE"))

        assertThat(
            dsl.select(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS)
                .from(Tables.BUDGET_OPTIMIZATION_RUNS)
                .where(Tables.BUDGET_OPTIMIZATION_RUNS.ID.eq(scenario.optimization.id))
                .fetchSingle(Tables.BUDGET_OPTIMIZATION_RUNS.STATUS),
        ).isEqualTo("STALE")
        assertThat(
            dsl.select(Tables.BUDGET_PLANS.STATUS)
                .from(Tables.BUDGET_PLANS)
                .where(Tables.BUDGET_PLANS.ID.eq(scenario.plan.id))
                .fetchSingle(Tables.BUDGET_PLANS.STATUS),
        ).isEqualTo("DRAFT")
        assertThat(
            dsl.select(Tables.BUDGETS.SOURCE_OPTIMIZATION_ID)
                .from(Tables.BUDGETS)
                .where(Tables.BUDGETS.USER_ID.eq(user.id))
                .fetchSingle(Tables.BUDGETS.SOURCE_OPTIMIZATION_ID),
        ).isNull()
    }

    @Test
    @Suppress("LongMethod")
    fun `should suggest confirm and read budget constraints`() {
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val account = createAccount(user)
        val salaryCategory = createCategory(user, "Salary", CategoryType.INCOME)
        val rentCategory = createCategory(user, "Rent", CategoryType.EXPENSE)
        val groceriesCategory = createCategory(user, "Groceries", CategoryType.EXPENSE)
        createBaselineBudget(user, rentCategory, groceriesCategory)
        createRecurring(user, account, salaryCategory, "Salary", "185000.00", 5)
        createRecurring(user, account, rentCategory, "Rent", "55000.00", 1)
        val plan = createPlan(user)
        confirmForecast(user, plan)

        val suggestedResult = mvc.perform(
            get(constraintsPath(plan.id)).cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUGGESTED"))
            .andExpect(jsonPath("$.revision").doesNotExist())
            .andExpect(jsonPath("$.savingsFloorAmount").value(30500.0))
            .andExpect(jsonPath("$.feasibility.status").value("FEASIBLE"))
            .andExpect(jsonPath("$.feasibility.requiredAmount").value(55000.0))
            .andExpect(jsonPath("$.feasibility.variableMinimumAmount").value(19200.0))
            .andExpect(jsonPath("$.categories.length()").value(2))
        val suggested = getBody(suggestedResult, BudgetConstraintSetRs::class.java)

        val confirmedResult = mvc.perform(
            put(constraintsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveRequest(suggested))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.revision").value(1))
            .andExpect(jsonPath("$.planVersion").value(2))
            .andExpect(jsonPath("$.feasibility.maximumSavingsAmount").value(110800.0))
        val confirmed = getBody(confirmedResult, BudgetConstraintSetRs::class.java)
        assertThat(confirmed.categories.flatMap { category -> category.sourceKeys }).hasSize(2)
        assertThat(
            dsl.fetchCount(
                Tables.BUDGET_CONSTRAINT_REVISIONS,
                Tables.BUDGET_CONSTRAINT_REVISIONS.PLAN_ID.eq(plan.id),
            ),
        ).isEqualTo(1)
        assertThat(
            dsl.fetchCount(Tables.BUDGET_CATEGORY_CONSTRAINTS),
        ).isEqualTo(2)
        assertThat(
            dsl.fetchCount(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS),
        ).isEqualTo(3)

        mvc.perform(get(constraintsPath(plan.id)).cookie(accessTokenCookie(user)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.revision").value(1))
            .andExpect(jsonPath("$.planVersion").value(2))
    }

    @Test
    fun `should confirm an additional active expense category outside the forecast`() {
        val user = userFixture.createInDb { copy(preferredCurrency = Currency.RUB) }
        val account = createAccount(user)
        val salaryCategory = createCategory(user, "Salary", CategoryType.INCOME)
        val rentCategory = createCategory(user, "Rent", CategoryType.EXPENSE)
        val groceriesCategory = createCategory(user, "Groceries", CategoryType.EXPENSE)
        val travelCategory = createCategory(user, "Travel", CategoryType.EXPENSE)
        createBaselineBudget(user, rentCategory, groceriesCategory)
        createRecurring(user, account, salaryCategory, "Salary", "185000.00", 5)
        createRecurring(user, account, rentCategory, "Rent", "55000.00", 1)
        val plan = createPlan(user)
        confirmForecast(user, plan)
        val suggestedResult = mvc.perform(
            get(constraintsPath(plan.id)).cookie(accessTokenCookie(user)),
        ).andExpect(status().isOk)
        val suggested = getBody(suggestedResult, BudgetConstraintSetRs::class.java)
        val manualConstraint = BudgetCategoryConstraintRq(
            categoryId = travelCategory.id,
            allocationType = BudgetAllocationType.VARIABLE,
            constraintRole = BudgetConstraintRole.FLEXIBLE,
            requiredAmount = BigDecimal.ZERO,
            priority = BudgetPriority.HIGH,
            fundingLevels = listOf(
                BudgetFundingLevelRq(BudgetFundingLevel.MINIMUM, BigDecimal("6000.00")),
                BudgetFundingLevelRq(BudgetFundingLevel.BALANCED, BigDecimal("8500.00")),
                BudgetFundingLevelRq(BudgetFundingLevel.COMFORTABLE, BigDecimal("10000.00")),
            ),
        )

        val confirmedResult = mvc.perform(
            put(constraintsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        saveRequest(suggested).copy(
                            categories = saveRequest(suggested).categories + manualConstraint,
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categories.length()").value(3))

        val confirmed = getBody(confirmedResult, BudgetConstraintSetRs::class.java)
        val travel = confirmed.categories.single { category -> category.category.id == travelCategory.id }
        assertThat(travel.sourceKeys).isEmpty()
        assertThat(travel.fundingLevels.map { level -> level.coverage })
            .containsExactly(BigDecimal("0.600000"), BigDecimal("0.850000"), BigDecimal("1.000000"))
    }

    private fun saveRequest(suggested: BudgetConstraintSetRs): SaveBudgetConstraintsRq =
        SaveBudgetConstraintsRq(
            expectedVersion = 1,
            forecastRevision = suggested.basedOnForecastRevision,
            savingsFloorAmount = suggested.savingsFloorAmount,
            categories = suggested.categories.map { category ->
                BudgetCategoryConstraintRq(
                    categoryId = category.category.id,
                    allocationType = category.allocationType,
                    constraintRole = category.constraintRole,
                    requiredAmount = category.requiredAmount,
                    priority = if (category.allocationType == BudgetAllocationType.VARIABLE) {
                        BudgetPriority.HIGH
                    } else {
                        null
                    },
                    fundingLevels = category.fundingLevels.map { level ->
                        BudgetFundingLevelRq(level.level, level.amount)
                    },
                )
            },
        )

    private fun generatedScenario(user: User, generationKey: String): GeneratedScenario {
        val account = createAccount(user)
        val salaryCategory = createCategory(user, "Salary", CategoryType.INCOME)
        val rentCategory = createCategory(user, "Rent", CategoryType.EXPENSE)
        val groceriesCategory = createCategory(user, "Groceries", CategoryType.EXPENSE)
        createBaselineBudget(user, rentCategory, groceriesCategory)
        createRecurring(user, account, salaryCategory, "Salary", "185000.00", 5)
        createRecurring(user, account, rentCategory, "Rent", "55000.00", 1)
        val plan = createPlan(user)
        confirmForecast(user, plan)
        val suggestedResult = mvc.perform(
            get(constraintsPath(plan.id)).cookie(accessTokenCookie(user)),
        ).andExpect(status().isOk)
        val suggested = getBody(suggestedResult, BudgetConstraintSetRs::class.java)
        val confirmedResult = mvc.perform(
            put(constraintsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(saveRequest(suggested))),
        ).andExpect(status().isOk)
        val confirmed = getBody(confirmedResult, BudgetConstraintSetRs::class.java)
        val generationRequest = GenerateBudgetOptimizationRq(
            expectedVersion = confirmed.planVersion,
            forecastRevision = confirmed.basedOnForecastRevision,
            constraintsRevision = requireNotNull(confirmed.revision),
            targetSavingsAmount = confirmed.savingsFloorAmount,
        )
        val generatedResult = mvc.perform(
            post(optimizationsPath(plan.id))
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", generationKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(generationRequest)),
        ).andExpect(status().isCreated)
        return GeneratedScenario(
            plan = plan,
            optimization = getBody(generatedResult, BudgetOptimizationRunRs::class.java),
        )
    }

    private fun confirmForecast(user: User, plan: BudgetPlanRs) {
        val previewResult = mvc.perform(
            get(forecastPreviewPath(plan.id)).cookie(accessTokenCookie(user)),
        ).andExpect(status().isOk)
        val preview = getBody(previewResult, BudgetForecastPreviewRs::class.java)
        mvc.perform(
            put(forecastPath(plan.id))
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        ConfirmBudgetForecastRq(
                            expectedVersion = 0,
                            sourceFingerprint = preview.sourceFingerprint,
                        ),
                    ),
                ),
        ).andExpect(status().isOk)
    }

    private fun createPlan(user: User): BudgetPlanRs {
        val result = mvc.perform(
            post(PathConstants.BUDGET_PLANS)
                .cookie(accessTokenCookie(user))
                .header("Idempotency-Key", "constraint-plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        CreateBudgetPlanRq(YearMonth.parse(TARGET_MONTH), Currency.RUB),
                    ),
                ),
        ).andExpect(status().isCreated)
        return getBody(result, BudgetPlanRs::class.java)
    }

    private fun createBaselineBudget(user: User, rent: Category, groceries: Category) {
        val budget = budgetRepository.insert(
            Budget(
                id = UUID.randomUUID(),
                userId = user.id,
                budgetMonth = LocalDate.parse("$TARGET_MONTH-01"),
                plannedIncome = BigDecimal("185000.00"),
                savingsTarget = BigDecimal("30500.00"),
                currency = Currency.RUB,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        )
        budgetAllocationRepository.insertAllocations(
            listOf(
                allocation(user, budget, rent, "55000.00", BudgetExpenseType.FIXED),
                allocation(user, budget, groceries, "32000.00", BudgetExpenseType.VARIABLE),
            ),
        )
    }

    private fun allocation(
        user: User,
        budget: Budget,
        category: Category,
        amount: String,
        type: BudgetExpenseType,
    ): BudgetCategory =
        BudgetCategory(
            id = UUID.randomUUID(),
            userId = user.id,
            budgetId = budget.id,
            categoryId = category.id,
            categoryType = CategoryType.EXPENSE,
            limitAmount = BigDecimal(amount),
            expenseType = type,
        )

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

    private fun constraintsPath(planId: UUID): String =
        PathConstants.BUDGET_PLANS + PathConstants.BUDGET_PLAN_CONSTRAINTS.replace("{planId}", planId.toString())

    private fun forecastPreviewPath(planId: UUID): String =
        PathConstants.BUDGET_PLANS + PathConstants.BUDGET_PLAN_FORECAST_PREVIEW.replace("{planId}", planId.toString())

    private fun forecastPath(planId: UUID): String =
        PathConstants.BUDGET_PLANS + PathConstants.BUDGET_PLAN_FORECAST.replace("{planId}", planId.toString())

    private fun optimizationsPath(planId: UUID): String =
        planningPath(PathConstants.BUDGET_PLAN_OPTIMIZATIONS, planId)

    private fun latestOptimizationPath(planId: UUID): String =
        planningPath(PathConstants.BUDGET_PLAN_OPTIMIZATIONS_LATEST, planId)

    private fun optimizationPath(planId: UUID, optimizationId: UUID): String =
        planningPath(PathConstants.BUDGET_PLAN_OPTIMIZATION_ID, planId)
            .replace("{optimizationId}", optimizationId.toString())

    private fun dismissalPath(planId: UUID, optimizationId: UUID): String =
        planningPath(PathConstants.BUDGET_PLAN_OPTIMIZATION_DISMISSAL, planId)
            .replace("{optimizationId}", optimizationId.toString())

    private fun applicationPath(planId: UUID, optimizationId: UUID): String =
        planningPath(PathConstants.BUDGET_PLAN_OPTIMIZATION_BUDGET_APPLICATION, planId)
            .replace("{optimizationId}", optimizationId.toString())

    private fun planningPath(template: String, planId: UUID): String =
        PathConstants.BUDGET_PLANS + template.replace("{planId}", planId.toString())

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val TARGET_MONTH = "2026-10"
        private val NOW = OffsetDateTime.parse("2026-09-15T12:00:00Z")
    }

    private data class GeneratedScenario(
        val plan: BudgetPlanRs,
        val optimization: BudgetOptimizationRunRs,
    )
}

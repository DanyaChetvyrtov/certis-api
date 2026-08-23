package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
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
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.dto.request.CreateRecurringTransactionRq
import ru.digitalhustle.certis.dto.request.UpdateRecurringTransactionRq
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RecurringTransactionControllerTest : AbstractIntegrationTest() {

    @Test
    fun `should create recurring transaction for authenticated user`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val category = createCategory(user.id)
        val request = createRequest(account.id, category.id)

        // when
        val result = mvc.perform(
            post(PathConstants.RECURRING_TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.accountId").value(account.id.toString()))
            .andExpect(jsonPath("$.categoryId").value(category.id.toString()))
            .andExpect(jsonPath("$.name").value(request.name))
            .andExpect(jsonPath("$.status").value(RecurringTransactionTemplateStatus.ACTIVE.name))
            .andExpect(jsonPath("$.frequency").value(RecurringTransactionFrequency.MONTHLY.name))
            .andExpect(jsonPath("$.intervalCount").value(1))
            .andExpect(jsonPath("$.lastRunDate").doesNotExist())
            .andExpect(jsonPath("$.nextRunDate").value(request.startDate.toString()))
            .andReturn()

        val templateId = UUID.fromString(
            objectMapper.readTree(result.response.contentAsByteArray)["id"].asText(),
        )

        assertThat(recurringTransactionTemplateRepository.findByIdAndUserId(templateId, user.id))
            .isNotNull()
    }

    @Test
    fun `should reject invalid date range`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val request = createRequest(account.id).copy(
            endDate = LocalDate.parse("2026-08-11"),
        )

        // when
        mvc.perform(
            post(PathConstants.RECURRING_TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.RECURRING_TRANSACTION_DATE_RANGE))
    }

    @Test
    fun `should not expose another user's recurring transaction`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "recurring-another-user@test.com")
        }
        val account = createAccount(owner.id)
        val templateId = createRecurringTransaction(owner, createRequest(account.id))

        // when
        mvc.perform(
            get("${PathConstants.RECURRING_TRANSACTIONS}/$templateId")
                .cookie(accessTokenCookie(anotherUser)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Recurring transaction not found"))
    }

    @Test
    fun `should list only authenticated user's recurring transactions`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "recurring-list-another-user@test.com")
        }
        val account = createAccount(user.id)
        val anotherAccount = createAccount(anotherUser.id)
        val templateId = createRecurringTransaction(user, createRequest(account.id))
        createRecurringTransaction(anotherUser, createRequest(anotherAccount.id))

        // when
        mvc.perform(
            get(PathConstants.RECURRING_TRANSACTIONS)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(templateId.toString()))
    }

    @Test
    fun `should pause and reschedule recurring transaction`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val templateId = createRecurringTransaction(user, createRequest(account.id))
        val request = updateRequest(account.id).copy(
            name = "Paused subscription",
            status = RecurringTransactionTemplateStatus.PAUSED,
            frequency = RecurringTransactionFrequency.WEEKLY,
            intervalCount = 2,
        )

        // when
        mvc.perform(
            put("${PathConstants.RECURRING_TRANSACTIONS}/$templateId")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(request.name))
            .andExpect(jsonPath("$.status").value(RecurringTransactionTemplateStatus.PAUSED.name))
            .andExpect(jsonPath("$.frequency").value(RecurringTransactionFrequency.WEEKLY.name))
            .andExpect(jsonPath("$.intervalCount").value(2))
    }

    @Test
    fun `should cancel recurring transaction idempotently`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val templateId = createRecurringTransaction(user, createRequest(account.id))

        // when
        mvc.perform(
            delete("${PathConstants.RECURRING_TRANSACTIONS}/$templateId")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNoContent)

        mvc.perform(
            delete("${PathConstants.RECURRING_TRANSACTIONS}/$templateId")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        val cancelled = recurringTransactionTemplateRepository.findByIdAndUserId(templateId, user.id)

        assertThat(cancelled?.status).isEqualTo(RecurringTransactionTemplateStatus.CANCELLED)
        assertThat(cancelled?.nextRunDate).isNull()
    }

    @Test
    fun `should prevent closing account used by recurring transaction`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        createRecurringTransaction(user, createRequest(account.id))

        // when
        mvc.perform(
            delete("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.ACCOUNT_IN_USE))

        assertThat(accountRepository.findByIdAndUserId(account.id, user.id)?.closedAt).isNull()
    }

    @Test
    fun `should allow closing account after recurring transaction cancellation`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val templateId = createRecurringTransaction(user, createRequest(account.id))

        mvc.perform(
            delete("${PathConstants.RECURRING_TRANSACTIONS}/$templateId")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNoContent)

        // when
        mvc.perform(
            delete("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(accountRepository.findByIdAndUserId(account.id, user.id)?.closedAt).isNotNull()
    }

    @Test
    fun `should create transaction and complete finite recurring transaction`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val currentDate = LocalDate.now(Clock.systemUTC())
        val templateId = createRecurringTransaction(
            user,
            createRequest(account.id).copy(
                startDate = currentDate,
                endDate = currentDate,
            ),
        )

        // when
        recurringTransactionExecutionService.execute(templateId, currentDate)

        // then
        val template = recurringTransactionTemplateRepository.findByIdAndUserId(templateId, user.id)
        val generatedTransactions = dsl.selectFrom(Tables.TRANSACTIONS)
            .where(Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID.eq(templateId))
            .fetch()

        assertThat(generatedTransactions).hasSize(1)
        assertThat(generatedTransactions.single()[Tables.TRANSACTIONS.SCHEDULED_FOR]).isEqualTo(currentDate)
        assertThat(template?.status).isEqualTo(RecurringTransactionTemplateStatus.COMPLETED)
        assertThat(template?.lastRunDate).isEqualTo(currentDate)
        assertThat(template?.nextRunDate).isNull()
    }

    @Test
    fun `should not duplicate occurrence when two workers execute same template`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val currentDate = LocalDate.now(Clock.systemUTC())
        val templateId = createRecurringTransaction(
            user,
            createRequest(account.id).copy(startDate = currentDate),
        )
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(WORKER_COUNT)

        // when
        try {
            val executions = (1..WORKER_COUNT).map {
                executor.submit {
                    startLatch.await()
                    recurringTransactionExecutionService.execute(templateId, currentDate)
                }
            }
            startLatch.countDown()
            executions.forEach { execution -> execution.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        // then
        val occurrenceCount = dsl.fetchCount(
            Tables.TRANSACTIONS,
            Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID.eq(templateId)
                .and(Tables.TRANSACTIONS.SCHEDULED_FOR.eq(currentDate)),
        )

        assertThat(occurrenceCount).isEqualTo(1)
    }

    @Test
    fun `should distribute due templates between concurrent workers`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val currentDate = LocalDate.now(Clock.systemUTC())
        val currentTime = OffsetDateTime.now(Clock.systemUTC())
        val templateIds = listOf(
            createRecurringTransaction(user, createRequest(account.id).copy(startDate = currentDate)),
            createRecurringTransaction(
                user,
                createRequest(account.id).copy(name = "Second subscription", startDate = currentDate),
            ),
        )
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(WORKER_COUNT)

        // when
        val executedTemplateIds = try {
            val executions = (1..WORKER_COUNT).map {
                executor.submit<UUID?> {
                    startLatch.await()
                    recurringTransactionExecutionService.executeNext(currentDate, currentTime, emptySet())?.templateId
                }
            }
            startLatch.countDown()
            executions.map { execution -> execution.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        // then
        assertThat(executedTemplateIds).containsExactlyInAnyOrderElementsOf(templateIds)
        assertThat(
            dsl.fetchCount(
                Tables.TRANSACTIONS,
                Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID.`in`(templateIds),
            ),
        ).isEqualTo(templateIds.size)
    }

    @Test
    fun `should advance template when scheduled occurrence already exists`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val currentDate = LocalDate.now(Clock.systemUTC())
        val templateId = createRecurringTransaction(
            user,
            createRequest(account.id).copy(startDate = currentDate),
        )
        val template = checkNotNull(recurringTransactionTemplateRepository.findByIdAndUserId(templateId, user.id))
        val now = OffsetDateTime.now(Clock.systemUTC())
        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = user.id,
                accountId = account.id,
                categoryId = null,
                recurringTransactionTemplateId = templateId,
                type = template.type,
                amount = template.amount,
                merchant = template.merchant,
                note = template.note,
                scheduledFor = currentDate,
                occurredAt = currentDate.atStartOfDay(Clock.systemUTC().zone).toOffsetDateTime(),
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                transferId = null,
            ),
        )

        // when
        recurringTransactionExecutionService.execute(templateId, currentDate)

        // then
        val occurrenceCount = dsl.fetchCount(
            Tables.TRANSACTIONS,
            Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID.eq(templateId)
                .and(Tables.TRANSACTIONS.SCHEDULED_FOR.eq(currentDate)),
        )
        val updatedTemplate = recurringTransactionTemplateRepository.findByIdAndUserId(templateId, user.id)

        assertThat(occurrenceCount).isEqualTo(1)
        assertThat(updatedTemplate?.lastRunDate).isEqualTo(currentDate)
        assertThat(updatedTemplate?.nextRunDate).isAfter(currentDate)
    }

    private fun createRecurringTransaction(
        user: User,
        request: CreateRecurringTransactionRq,
    ): UUID {
        val result = mvc.perform(
            post(PathConstants.RECURRING_TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return UUID.fromString(
            objectMapper.readTree(result.response.contentAsByteArray)["id"].asText(),
        )
    }

    private fun createRequest(
        accountId: UUID,
        categoryId: UUID? = null,
    ): CreateRecurringTransactionRq =
        CreateRecurringTransactionRq(
            accountId = accountId,
            categoryId = categoryId,
            name = "Subscription",
            type = TransactionType.EXPENSE,
            amount = BigDecimal("12.99"),
            merchant = "Streaming service",
            note = "Family plan",
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
            startDate = LocalDate.parse("2026-08-12"),
            endDate = null,
        )

    private fun updateRequest(accountId: UUID): UpdateRecurringTransactionRq =
        UpdateRecurringTransactionRq(
            accountId = accountId,
            categoryId = null,
            name = "Subscription",
            type = TransactionType.EXPENSE,
            amount = BigDecimal("15.99"),
            merchant = "Streaming service",
            note = "Updated plan",
            status = RecurringTransactionTemplateStatus.ACTIVE,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
            startDate = LocalDate.parse("2026-08-12"),
            endDate = null,
        )

    private fun createAccount(userId: UUID): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = "Main card",
                type = AccountType.CARD,
                openingBalance = BigDecimal("100.00"),
                currency = Currency.EUR,
                createdAt = OffsetDateTime.now(),
                closedAt = null,
            ),
        )

    private fun createCategory(userId: UUID): Category =
        categoryRepository.insert(
            Category(
                id = UUID.randomUUID(),
                userId = userId,
                name = "Subscriptions",
                type = CategoryType.EXPENSE,
                icon = "subscription",
                color = "#10B981",
                archivedAt = null,
            ),
        )

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )

    private companion object {
        private const val WORKER_COUNT = 2
        private const val EXECUTION_TIMEOUT_SECONDS = 10L
    }
}

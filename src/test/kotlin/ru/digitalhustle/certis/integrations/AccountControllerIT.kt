package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
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
import ru.digitalhustle.certis.dto.request.CreateAccountRq
import ru.digitalhustle.certis.dto.request.UpdateAccountRq
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalStatus
import ru.digitalhustle.certis.enums.GoalTransactionType
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Goal
import ru.digitalhustle.certis.model.entity.GoalTransaction
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class AccountControllerIT : AbstractIntegrationTest() {

    private companion object {
        private const val ACCESS_TOKEN_COOKIE = "access_token"
        private const val NAME = "Main card"
        private const val UPDATED_NAME = "Salary account"
        private val OPENING_BALANCE = BigDecimal("100.00")
        private val UPDATED_OPENING_BALANCE = BigDecimal("250.00")
    }

    @Test
    fun `should createInDb account for authenticated user`() {
        // given
        val user = userFixture.createInDb()

        // when
        val result = mvc.perform(
            post(PathConstants.ACCOUNTS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createAccountRequest())),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value(NAME))
            .andExpect(jsonPath("$.type").value(AccountType.CARD.name))
            .andExpect(jsonPath("$.openingBalance").value(OPENING_BALANCE.toDouble()))
            .andExpect(jsonPath("$.balance").value(OPENING_BALANCE.toDouble()))
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.closedAt").doesNotExist())
            .andReturn()

        val accountId = UUID.fromString(
            objectMapper.readTree(result.response.contentAsByteArray)["id"].asText(),
        )
        val account = accountRepository.findByIdAndUserId(accountId, user.id)

        assertThat(account).isNotNull()
        assertThat(account?.userId).isEqualTo(user.id)
    }

    @Test
    fun `should reject invalid account`() {
        // given
        val user = userFixture.createInDb()
        val request = createAccountRequest(
            name = " ",
            openingBalance = BigDecimal("1234567890123456.00000"),
        )

        // when
        mvc.perform(
            post(PathConstants.ACCOUNTS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.message").value(ErrorMessages.VALIDATION_FAILED))
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.openingBalance").exists())
    }

    @Test
    fun `should get account with balance calculated from all operations`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        createTransaction(account, TransactionType.INCOME, BigDecimal("40.50"))
        createTransaction(account, TransactionType.EXPENSE, BigDecimal("10.25"))
        val goal = createGoal(account)
        createGoalTransaction(goal, account, GoalTransactionType.CONTRIBUTION, BigDecimal("25.00"))
        createGoalTransaction(goal, account, GoalTransactionType.REFUND, BigDecimal("5.00"))

        // when
        mvc.perform(
            get("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(account.id.toString()))
            .andExpect(jsonPath("$.openingBalance").value(OPENING_BALANCE.toDouble()))
            .andExpect(jsonPath("$.balance").value(BigDecimal("110.25").toDouble()))
    }

    @Test
    fun `should not expose another user's account`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val account = createAccount(owner.id)

        // when
        mvc.perform(
            get("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(anotherUser)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Account not found"))
    }

    @Test
    fun `should get only authenticated user's accounts`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val firstAccount = createAccount(user.id, name = "Cash")
        val secondAccount = createAccount(user.id, name = "Card")
        createAccount(anotherUser.id, name = "Hidden")

        // when
        mvc.perform(
            get(PathConstants.ACCOUNTS)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(
                jsonPath("$[*].id").value(
                    containsInAnyOrder(
                        firstAccount.id.toString(),
                        secondAccount.id.toString(),
                    ),
                ),
            )
    }

    @Test
    fun `should update account`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val request = UpdateAccountRq(
            name = UPDATED_NAME,
            type = AccountType.BANK,
            openingBalance = UPDATED_OPENING_BALANCE,
        )

        // when
        mvc.perform(
            put("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(UPDATED_NAME))
            .andExpect(jsonPath("$.type").value(AccountType.BANK.name))
            .andExpect(jsonPath("$.openingBalance").value(UPDATED_OPENING_BALANCE.toDouble()))
            .andExpect(jsonPath("$.balance").value(UPDATED_OPENING_BALANCE.toDouble()))
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))

        val updatedAccount = accountRepository.findByIdAndUserId(account.id, user.id)

        assertThat(updatedAccount?.createdAt).isEqualTo(account.createdAt)
        assertThat(updatedAccount?.closedAt).isNull()
    }

    @Test
    fun `should reject update for closed account`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val request = UpdateAccountRq(
            name = UPDATED_NAME,
            type = AccountType.BANK,
            openingBalance = UPDATED_OPENING_BALANCE,
        )

        mvc.perform(
            delete("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNoContent)

        val closedAt = accountRepository.findByIdAndUserId(account.id, user.id)?.closedAt

        // when
        mvc.perform(
            put("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.ACCOUNT_CLOSED))

        val unchangedAccount = accountRepository.findByIdAndUserId(account.id, user.id)

        assertThat(unchangedAccount?.name).isEqualTo(account.name)
        assertThat(unchangedAccount?.type).isEqualTo(account.type)
        assertThat(unchangedAccount?.openingBalance).isEqualByComparingTo(account.openingBalance)
        assertThat(unchangedAccount?.closedAt).isEqualTo(closedAt)
    }

    @Test
    fun `should close account without deleting it`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)

        // when
        mvc.perform(
            delete("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        val closedAccount = accountRepository.findByIdAndUserId(account.id, user.id)

        assertThat(closedAccount).isNotNull()
        assertThat(closedAccount?.closedAt).isNotNull()
    }

    @Test
    fun `should close account idempotently`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)

        mvc.perform(
            delete("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNoContent)

        val firstClosedAt = accountRepository.findByIdAndUserId(account.id, user.id)?.closedAt

        // when
        mvc.perform(
            delete("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(accountRepository.findByIdAndUserId(account.id, user.id)?.closedAt)
            .isEqualTo(firstClosedAt)
    }

    private fun createAccountRequest(
        name: String = NAME,
        openingBalance: BigDecimal = OPENING_BALANCE,
    ): CreateAccountRq =
        CreateAccountRq(
            name = name,
            type = AccountType.CARD,
            openingBalance = openingBalance,
            currency = Currency.EUR,
        )

    private fun createAccount(
        userId: UUID,
        name: String = NAME,
    ): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = AccountType.CARD,
                openingBalance = OPENING_BALANCE,
                currency = Currency.EUR,
                createdAt = OffsetDateTime.now(),
                closedAt = null,
            ),
        )

    private fun createTransaction(
        account: Account,
        type: TransactionType,
        amount: BigDecimal,
    ) {
        val now = OffsetDateTime.now()
        val transaction = Transaction(
            id = UUID.randomUUID(),
            userId = account.userId,
            accountId = account.id,
            type = type,
            amount = amount,
            categoryId = null,
            merchant = null,
            note = null,
            date = now,
            createdAt = now,
            recurringTransactionId = null,
            deletedAt = null,
        )

        dsl.insertInto(Tables.TRANSACTIONS)
            .set(dsl.newRecord(Tables.TRANSACTIONS, transaction))
            .execute()
    }

    private fun createGoal(account: Account): Goal {
        val goal = Goal(
            id = UUID.randomUUID(),
            userId = account.userId,
            name = "Emergency fund",
            targetAmount = BigDecimal("1000.00"),
            currency = account.currency,
            deadline = null,
            status = GoalStatus.ACTIVE,
            achievedAt = null,
            archivedAt = null,
        )

        dsl.insertInto(Tables.GOALS)
            .set(dsl.newRecord(Tables.GOALS, goal))
            .execute()

        return goal
    }

    private fun createGoalTransaction(
        goal: Goal,
        account: Account,
        type: GoalTransactionType,
        amount: BigDecimal,
    ) {
        val now = OffsetDateTime.now()
        val transaction = GoalTransaction(
            id = UUID.randomUUID(),
            userId = account.userId,
            goalId = goal.id,
            accountId = account.id,
            currency = account.currency,
            type = type,
            amount = amount,
            date = now,
            createdAt = now,
        )

        dsl.insertInto(Tables.GOAL_TRANSACTIONS)
            .set(dsl.newRecord(Tables.GOAL_TRANSACTIONS, transaction))
            .execute()
    }

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

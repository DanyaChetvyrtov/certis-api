package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
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
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.dto.request.CreateTransferRq
import ru.digitalhustle.certis.dto.request.ReverseTransferRq
import ru.digitalhustle.certis.dto.request.UpdateTransactionRq
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransferControllerTest : AbstractIntegrationTest() {

    private companion object {
        private val OPENING_BALANCE = BigDecimal("100.00")
        private val AMOUNT = BigDecimal("25.50")
        private val OCCURRED_AT = OffsetDateTime.parse("2026-08-16T10:30:00Z")
        private val REVERSAL_OCCURRED_AT = OffsetDateTime.parse("2026-08-16T11:30:00Z")
    }

    @Test
    fun `should create transfer with linked postings and update both balances`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")

        // when
        val result = mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        createRequest(sourceAccount.id, destinationAccount.id),
                    ),
                ),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.sourceAccountId").value(sourceAccount.id.toString()))
            .andExpect(jsonPath("$.destinationAccountId").value(destinationAccount.id.toString()))
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))
            .andExpect(jsonPath("$.amount").value(AMOUNT.toDouble()))
            .andExpect(jsonPath("$.note").value("Move to savings"))
            .andExpect(jsonPath("$.occurredAt").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn()

        val transferId = UUID.fromString(
            objectMapper.readTree(result.response.contentAsByteArray)["id"].asText(),
        )
        val actualOccurredAt = OffsetDateTime.parse(
            objectMapper.readTree(result.response.contentAsByteArray)["occurredAt"].asText(),
        )
        val postings = dsl.selectFrom(Tables.TRANSACTIONS)
            .where(Tables.TRANSACTIONS.TRANSFER_ID.eq(transferId))
            .fetch()

        assertThat(postings).hasSize(2)
        assertThat(actualOccurredAt.toInstant()).isEqualTo(OCCURRED_AT.toInstant())
        assertThat(postings.map { it.type }).containsExactlyInAnyOrder(
            TransactionType.EXPENSE.name,
            TransactionType.INCOME.name,
        )
        assertThat(postings.single { it.type == TransactionType.EXPENSE.name }.accountId)
            .isEqualTo(sourceAccount.id)
        assertThat(postings.single { it.type == TransactionType.INCOME.name }.accountId)
            .isEqualTo(destinationAccount.id)
        assertThat(postings).allSatisfy { posting ->
            assertThat(posting.amount).isEqualByComparingTo(AMOUNT)
            assertThat(posting.categoryId).isNull()
            assertThat(posting.transferId).isEqualTo(transferId)
        }

        assertAccountBalance(sourceAccount.id, user, BigDecimal("74.50"))
        assertAccountBalance(destinationAccount.id, user, BigDecimal("125.50"))
    }

    @Test
    fun `should get only authenticated user transfers`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")
        val anotherSourceAccount = createAccount(anotherUser.id, "Other checking")
        val anotherDestinationAccount = createAccount(anotherUser.id, "Other savings")
        val transferId = createTransfer(user, sourceAccount, destinationAccount)
        createTransfer(anotherUser, anotherSourceAccount, anotherDestinationAccount)

        // when
        mvc.perform(
            get(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(transferId.toString()))

        mvc.perform(
            get("${PathConstants.TRANSFERS}/$transferId")
                .cookie(accessTokenCookie(anotherUser)),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transfer not found"))
    }

    @Test
    fun `should reject invalid transfer request`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Checking")
        val request = createRequest(account.id, account.id).copy(amount = BigDecimal.ZERO)

        // when
        mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.message").value(ErrorMessages.VALIDATION_FAILED))
            .andExpect(jsonPath("$.errors.amount").exists())
    }

    @Test
    fun `should reject transfer to the same account`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Checking")

        // when
        mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createRequest(account.id, account.id))),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSFER_SAME_ACCOUNT))
    }

    @Test
    fun `should reject transfer involving another user account`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(anotherUser.id, "Other savings")

        // when
        mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        createRequest(sourceAccount.id, destinationAccount.id),
                    ),
                ),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Account not found"))

        assertThat(transferRepository.findAllByUserId(user.id)).isEmpty()
    }

    @Test
    fun `should reject transfer between different currencies`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Euro", Currency.EUR)
        val destinationAccount = createAccount(user.id, "Dollar", Currency.USD)

        // when
        mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        createRequest(sourceAccount.id, destinationAccount.id),
                    ),
                ),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSFER_CURRENCY_MISMATCH))
    }

    @Test
    fun `should reject transfer involving closed account`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(
            userId = user.id,
            name = "Closed savings",
            closedAt = OffsetDateTime.parse("2026-08-16T09:00:00Z"),
        )

        // when
        mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        createRequest(sourceAccount.id, destinationAccount.id),
                    ),
                ),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSFER_ACCOUNT_CLOSED))
    }

    @Test
    fun `should reverse transfer once and restore both balances`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")
        val transferId = createTransfer(user, sourceAccount, destinationAccount)
        val request = reverseRequest()

        // when
        val result = mvc.perform(
            post("${PathConstants.TRANSFERS}/$transferId/reversal")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.sourceAccountId").value(destinationAccount.id.toString()))
            .andExpect(jsonPath("$.destinationAccountId").value(sourceAccount.id.toString()))
            .andExpect(jsonPath("$.reversalOfTransferId").value(transferId.toString()))
            .andExpect(jsonPath("$.currency").value(Currency.EUR.name))
            .andExpect(jsonPath("$.amount").value(AMOUNT.toDouble()))
            .andExpect(jsonPath("$.note").value(request.note))
            .andReturn()

        val response = objectMapper.readTree(result.response.contentAsByteArray)
        val reversalId = UUID.fromString(response["id"].asText())
        val actualOccurredAt = OffsetDateTime.parse(response["occurredAt"].asText())
        val reversalPostings = dsl.selectFrom(Tables.TRANSACTIONS)
            .where(Tables.TRANSACTIONS.TRANSFER_ID.eq(reversalId))
            .fetch()

        assertThat(actualOccurredAt.toInstant()).isEqualTo(REVERSAL_OCCURRED_AT.toInstant())
        assertThat(reversalPostings).hasSize(2)
        assertThat(reversalPostings.single { it.type == TransactionType.EXPENSE.name }.accountId)
            .isEqualTo(destinationAccount.id)
        assertThat(reversalPostings.single { it.type == TransactionType.INCOME.name }.accountId)
            .isEqualTo(sourceAccount.id)
        assertAccountBalance(sourceAccount.id, user, OPENING_BALANCE)
        assertAccountBalance(destinationAccount.id, user, OPENING_BALANCE)
    }

    @Test
    fun `should return existing reversal without duplicate postings`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")
        val transferId = createTransfer(user, sourceAccount, destinationAccount)
        val request = reverseRequest()
        val firstResult = mvc.perform(
            post("${PathConstants.TRANSFERS}/$transferId/reversal")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            .andExpect(status().isCreated)
            .andReturn()
        val reversalId = UUID.fromString(
            objectMapper.readTree(firstResult.response.contentAsByteArray)["id"].asText(),
        )

        // when
        mvc.perform(
            post("${PathConstants.TRANSFERS}/$transferId/reversal")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(reversalId.toString()))

        assertThat(transferRepository.findAllByUserId(user.id)).hasSize(2)
        assertThat(
            dsl.fetchCount(
                Tables.TRANSACTIONS,
                Tables.TRANSACTIONS.USER_ID.eq(user.id),
            ),
        ).isEqualTo(4)
        assertAccountBalance(sourceAccount.id, user, OPENING_BALANCE)
        assertAccountBalance(destinationAccount.id, user, OPENING_BALANCE)
    }

    @Test
    fun `should reject reversal of a reversal`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")
        val transferId = createTransfer(user, sourceAccount, destinationAccount)
        val request = reverseRequest()
        val reverseResult = mvc.perform(
            post("${PathConstants.TRANSFERS}/$transferId/reversal")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            .andExpect(status().isCreated)
            .andReturn()
        val reversalId = UUID.fromString(
            objectMapper.readTree(reverseResult.response.contentAsByteArray)["id"].asText(),
        )

        // when
        mvc.perform(
            post("${PathConstants.TRANSFERS}/$reversalId/reversal")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSFER_REVERSAL_OF_REVERSAL))
    }

    @Test
    fun `should not reverse another user transfer`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")
        val transferId = createTransfer(user, sourceAccount, destinationAccount)

        // when
        mvc.perform(
            post("${PathConstants.TRANSFERS}/$transferId/reversal")
                .cookie(accessTokenCookie(anotherUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(reverseRequest())),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transfer not found"))

        assertThat(transferRepository.findAllByUserId(user.id)).hasSize(1)
        assertThat(transferRepository.findAllByUserId(anotherUser.id)).isEmpty()
    }

    @Test
    fun `should reject independent update and deletion of transfer posting`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")
        val transferId = createTransfer(user, sourceAccount, destinationAccount)
        val posting = dsl.selectFrom(Tables.TRANSACTIONS)
            .where(Tables.TRANSACTIONS.TRANSFER_ID.eq(transferId))
            .fetchAny()!!
        val updateRequest = UpdateTransactionRq(
            accountId = posting.accountId,
            type = TransactionType.valueOf(posting.type),
            amount = AMOUNT,
            categoryId = null,
            merchant = null,
            note = "Changed",
            occurredAt = OCCURRED_AT,
        )

        // when, then
        mvc.perform(
            put("${PathConstants.TRANSACTIONS}/${posting.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(updateRequest)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE))

        mvc.perform(
            delete("${PathConstants.TRANSACTIONS}/${posting.id}")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE))
    }

    @Test
    fun `should roll back transfer and first posting when second posting fails`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Checking")
        val destinationAccount = createAccount(user.id, "Savings")
        dsl.execute(
            """
            CREATE FUNCTION keeper.fail_incoming_transfer_posting()
            RETURNS trigger
            LANGUAGE plpgsql
            AS ${'$'}${'$'}
            BEGIN
                IF NEW.transfer_id IS NOT NULL AND NEW.type = 'INCOME' THEN
                    RAISE EXCEPTION 'posting failed';
                END IF;
                RETURN NEW;
            END;
            ${'$'}${'$'}
            """.trimIndent(),
        )
        dsl.execute(
            """
            CREATE TRIGGER fail_incoming_transfer_posting
            BEFORE INSERT ON keeper.transactions
            FOR EACH ROW
            EXECUTE FUNCTION keeper.fail_incoming_transfer_posting()
            """.trimIndent(),
        )

        // when
        mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        createRequest(sourceAccount.id, destinationAccount.id),
                    ),
                ),
        )
            // then
            .andExpect(status().isInternalServerError)

        assertThat(transferRepository.findAllByUserId(user.id)).isEmpty()
        assertThat(
            dsl.fetchCount(
                Tables.TRANSACTIONS,
                Tables.TRANSACTIONS.USER_ID.eq(user.id),
            ),
        ).isZero()
    }

    private fun createTransfer(
        user: User,
        sourceAccount: Account,
        destinationAccount: Account,
    ): UUID {
        val result = mvc.perform(
            post(PathConstants.TRANSFERS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        createRequest(sourceAccount.id, destinationAccount.id),
                    ),
                ),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return UUID.fromString(
            objectMapper.readTree(result.response.contentAsByteArray)["id"].asText(),
        )
    }

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

    private fun createRequest(
        sourceAccountId: UUID,
        destinationAccountId: UUID,
    ): CreateTransferRq =
        CreateTransferRq(
            sourceAccountId = sourceAccountId,
            destinationAccountId = destinationAccountId,
            amount = AMOUNT,
            note = "Move to savings",
            occurredAt = OCCURRED_AT,
        )

    private fun reverseRequest(): ReverseTransferRq =
        ReverseTransferRq(
            note = "Undo transfer",
            occurredAt = REVERSAL_OCCURRED_AT,
        )

    private fun createAccount(
        userId: UUID,
        name: String,
        currency: Currency = Currency.EUR,
        closedAt: OffsetDateTime? = null,
    ): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = AccountType.BANK,
                openingBalance = OPENING_BALANCE,
                currency = currency,
                createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
                closedAt = closedAt,
            ),
        )

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

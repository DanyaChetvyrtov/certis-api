package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.api.dto.request.TransactionFilterRq
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.account.enums.AccountType
import ru.digitalhustle.certis.features.account.model.Account
import ru.digitalhustle.certis.features.security.model.User
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.model.Transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class TransactionQueryControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val FILTERED_TRANSACTION_COUNT = 3
        private val OPENING_BALANCE = BigDecimal("100.00")
        private val AMOUNT = BigDecimal("25.50")
        private val TRANSACTION_DATE = OffsetDateTime.parse("2026-08-07T12:30:00Z")
    }

    @Test
    fun `should filter and paginate transactions`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val anotherAccount = createAccount(user.id, name = "Cash")
        val oldest = createTransaction(account, occurredAt = TRANSACTION_DATE.minusDays(2))
        createTransaction(account, occurredAt = TRANSACTION_DATE.minusDays(1))
        createTransaction(account, occurredAt = TRANSACTION_DATE)
        createTransaction(account, type = TransactionType.INCOME, occurredAt = TRANSACTION_DATE.plusDays(1))
        createTransaction(anotherAccount, occurredAt = TRANSACTION_DATE.plusDays(2))

        // when
        mvc.perform(
            get(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .param("accountId", account.id.toString())
                .param("type", TransactionType.EXPENSE.name)
                .param("from", TRANSACTION_DATE.minusDays(2).toString())
                .param("to", TRANSACTION_DATE.toString())
                .param("page", "1")
                .param("size", "2"),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(oldest.id.toString()))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(FILTERED_TRANSACTION_COUNT))
            .andExpect(jsonPath("$.totalPages").value(2))
    }

    @Test
    fun `should preserve query JSON contract and exclude deleted or foreign transactions`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val active = createTransaction(account, occurredAt = TRANSACTION_DATE)
        val deleted = createTransaction(account, occurredAt = TRANSACTION_DATE.plusDays(1))
        transactionRepository.softDelete(deleted.id, user.id, deleted.createdAt.plusSeconds(1))
        val anotherUser = userFixture.createInDb { copy(email = "another@test.com") }
        createTransaction(createAccount(anotherUser.id), occurredAt = TRANSACTION_DATE)

        // when
        val response = mvc.perform(
            get(PathConstants.TRANSACTIONS).cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(active.id.toString()))
            .andExpect(jsonPath("$.items[0].accountId").value(account.id.toString()))
            .andExpect(jsonPath("$.items[0].type").value(TransactionType.EXPENSE.name))
            .andExpect(jsonPath("$.items[0].amount").value(AMOUNT.toDouble()))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andReturn().response.contentAsString

        val json = objectMapper.readTree(response)
        assertThat(json.fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("items", "page", "size", "totalElements", "totalPages")
        assertThat(json.get("items").get(0).fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "id",
            "accountId",
            "type",
            "amount",
            "occurredAt",
            "createdAt",
            "updatedAt",
        )

        mvc.perform(
            get("${PathConstants.TRANSACTIONS}/${deleted.id}").cookie(accessTokenCookie(user)),
        ).andExpect(status().isNotFound)
        mvc.perform(
            get("${PathConstants.TRANSACTIONS}/${active.id}").cookie(accessTokenCookie(anotherUser)),
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `should reject invalid transaction filters`() {
        // given
        val user = userFixture.createInDb()

        // when
        mvc.perform(
            get(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .param("from", TRANSACTION_DATE.plusDays(1).toString())
                .param("to", TRANSACTION_DATE.toString())
                .param("size", (TransactionFilterRq.MAX_SIZE + 1).toString()),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.VALIDATION_FAILED))
            .andExpect(jsonPath("$.errors.size").exists())
            .andExpect(jsonPath("$.errors.validDateRange").exists())
    }

    private fun createAccount(
        userId: UUID,
        name: String = "Main card",
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
        type: TransactionType = TransactionType.EXPENSE,
        occurredAt: OffsetDateTime,
    ): Transaction =
        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = null,
                recurringTransactionTemplateId = null,
                type = type,
                amount = AMOUNT,
                merchant = null,
                note = null,
                scheduledFor = null,
                occurredAt = occurredAt,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
                deletedAt = null,
            ),
        )

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

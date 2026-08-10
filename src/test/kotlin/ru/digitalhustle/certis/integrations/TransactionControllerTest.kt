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
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.dto.request.CreateTransactionRq
import ru.digitalhustle.certis.dto.request.TransactionFilterRq
import ru.digitalhustle.certis.dto.request.UpdateTransactionRq
import ru.digitalhustle.certis.enums.AccountType
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

class TransactionControllerTest : AbstractIntegrationTest() {

    private companion object {
        private val OPENING_BALANCE = BigDecimal("100.00")
        private val AMOUNT = BigDecimal("25.50")
        private val TRANSACTION_DATE = OffsetDateTime.parse("2026-08-07T12:30:00Z")
    }

    @Test
    fun `should create transaction for authenticated user`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val category = createCategory(user.id, CategoryType.EXPENSE)
        val request = createTransactionRequest(account.id, category.id)

        // when
        val result = mvc.perform(
            post(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.accountId").value(account.id.toString()))
            .andExpect(jsonPath("$.type").value(TransactionType.EXPENSE.name))
            .andExpect(jsonPath("$.amount").value(AMOUNT.toDouble()))
            .andExpect(jsonPath("$.categoryId").value(category.id.toString()))
            .andExpect(jsonPath("$.merchant").value("Coffee shop"))
            .andExpect(jsonPath("$.note").value("Lunch"))
            .andExpect(jsonPath("$.occurredAt").value(TRANSACTION_DATE.toInstant().toString()))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty())
            .andExpect(jsonPath("$.recurringTransactionTemplateId").doesNotExist())
            .andExpect(jsonPath("$.scheduledFor").doesNotExist())
            .andReturn()

        val transactionId = UUID.fromString(
            objectMapper.readTree(result.response.contentAsByteArray)["id"].asText(),
        )

        assertThat(transactionRepository.findByIdAndUserId(transactionId, user.id)).isNotNull()
    }

    @Test
    fun `should reject invalid transaction`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val request = createTransactionRequest(account.id).copy(
            amount = BigDecimal.ZERO,
            merchant = " ",
        )

        // when
        mvc.perform(
            post(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.message").value(ErrorMessages.VALIDATION_FAILED))
            .andExpect(jsonPath("$.errors.amount").exists())
            .andExpect(jsonPath("$.errors.merchant").exists())
    }

    @Test
    fun `should reject transaction for another user's account`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val account = createAccount(owner.id)

        // when
        mvc.perform(
            post(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(anotherUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createTransactionRequest(account.id))),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Account not found"))
    }

    @Test
    fun `should reject transaction for closed account`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        accountRepository.close(account.id, user.id, OffsetDateTime.now())

        // when
        mvc.perform(
            post(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createTransactionRequest(account.id))),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_ACCOUNT_CLOSED))
    }

    @Test
    fun `should reject category with different transaction type`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val category = createCategory(user.id, CategoryType.INCOME)

        // when
        mvc.perform(
            post(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createTransactionRequest(account.id, category.id))),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH))
    }

    @Test
    fun `should reject transaction with archived category`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val category = createCategory(
            userId = user.id,
            type = CategoryType.EXPENSE,
            archivedAt = OffsetDateTime.now(),
        )

        // when
        mvc.perform(
            post(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createTransactionRequest(account.id, category.id))),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_CATEGORY_ARCHIVED))
    }

    @Test
    fun `should not expose another user's transaction`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val account = createAccount(owner.id)
        val transaction = createTransaction(account)

        // when
        mvc.perform(
            get("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(anotherUser)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transaction not found"))
    }

    @Test
    fun `should get only authenticated user's transactions`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-user@test.com")
        }
        val account = createAccount(user.id)
        val anotherAccount = createAccount(anotherUser.id)
        val firstTransaction = createTransaction(account, amount = BigDecimal("10.00"))
        val secondTransaction = createTransaction(account, amount = BigDecimal("20.00"))
        createTransaction(anotherAccount)

        // when
        mvc.perform(
            get(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(TransactionFilterRq.DEFAULT_SIZE))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(
                jsonPath("$.items[*].id").value(
                    containsInAnyOrder(
                        firstTransaction.id.toString(),
                        secondTransaction.id.toString(),
                    ),
                ),
            )
    }

    @Test
    fun `should update transaction`() {
        // given
        val user = userFixture.createInDb()
        val firstAccount = createAccount(user.id)
        val secondAccount = createAccount(user.id, name = "Cash")
        val transaction = createTransaction(firstAccount)
        val createdAt = transaction.createdAt
        val request = UpdateTransactionRq(
            accountId = secondAccount.id,
            type = TransactionType.INCOME,
            amount = BigDecimal("80.00"),
            categoryId = null,
            merchant = "Employer",
            note = "Bonus",
            occurredAt = TRANSACTION_DATE.plusDays(1),
        )

        // when
        mvc.perform(
            put("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountId").value(secondAccount.id.toString()))
            .andExpect(jsonPath("$.type").value(TransactionType.INCOME.name))
            .andExpect(jsonPath("$.amount").value(request.amount.toDouble()))
            .andExpect(jsonPath("$.merchant").value("Employer"))
            .andExpect(jsonPath("$.note").value("Bonus"))
            .andExpect(jsonPath("$.occurredAt").value(request.occurredAt.toInstant().toString()))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty())

        val updatedTransaction = checkNotNull(
            transactionRepository.findByIdAndUserId(transaction.id, user.id),
        )

        assertThat(updatedTransaction.createdAt).isEqualTo(createdAt)
        assertThat(updatedTransaction.updatedAt).isAfterOrEqualTo(createdAt)
    }

    @Test
    fun `should allow correcting historical transaction on its closed account`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val transaction = createTransaction(account)
        accountRepository.close(account.id, user.id, OffsetDateTime.now())
        val request = UpdateTransactionRq(
            accountId = account.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("30.00"),
            categoryId = null,
            merchant = "Corrected merchant",
            note = null,
            occurredAt = TRANSACTION_DATE,
        )

        // when
        mvc.perform(
            put("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountId").value(account.id.toString()))
            .andExpect(jsonPath("$.amount").value(request.amount.toDouble()))
            .andExpect(jsonPath("$.merchant").value(request.merchant))
    }

    @Test
    fun `should not update another user's transaction`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-update-user@test.com")
        }
        val ownerAccount = createAccount(owner.id)
        val anotherAccount = createAccount(anotherUser.id)
        val transaction = createTransaction(ownerAccount)
        val request = UpdateTransactionRq(
            accountId = anotherAccount.id,
            type = TransactionType.INCOME,
            amount = BigDecimal("80.00"),
            categoryId = null,
            merchant = "Employer",
            note = "Bonus",
            occurredAt = TRANSACTION_DATE.plusDays(1),
        )

        // when
        mvc.perform(
            put("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(anotherUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transaction not found"))

        assertThat(transactionRepository.findByIdAndUserId(transaction.id, owner.id))
            .isEqualTo(transaction)
    }

    @Test
    fun `should reject moving transaction to closed account`() {
        // given
        val user = userFixture.createInDb()
        val activeAccount = createAccount(user.id)
        val closedAccount = createAccount(user.id, name = "Closed account")
        val transaction = createTransaction(activeAccount)
        accountRepository.close(closedAccount.id, user.id, OffsetDateTime.now())
        val request = UpdateTransactionRq(
            accountId = closedAccount.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("80.00"),
            categoryId = null,
            merchant = null,
            note = null,
            occurredAt = TRANSACTION_DATE.plusDays(1),
        )

        // when
        mvc.perform(
            put("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_ACCOUNT_CLOSED))

        assertThat(transactionRepository.findByIdAndUserId(transaction.id, user.id)?.accountId)
            .isEqualTo(activeAccount.id)
    }

    @Test
    fun `should keep existing archived category when updating transaction`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val category = createCategory(user.id, CategoryType.EXPENSE)
        val transaction = createTransaction(account, categoryId = category.id)
        val request = UpdateTransactionRq(
            accountId = account.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("80.00"),
            categoryId = category.id,
            merchant = "Updated merchant",
            note = "Updated note",
            occurredAt = TRANSACTION_DATE.plusDays(1),
        )
        categoryRepository.archive(category.id, user.id, OffsetDateTime.now())

        // when
        mvc.perform(
            put("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.categoryId").value(category.id.toString()))
            .andExpect(jsonPath("$.amount").value(request.amount.toDouble()))
    }

    @Test
    fun `should reject assigning archived category when updating transaction`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val transaction = createTransaction(account)
        val category = createCategory(
            userId = user.id,
            type = CategoryType.EXPENSE,
            archivedAt = OffsetDateTime.now(),
        )
        val request = UpdateTransactionRq(
            accountId = account.id,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("80.00"),
            categoryId = category.id,
            merchant = null,
            note = null,
            occurredAt = TRANSACTION_DATE.plusDays(1),
        )

        // when
        mvc.perform(
            put("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_CATEGORY_ARCHIVED))
    }

    @Test
    fun `should soft delete transaction and remove its balance effect`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val transaction = createTransaction(account, amount = AMOUNT)

        mvc.perform(
            get("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(BigDecimal("74.50").toDouble()))

        // when
        mvc.perform(
            delete("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        mvc.perform(
            get("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNotFound)

        mvc.perform(
            get(PathConstants.TRANSACTIONS)
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(0))

        mvc.perform(
            get("${PathConstants.ACCOUNTS}/${account.id}")
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(OPENING_BALANCE.toDouble()))

        assertThat(transactionRepository.existsIncludingDeletedByIdAndUserId(transaction.id, user.id)).isTrue()

        mvc.perform(
            delete("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(user)),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `should not delete another user's transaction`() {
        // given
        val owner = userFixture.createInDb()
        val anotherUser = userFixture.createInDb {
            copy(email = "another-delete-user@test.com")
        }
        val account = createAccount(owner.id)
        val transaction = createTransaction(account)

        // when
        mvc.perform(
            delete("${PathConstants.TRANSACTIONS}/${transaction.id}")
                .cookie(accessTokenCookie(anotherUser)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transaction not found"))

        assertThat(transactionRepository.findByIdAndUserId(transaction.id, owner.id))
            .isEqualTo(transaction)
    }

    private fun createTransactionRequest(
        accountId: UUID,
        categoryId: UUID? = null,
    ): CreateTransactionRq =
        CreateTransactionRq(
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = AMOUNT,
            categoryId = categoryId,
            merchant = "Coffee shop",
            note = "Lunch",
            occurredAt = TRANSACTION_DATE,
        )

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

    private fun createCategory(
        userId: UUID,
        type: CategoryType,
        archivedAt: OffsetDateTime? = null,
    ): Category {
        val category = Category(
            id = UUID.randomUUID(),
            userId = userId,
            name = "Category",
            type = type,
            icon = "wallet",
            color = "#10B981",
            archivedAt = archivedAt,
        )

        dsl.insertInto(Tables.CATEGORIES)
            .set(dsl.newRecord(Tables.CATEGORIES, category))
            .execute()

        return category
    }

    private fun createTransaction(
        account: Account,
        amount: BigDecimal = AMOUNT,
        categoryId: UUID? = null,
    ): Transaction =
        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = categoryId,
                recurringTransactionTemplateId = null,
                type = TransactionType.EXPENSE,
                amount = amount,
                merchant = null,
                note = null,
                scheduledFor = null,
                occurredAt = TRANSACTION_DATE,
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

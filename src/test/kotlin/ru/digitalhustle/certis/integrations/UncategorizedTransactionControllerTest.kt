package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.dto.request.AssignTransactionsCategoryRq
import ru.digitalhustle.certis.dto.request.TransactionCategoryAssignmentRq
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.entity.User
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class UncategorizedTransactionControllerTest : AbstractIntegrationTest() {

    private companion object {
        const val MONTH = "2026-09"
        private val OCCURRED_AT = OffsetDateTime.parse("2026-09-03T14:32:00Z")
        private val AMOUNT = BigDecimal("500.00")
    }

    @Test
    fun `should return requested page of uncategorized transactions`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb { copy(email = "uncategorized-other@test.com") }
        val mainAccount = createAccount(user.id, "Tinkoff Black", Currency.RUB)
        val cashAccount = createAccount(user.id, "Cash", Currency.RUB)
        val euroAccount = createAccount(user.id, "Euro card", Currency.EUR)
        val anotherAccount = createAccount(anotherUser.id, "Other card", Currency.RUB)
        val category = createCategory(user.id, CategoryType.EXPENSE)

        val first = createTransaction(
            account = mainAccount,
            occurredAt = OCCURRED_AT,
            merchant = "Pyaterochka",
            note = "Card purchase",
        )
        val second = createTransaction(
            account = cashAccount,
            occurredAt = OCCURRED_AT.minusDays(1),
            amount = BigDecimal("200.00"),
            merchant = "Market",
        )
        createTransaction(mainAccount, categoryId = category.id)
        createTransaction(mainAccount, type = TransactionType.INCOME)
        createTransaction(mainAccount, occurredAt = OffsetDateTime.parse("2026-08-30T23:59:59Z"))
        createTransaction(euroAccount)
        createTransaction(anotherAccount)

        val deleted = createTransaction(mainAccount)
        transactionRepository.softDelete(deleted.id, user.id, OffsetDateTime.now())

        val transfer = createTransfer(user.id, mainAccount, cashAccount)
        createTransaction(mainAccount, transferId = transfer.id)

        // when
        mvc.perform(
            get("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_UNCATEGORIZED}")
                .cookie(accessTokenCookie(user))
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", TransactionType.EXPENSE.name)
                .param("page", "0")
                .param("size", "20"),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.month").value(MONTH))
            .andExpect(jsonPath("$.currency").value(Currency.RUB.name))
            .andExpect(jsonPath("$.type").value(TransactionType.EXPENSE.name))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value(first.id.toString()))
            .andExpect(jsonPath("$.items[0].merchant").value("Pyaterochka"))
            .andExpect(jsonPath("$.items[0].note").value("Card purchase"))
            .andExpect(jsonPath("$.items[0].amount").value(AMOUNT.toDouble()))
            .andExpect(jsonPath("$.items[0].account.id").value(mainAccount.id.toString()))
            .andExpect(jsonPath("$.items[0].account.name").value(mainAccount.name))
            .andExpect(jsonPath("$.items[0].account.type").value(mainAccount.type.name))
            .andExpect(jsonPath("$.items[1].id").value(second.id.toString()))
    }

    @Test
    fun `should filter uncategorized transactions by account and search`() {
        // given
        val user = userFixture.createInDb()
        val mainAccount = createAccount(user.id, "Main card", Currency.RUB)
        val cashAccount = createAccount(user.id, "Cash", Currency.RUB)
        val expected = createTransaction(
            account = mainAccount,
            merchant = "Pyaterochka",
            note = null,
        )
        createTransaction(mainAccount, merchant = "Pharmacy", note = "Medicine")
        createTransaction(cashAccount, merchant = null, note = "Pyaterochka receipt")

        // when
        mvc.perform(
            get("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_UNCATEGORIZED}")
                .cookie(accessTokenCookie(user))
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", TransactionType.EXPENSE.name)
                .param("accountId", mainAccount.id.toString())
                .param("search", "  PYATERO  "),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(expected.id.toString()))
    }

    @Test
    fun `should paginate uncategorized transactions deterministically`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Main card", Currency.RUB)
        createTransaction(account, occurredAt = OCCURRED_AT)
        val expected = createTransaction(account, occurredAt = OCCURRED_AT.minusDays(1))

        // when
        mvc.perform(
            get("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_UNCATEGORIZED}")
                .cookie(accessTokenCookie(user))
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", TransactionType.EXPENSE.name)
                .param("page", "1")
                .param("size", "1"),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(expected.id.toString()))
    }

    @Test
    fun `should validate uncategorized transaction filters`() {
        // given
        val user = userFixture.createInDb()

        // when
        mvc.perform(
            get("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_UNCATEGORIZED}")
                .cookie(accessTokenCookie(user))
                .param("month", "invalid")
                .param("currency", Currency.RUB.name)
                .param("type", TransactionType.EXPENSE.name)
                .param("size", "0"),
        )
            // then
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should require authentication for uncategorized transaction search`() {
        // when
        mvc.perform(
            get("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_UNCATEGORIZED}")
                .param("month", MONTH)
                .param("currency", Currency.RUB.name)
                .param("type", TransactionType.EXPENSE.name),
        )
            // then
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should assign different categories to selected uncategorized transactions`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Main card", Currency.RUB)
        val categories = listOf(
            createCategory(user.id, CategoryType.EXPENSE),
            createCategory(user.id, CategoryType.EXPENSE),
        )
        val transactions = listOf(
            createTransaction(account),
            createTransaction(account, occurredAt = OCCURRED_AT.minusDays(1)),
        )
        val request = assignmentRequest(
            transactions.zip(categories).map { (transaction, category) ->
                transaction.id to category.id
            },
        )

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isNoContent)

        transactions.zip(categories).forEach { (transaction, category) ->
            assertThat(transactionRepository.findByIdAndUserId(transaction.id, user.id)?.categoryId)
                .isEqualTo(category.id)
        }
    }

    @Test
    fun `should keep batch unchanged when one transaction belongs to another user`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb { copy(email = "assignment-other@test.com") }
        val account = createAccount(user.id, "Main card", Currency.RUB)
        val anotherAccount = createAccount(anotherUser.id, "Other card", Currency.RUB)
        val category = createCategory(user.id, CategoryType.EXPENSE)
        val ownedTransaction = createTransaction(account)
        val anotherTransaction = createTransaction(anotherAccount)
        val request = assignmentRequest(
            setOf(ownedTransaction.id, anotherTransaction.id),
            category.id,
        )

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Transaction not found"))

        assertThat(transactionRepository.findByIdAndUserId(ownedTransaction.id, user.id)?.categoryId)
            .isNull()
        assertThat(transactionRepository.findByIdAndUserId(anotherTransaction.id, anotherUser.id)?.categoryId)
            .isNull()
    }

    @Test
    fun `should not expose another user's category during assignment`() {
        // given
        val user = userFixture.createInDb()
        val anotherUser = userFixture.createInDb { copy(email = "category-owner@test.com") }
        val account = createAccount(user.id, "Main card", Currency.RUB)
        val transaction = createTransaction(account)
        val anotherCategory = createCategory(anotherUser.id, CategoryType.EXPENSE)
        val request = assignmentRequest(setOf(transaction.id), anotherCategory.id)

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Category not found"))

        assertThat(transactionRepository.findByIdAndUserId(transaction.id, user.id)?.categoryId)
            .isNull()
    }

    @Test
    fun `should reject archived category assignment`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Main card", Currency.RUB)
        val transaction = createTransaction(account)
        val category = createCategory(
            userId = user.id,
            type = CategoryType.EXPENSE,
            archivedAt = OffsetDateTime.now(),
        )
        val request = assignmentRequest(setOf(transaction.id), category.id)

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_CATEGORY_ARCHIVED))

        assertThat(transactionRepository.findByIdAndUserId(transaction.id, user.id)?.categoryId)
            .isNull()
    }

    @Test
    fun `should reject assignment when transaction type does not match category`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Main card", Currency.RUB)
        val transaction = createTransaction(account, type = TransactionType.EXPENSE)
        val category = createCategory(user.id, CategoryType.INCOME)
        val request = assignmentRequest(setOf(transaction.id), category.id)

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH))

        assertThat(transactionRepository.findByIdAndUserId(transaction.id, user.id)?.categoryId)
            .isNull()
    }

    @Test
    fun `should reject assignment when any transaction is already categorized`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Main card", Currency.RUB)
        val category = createCategory(user.id, CategoryType.EXPENSE)
        val anotherCategory = createCategory(user.id, CategoryType.EXPENSE)
        val uncategorized = createTransaction(account)
        val categorized = createTransaction(account, categoryId = anotherCategory.id)
        val request = assignmentRequest(setOf(uncategorized.id, categorized.id), category.id)

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_ALREADY_CATEGORIZED))

        assertThat(transactionRepository.findByIdAndUserId(uncategorized.id, user.id)?.categoryId)
            .isNull()
        assertThat(transactionRepository.findByIdAndUserId(categorized.id, user.id)?.categoryId)
            .isEqualTo(anotherCategory.id)
    }

    @Test
    fun `should reject category assignment to transfer posting`() {
        // given
        val user = userFixture.createInDb()
        val sourceAccount = createAccount(user.id, "Source", Currency.RUB)
        val destinationAccount = createAccount(user.id, "Destination", Currency.RUB)
        val category = createCategory(user.id, CategoryType.EXPENSE)
        val transfer = createTransfer(user.id, sourceAccount, destinationAccount)
        val posting = createTransaction(sourceAccount, transferId = transfer.id)
        val request = assignmentRequest(setOf(posting.id), category.id)

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE))

        assertThat(transactionRepository.findByIdAndUserId(posting.id, user.id)?.categoryId)
            .isNull()
    }

    @Test
    fun `should validate category assignment request`() {
        // given
        val user = userFixture.createInDb()
        val request = assignmentRequest(emptySet(), UUID.randomUUID())

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.assignments").exists())
    }

    @Test
    fun `should reject duplicate transaction assignments without changing it`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id, "Main card", Currency.RUB)
        val transaction = createTransaction(account)
        val firstCategory = createCategory(user.id, CategoryType.EXPENSE)
        val secondCategory = createCategory(user.id, CategoryType.EXPENSE)
        val request = assignmentRequest(
            listOf(
                transaction.id to firstCategory.id,
                transaction.id to secondCategory.id,
            ),
        )

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(ErrorMessages.TRANSACTION_DUPLICATE_CATEGORY_ASSIGNMENTS))

        assertThat(transactionRepository.findByIdAndUserId(transaction.id, user.id)?.categoryId)
            .isNull()
    }

    @Test
    fun `should require authentication for category assignment`() {
        // given
        val request = assignmentRequest(setOf(UUID.randomUUID()), UUID.randomUUID())

        // when
        mvc.perform(
            patch("${PathConstants.TRANSACTIONS}${PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isUnauthorized)
    }

    private fun assignmentRequest(
        transactionIds: Set<UUID>,
        categoryId: UUID,
    ): AssignTransactionsCategoryRq =
        assignmentRequest(
            transactionIds.map { transactionId -> transactionId to categoryId },
        )

    private fun assignmentRequest(
        assignments: List<Pair<UUID, UUID>>,
    ): AssignTransactionsCategoryRq =
        AssignTransactionsCategoryRq(
            assignments = assignments.map { (transactionId, categoryId) ->
                TransactionCategoryAssignmentRq(
                    transactionId = transactionId,
                    categoryId = categoryId,
                )
            },
        )

    private fun createAccount(
        userId: UUID,
        name: String,
        currency: Currency,
    ): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = name,
                type = AccountType.CARD,
                openingBalance = BigDecimal("1000.00"),
                currency = currency,
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
            name = "Category-${UUID.randomUUID()}",
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
        categoryId: UUID? = null,
        type: TransactionType = TransactionType.EXPENSE,
        amount: BigDecimal = AMOUNT,
        merchant: String? = null,
        note: String? = null,
        occurredAt: OffsetDateTime = OCCURRED_AT,
        transferId: UUID? = null,
    ): Transaction =
        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = categoryId,
                recurringTransactionTemplateId = null,
                type = type,
                amount = amount,
                merchant = merchant,
                note = note,
                scheduledFor = null,
                occurredAt = occurredAt,
                createdAt = occurredAt,
                updatedAt = occurredAt,
                deletedAt = null,
                transferId = transferId,
            ),
        )

    private fun createTransfer(
        userId: UUID,
        sourceAccount: Account,
        destinationAccount: Account,
    ): Transfer =
        transferRepository.insert(
            Transfer(
                id = UUID.randomUUID(),
                userId = userId,
                sourceAccountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                currency = sourceAccount.currency,
                amount = AMOUNT,
                note = null,
                occurredAt = OCCURRED_AT,
                createdAt = OCCURRED_AT,
            ),
        )

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )
}

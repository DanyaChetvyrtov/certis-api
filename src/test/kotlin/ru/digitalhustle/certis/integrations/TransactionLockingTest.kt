package ru.digitalhustle.certis.integrations

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.Transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class TransactionLockingTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `should serialize concurrent transaction row locks`() {
        // given
        val user = userFixture.createInDb()
        val account = createAccount(user.id)
        val transaction = createTransaction(account)
        val firstLockAcquired = CountDownLatch(1)
        val releaseFirstLock = CountDownLatch(1)
        val secondLockStarted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val firstLock = executor.submit<Transaction?> {
                TransactionTemplate(transactionManager).execute {
                    val lockedTransaction =
                        transactionRepository.findByIdAndUserIdForUpdate(transaction.id, user.id)
                    firstLockAcquired.countDown()
                    check(releaseFirstLock.await(5, TimeUnit.SECONDS))
                    lockedTransaction
                }
            }

            assertThat(firstLockAcquired.await(5, TimeUnit.SECONDS)).isTrue()

            val secondLock = executor.submit<Transaction?> {
                secondLockStarted.countDown()
                TransactionTemplate(transactionManager).execute {
                    transactionRepository.findByIdAndUserIdForUpdate(transaction.id, user.id)
                }
            }

            assertThat(secondLockStarted.await(5, TimeUnit.SECONDS)).isTrue()
            assertThatThrownBy {
                secondLock.get(250, TimeUnit.MILLISECONDS)
            }.isInstanceOf(TimeoutException::class.java)

            releaseFirstLock.countDown()

            assertThat(firstLock.get(5, TimeUnit.SECONDS)).isEqualTo(transaction)
            assertThat(secondLock.get(5, TimeUnit.SECONDS)).isEqualTo(transaction)
        } finally {
            releaseFirstLock.countDown()
            executor.shutdownNow()
        }
    }

    private fun createAccount(userId: UUID): Account =
        accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = userId,
                name = "Main card",
                type = AccountType.CARD,
                openingBalance = BigDecimal("100.00"),
                currency = Currency.EUR,
                createdAt = OffsetDateTime.parse("2026-08-09T10:00:00Z"),
                closedAt = null,
            ),
        )

    private fun createTransaction(account: Account): Transaction =
        transactionRepository.insert(
            Transaction(
                id = UUID.randomUUID(),
                userId = account.userId,
                accountId = account.id,
                categoryId = null,
                recurringTransactionTemplateId = null,
                type = TransactionType.EXPENSE,
                amount = BigDecimal("25.50"),
                merchant = null,
                note = null,
                scheduledFor = null,
                occurredAt = OffsetDateTime.parse("2026-08-09T11:00:00Z"),
                createdAt = OffsetDateTime.parse("2026-08-09T12:00:00Z"),
                updatedAt = OffsetDateTime.parse("2026-08-09T12:00:00Z"),
                deletedAt = null,
            ),
        )
}

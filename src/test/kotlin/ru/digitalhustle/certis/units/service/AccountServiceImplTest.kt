package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.NewAccount
import ru.digitalhustle.certis.model.UpdateAccountData
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.repository.AccountRepository
import ru.digitalhustle.certis.service.domain.impl.AccountServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class AccountServiceImplTest {

    private val accountRepository = mock(AccountRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-07-31T10:15:30Z"), ZoneOffset.UTC)
    private val accountService = AccountServiceImpl(accountRepository, clock)

    private companion object {
        private const val NAME = "Main card"
        private const val UPDATED_NAME = "Salary card"
        private val OPENING_BALANCE = BigDecimal("100.00")
        private val UPDATED_OPENING_BALANCE = BigDecimal("150.00")
        private val TRANSACTION_DELTA = BigDecimal("25.50")
    }

    @Test
    fun `should get account with calculated balance`() {
        // given
        val account = createAccount()

        `when`(accountRepository.findByIdAndUserId(account.id, account.userId))
            .thenReturn(account)
        `when`(accountRepository.findBalanceDeltas(account.userId, listOf(account.id)))
            .thenReturn(mapOf(account.id to TRANSACTION_DELTA))

        // when
        val result = accountService.getById(account.id, account.userId)

        // then
        assertThat(result.id).isEqualTo(account.id)
        assertThat(result.balance).isEqualByComparingTo(OPENING_BALANCE + TRANSACTION_DELTA)
    }

    @Test
    fun `should throw not found when account does not belong to user`() {
        // given
        val accountId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(accountRepository.findByIdAndUserId(accountId, userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            accountService.getById(accountId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should get account with shared lock`() {
        // given
        val account = createAccount()

        `when`(accountRepository.findByIdAndUserIdForShare(account.id, account.userId))
            .thenReturn(account)

        // when
        val result = accountService.getByIdForShare(account.id, account.userId)

        // then
        assertThat(result).isEqualTo(account)
    }

    @Test
    fun `should return empty account list`() {
        // given
        val userId = UUID.randomUUID()

        `when`(accountRepository.findAllByUserId(userId))
            .thenReturn(emptyList())

        // when
        val result = accountService.getAllByUserId(userId)

        // then
        assertThat(result).isEmpty()
        verify(accountRepository, never())
            .findBalanceDeltas(userId, emptyList())
    }

    @Test
    fun `should save account`() {
        // given
        val newAccount = createNewAccount()
        val accountCaptor = ArgumentCaptor.forClass(Account::class.java)

        `when`(accountRepository.insert(captureAccount(accountCaptor)))
            .thenAnswer { accountCaptor.value }

        // when
        val result = accountService.save(newAccount)

        // then
        assertAll(
            { assertThat(result.name).isEqualTo(newAccount.name) },
            { assertThat(result.balance).isEqualByComparingTo(newAccount.openingBalance) },
            { assertThat(accountCaptor.value.userId).isEqualTo(newAccount.userId) },
            { assertThat(accountCaptor.value.createdAt).isEqualTo(OffsetDateTime.now(clock)) },
            { assertThat(accountCaptor.value.closedAt).isNull() },
        )
    }

    @Test
    fun `should update active account`() {
        // given
        val savedAccount = createAccount()
        val updateData = createUpdateAccountData(
            id = savedAccount.id,
            userId = savedAccount.userId,
        )
        val updatedAccount = savedAccount.copy(
            name = updateData.name,
            type = updateData.type,
            openingBalance = updateData.openingBalance,
        )

        `when`(accountRepository.updateActive(updateData))
            .thenReturn(updatedAccount)
        `when`(accountRepository.findBalanceDeltas(savedAccount.userId, listOf(savedAccount.id)))
            .thenReturn(emptyMap())

        // when
        val result = accountService.update(updateData)

        // then
        assertAll(
            { assertThat(result.name).isEqualTo(UPDATED_NAME) },
            { assertThat(result.openingBalance).isEqualByComparingTo(UPDATED_OPENING_BALANCE) },
            { assertThat(result.currency).isEqualTo(savedAccount.currency) },
            { assertThat(result.createdAt).isEqualTo(savedAccount.createdAt) },
            { assertThat(result.closedAt).isNull() },
        )
        verify(accountRepository, never())
            .findByIdAndUserId(savedAccount.id, savedAccount.userId)
    }

    @Test
    fun `should reject update when account is closed`() {
        // given
        val account = createAccount(closedAt = OffsetDateTime.now(clock))
        val updateData = createUpdateAccountData(
            id = account.id,
            userId = account.userId,
        )

        `when`(accountRepository.updateActive(updateData))
            .thenReturn(null)
        `when`(accountRepository.findByIdAndUserId(account.id, account.userId))
            .thenReturn(account)

        // when, then
        assertThatThrownBy {
            accountService.update(updateData)
        }
            .isInstanceOf(AccountClosedException::class.java)
            .hasMessage(ErrorMessages.ACCOUNT_CLOSED)

        verify(accountRepository, never())
            .findBalanceDeltas(account.userId, listOf(account.id))
    }

    @Test
    fun `should throw not found when updating missing account`() {
        // given
        val updateData = createUpdateAccountData()

        `when`(accountRepository.updateActive(updateData))
            .thenReturn(null)
        `when`(accountRepository.findByIdAndUserId(updateData.id, updateData.userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            accountService.update(updateData)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should close account`() {
        // given
        val account = createAccount()

        `when`(accountRepository.close(account.id, account.userId, OffsetDateTime.now(clock)))
            .thenReturn(true)

        // when
        accountService.close(account.id, account.userId)

        // then
        verify(accountRepository).close(account.id, account.userId, OffsetDateTime.now(clock))
        verify(accountRepository, never())
            .findByIdAndUserId(account.id, account.userId)
    }

    @Test
    fun `should not close account twice`() {
        // given
        val account = createAccount(closedAt = OffsetDateTime.now(clock))

        `when`(accountRepository.close(account.id, account.userId, OffsetDateTime.now(clock)))
            .thenReturn(false)
        `when`(accountRepository.findByIdAndUserId(account.id, account.userId))
            .thenReturn(account)

        // when
        accountService.close(account.id, account.userId)

        // then
        verify(accountRepository).close(account.id, account.userId, OffsetDateTime.now(clock))
    }

    @Test
    fun `should throw not found when closing missing account`() {
        // given
        val accountId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(accountRepository.close(accountId, userId, OffsetDateTime.now(clock)))
            .thenReturn(false)
        `when`(accountRepository.findByIdAndUserId(accountId, userId))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            accountService.close(accountId, userId)
        }.isInstanceOf(NotFoundException::class.java)
    }

    private fun createNewAccount(
        userId: UUID = UUID.randomUUID(),
    ): NewAccount =
        NewAccount(
            userId = userId,
            name = NAME,
            type = AccountType.CARD,
            openingBalance = OPENING_BALANCE,
            currency = Currency.EUR,
        )

    private fun createUpdateAccountData(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
    ): UpdateAccountData =
        UpdateAccountData(
            id = id,
            userId = userId,
            name = UPDATED_NAME,
            type = AccountType.BANK,
            openingBalance = UPDATED_OPENING_BALANCE,
        )

    private fun createAccount(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        closedAt: OffsetDateTime? = null,
    ): Account =
        Account(
            id = id,
            userId = userId,
            name = NAME,
            type = AccountType.CARD,
            openingBalance = OPENING_BALANCE,
            currency = Currency.EUR,
            createdAt = OffsetDateTime.parse("2026-07-01T10:00:00Z"),
            closedAt = closedAt,
        )

    private fun captureAccount(captor: ArgumentCaptor<Account>): Account {
        captor.capture()
        return createAccount()
    }
}

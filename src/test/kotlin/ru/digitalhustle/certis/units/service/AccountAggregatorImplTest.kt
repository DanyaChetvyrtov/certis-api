package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.AccountInUseException
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.service.account.impl.AccountAggregatorImpl
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.RecurringTransactionTemplateService
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class AccountAggregatorImplTest {

    private val accountService = mock(AccountService::class.java)
    private val recurringTransactionTemplateService = mock(RecurringTransactionTemplateService::class.java)
    private val aggregator = AccountAggregatorImpl(accountService, recurringTransactionTemplateService)

    @Test
    fun `should close account without schedulable recurring transactions`() {
        // given
        val account = createAccount()

        `when`(accountService.getByIdForUpdate(account.id, account.userId)).thenReturn(account)
        `when`(recurringTransactionTemplateService.existsSchedulableByAccountId(account.id, account.userId))
            .thenReturn(false)

        // when
        aggregator.close(account.id, account.userId)

        // then
        verify(accountService).close(account.id, account.userId)
    }

    @Test
    fun `should reject closing account used by recurring transaction`() {
        // given
        val account = createAccount()

        `when`(accountService.getByIdForUpdate(account.id, account.userId)).thenReturn(account)
        `when`(recurringTransactionTemplateService.existsSchedulableByAccountId(account.id, account.userId))
            .thenReturn(true)

        // when, then
        assertThatThrownBy {
            aggregator.close(account.id, account.userId)
        }
            .isInstanceOf(AccountInUseException::class.java)
            .hasMessage(ErrorMessages.ACCOUNT_IN_USE)

        verify(accountService, never()).close(account.id, account.userId)
    }

    private fun createAccount(): Account =
        Account(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = "Main account",
            type = AccountType.CARD,
            openingBalance = BigDecimal.ZERO,
            currency = Currency.EUR,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            closedAt = null,
        )
}

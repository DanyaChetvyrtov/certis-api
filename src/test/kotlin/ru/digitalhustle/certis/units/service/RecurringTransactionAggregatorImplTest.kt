package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.InvalidRecurringTransactionException
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.provider.RecurringTransactionScheduleProvider
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.domain.RecurringTransactionTemplateService
import ru.digitalhustle.certis.service.transaction.impl.RecurringTransactionAggregatorImpl
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class RecurringTransactionAggregatorImplTest {

    private val templateService = mock(RecurringTransactionTemplateService::class.java)
    private val accountService = mock(AccountService::class.java)
    private val categoryService = mock(CategoryService::class.java)
    private val aggregator = RecurringTransactionAggregatorImpl(
        recurringTransactionTemplateService = templateService,
        accountService = accountService,
        categoryService = categoryService,
        scheduleProvider = RecurringTransactionScheduleProvider(),
    )

    @Test
    fun `should save recurring transaction after locking account`() {
        // given
        val newTemplate = createNewTemplate()
        val savedTemplate = createTemplate(newTemplate)

        `when`(accountService.getByIdForShare(newTemplate.accountId, newTemplate.userId))
            .thenReturn(createAccount(newTemplate))
        `when`(templateService.save(newTemplate)).thenReturn(savedTemplate)

        // when
        val result = aggregator.save(newTemplate)

        // then
        assertThat(result).isEqualTo(savedTemplate)
        verify(templateService).save(newTemplate)
    }

    @Test
    fun `should reject end date before start date`() {
        // given
        val newTemplate = createNewTemplate().copy(
            endDate = LocalDate.parse("2026-08-11"),
        )

        // when, then
        assertThatThrownBy {
            aggregator.save(newTemplate)
        }
            .isInstanceOf(InvalidRecurringTransactionException::class.java)
            .hasMessage(ErrorMessages.RECURRING_TRANSACTION_DATE_RANGE)

        verify(templateService, never()).save(newTemplate)
    }

    @Test
    fun `should reject update of completed recurring transaction`() {
        // given
        val currentTemplate = createTemplate(
            createNewTemplate(),
            status = RecurringTransactionTemplateStatus.COMPLETED,
        ).copy(nextRunDate = null)
        val updateData = createUpdateData(currentTemplate)

        `when`(templateService.getByIdForUpdate(currentTemplate.id, currentTemplate.userId))
            .thenReturn(currentTemplate)

        // when, then
        assertThatThrownBy {
            aggregator.update(updateData)
        }
            .isInstanceOf(InvalidRecurringTransactionException::class.java)
            .hasMessage(ErrorMessages.RECURRING_TRANSACTION_TERMINAL)
    }

    @Test
    fun `should recalculate next run after schedule update`() {
        // given
        val currentTemplate = createTemplate(createNewTemplate()).copy(
            lastRunDate = LocalDate.parse("2026-08-12"),
            nextRunDate = LocalDate.parse("2026-09-12"),
        )
        val updateData = createUpdateData(currentTemplate).copy(
            frequency = RecurringTransactionFrequency.WEEKLY,
            intervalCount = 2,
        )
        val expectedNextRunDate = LocalDate.parse("2026-08-26")

        `when`(templateService.getByIdForUpdate(currentTemplate.id, currentTemplate.userId))
            .thenReturn(currentTemplate)
        `when`(accountService.getByIdForShare(currentTemplate.accountId, currentTemplate.userId))
            .thenReturn(createAccount(createNewTemplate()))
        `when`(templateService.update(currentTemplate, updateData, expectedNextRunDate))
            .thenReturn(currentTemplate.copy(nextRunDate = expectedNextRunDate))

        // when
        val result = aggregator.update(updateData)

        // then
        assertThat(result.nextRunDate).isEqualTo(expectedNextRunDate)
    }

    private fun createNewTemplate(): NewRecurringTransactionTemplate =
        NewRecurringTransactionTemplate(
            userId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            categoryId = null,
            name = "Subscription",
            type = TransactionType.EXPENSE,
            amount = BigDecimal("12.99"),
            merchant = null,
            note = null,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
            startDate = LocalDate.parse("2026-08-12"),
            endDate = null,
        )

    private fun createTemplate(
        source: NewRecurringTransactionTemplate,
        status: RecurringTransactionTemplateStatus = RecurringTransactionTemplateStatus.ACTIVE,
    ): RecurringTransactionTemplate =
        RecurringTransactionTemplate(
            id = UUID.randomUUID(),
            userId = source.userId,
            accountId = source.accountId,
            categoryId = source.categoryId,
            name = source.name,
            type = source.type,
            amount = source.amount,
            merchant = source.merchant,
            note = source.note,
            status = status,
            frequency = source.frequency,
            intervalCount = source.intervalCount,
            startDate = source.startDate,
            endDate = source.endDate,
            lastRunDate = null,
            nextRunDate = source.startDate,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
        )

    private fun createUpdateData(template: RecurringTransactionTemplate): UpdateRecurringTransactionTemplateData =
        UpdateRecurringTransactionTemplateData(
            id = template.id,
            userId = template.userId,
            accountId = template.accountId,
            categoryId = template.categoryId,
            name = template.name,
            type = template.type,
            amount = template.amount,
            merchant = template.merchant,
            note = template.note,
            status = RecurringTransactionTemplateStatus.ACTIVE,
            frequency = template.frequency,
            intervalCount = template.intervalCount,
            startDate = template.startDate,
            endDate = template.endDate,
        )

    private fun createAccount(source: NewRecurringTransactionTemplate): Account =
        Account(
            id = source.accountId,
            userId = source.userId,
            name = "Main card",
            type = AccountType.CARD,
            openingBalance = BigDecimal.ZERO,
            currency = Currency.EUR,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            closedAt = null,
        )
}

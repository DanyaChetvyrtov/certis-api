package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.features.transaction.command.service.RecurringTransactionExecutionStateService
import ru.digitalhustle.certis.features.transaction.command.service.TransactionService
import ru.digitalhustle.certis.features.transaction.command.service.impl.RecurringTransactionExecutionServiceImpl
import ru.digitalhustle.certis.features.transaction.command.validator.AccountValidator
import ru.digitalhustle.certis.features.transaction.command.validator.CategoryValidator
import ru.digitalhustle.certis.features.transaction.command.validator.RecurringTransactionValidator
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.exceptions.RecurringTransactionExecutionException
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.scheduler.RecurringTransactionScheduleProvider
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class RecurringTransactionExecutionServiceTest {

    private val executionStateService = mock(RecurringTransactionExecutionStateService::class.java)
    private val transactionService = mock(TransactionService::class.java)
    private val accountValidator = mock(AccountValidator::class.java)
    private val categoryValidator = mock(CategoryValidator::class.java)
    private val recurringTransactionValidator = mock(RecurringTransactionValidator::class.java)
    private val executionService = RecurringTransactionExecutionServiceImpl(
        executionStateService = executionStateService,
        transactionService = transactionService,
        scheduleProvider = RecurringTransactionScheduleProvider(),
        accountValidator = accountValidator,
        categoryValidator = categoryValidator,
        recurringTransactionValidator = recurringTransactionValidator,
    )

    @Test
    fun `should create scheduled transaction and advance template`() {
        // given
        val template = createTemplate()
        val currentDate = template.startDate
        val nextRunDate = template.startDate.plusMonths(1)

        `when`(executionStateService.getByIdForUpdate(template.id)).thenReturn(template)

        // when
        executionService.execute(template.id, currentDate)

        // then
        verify(accountValidator).validateActiveAccount(template.accountId, template.userId)
        verify(categoryValidator).validateCategory(template.categoryId, template.userId, false)
        verify(transactionService).saveScheduled(template, template.startDate)
        verify(executionStateService).recordExecution(template, template.startDate, nextRunDate)
    }

    @Test
    fun `should claim and execute next due transaction`() {
        // given
        val template = createTemplate()
        val currentDate = template.startDate
        val currentTime = OffsetDateTime.parse("2026-08-12T18:00:00Z")
        `when`(executionStateService.findDueForUpdate(currentDate, currentTime, emptySet()))
            .thenReturn(template)

        // when
        val result = executionService.executeNext(currentDate, currentTime, emptySet())

        // then
        assertThat(result?.templateId).isEqualTo(template.id)
        assertThat(result?.scheduledFor).isEqualTo(template.startDate)
        verify(transactionService).saveScheduled(template, template.startDate)
    }

    @Test
    fun `should preserve occurrence context when claimed execution fails`() {
        // given
        val template = createTemplate()
        val currentDate = template.startDate
        val currentTime = OffsetDateTime.parse("2026-08-12T18:00:00Z")
        val failure = IllegalStateException("Persistence failure")
        `when`(executionStateService.findDueForUpdate(currentDate, currentTime, emptySet()))
            .thenReturn(template)
        `when`(transactionService.saveScheduled(template, template.startDate))
            .thenThrow(failure)

        // when, then
        assertThatThrownBy {
            executionService.executeNext(currentDate, currentTime, emptySet())
        }.isInstanceOfSatisfying(RecurringTransactionExecutionException::class.java) { exception ->
            assertThat(exception.templateId).isEqualTo(template.id)
            assertThat(exception.scheduledFor).isEqualTo(template.startDate)
            assertThat(exception.cause).isSameAs(failure)
        }
    }

    @Test
    fun `should complete template after final occurrence`() {
        // given
        val template = createTemplate(endDate = LocalDate.parse("2026-08-12"))

        `when`(executionStateService.getByIdForUpdate(template.id)).thenReturn(template)

        // when
        executionService.execute(template.id, template.startDate)

        // then
        verify(transactionService).saveScheduled(template, template.startDate)
        verify(executionStateService).recordExecution(template, template.startDate, null)
    }

    @Test
    fun `should ignore template that is no longer due`() {
        // given
        val template = createTemplate(nextRunDate = LocalDate.parse("2026-09-12"))
        val currentDate = LocalDate.parse("2026-08-12")
        val nextRunDate = checkNotNull(template.nextRunDate)

        `when`(executionStateService.getByIdForUpdate(template.id)).thenReturn(template)

        // when
        executionService.execute(template.id, currentDate)

        // then
        verify(transactionService, never()).saveScheduled(template, nextRunDate)
        verify(accountValidator, never()).validateActiveAccount(template.accountId, template.userId)
    }

    @Test
    fun `should ignore paused template`() {
        // given
        val template = createTemplate().copy(status = RecurringTransactionTemplateStatus.PAUSED)

        `when`(executionStateService.getByIdForUpdate(template.id)).thenReturn(template)

        // when
        executionService.execute(template.id, template.startDate)

        // then
        verify(transactionService, never()).saveScheduled(template, template.startDate)
        verify(accountValidator, never()).validateActiveAccount(template.accountId, template.userId)
    }

    @Test
    fun `should not advance template when transaction creation fails`() {
        // given
        val template = createTemplate()
        val failure = IllegalStateException("Persistence failure")

        `when`(executionStateService.getByIdForUpdate(template.id)).thenReturn(template)
        `when`(transactionService.saveScheduled(template, template.startDate))
            .thenThrow(failure)

        // when, then
        assertThatThrownBy {
            executionService.execute(template.id, template.startDate)
        }.isSameAs(failure)

        verify(executionStateService, never()).recordExecution(
            template,
            template.startDate,
            template.startDate.plusMonths(1),
        )
    }

    private fun createTemplate(
        endDate: LocalDate? = null,
        nextRunDate: LocalDate = LocalDate.parse("2026-08-12"),
    ): RecurringTransactionTemplate =
        RecurringTransactionTemplate(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            categoryId = null,
            name = "Subscription",
            type = TransactionType.EXPENSE,
            amount = BigDecimal("12.99"),
            merchant = "Streaming service",
            note = null,
            status = RecurringTransactionTemplateStatus.ACTIVE,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
            startDate = LocalDate.parse("2026-08-12"),
            endDate = endDate,
            lastRunDate = null,
            nextRunDate = nextRunDate,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
        )
}

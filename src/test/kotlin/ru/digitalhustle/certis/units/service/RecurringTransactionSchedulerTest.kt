package ru.digitalhustle.certis.units.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.config.properties.RecurringTransactionProperties
import ru.digitalhustle.certis.exception.custom.RecurringTransactionExecutionException
import ru.digitalhustle.certis.model.transaction.RecurringTransactionExecutionResult
import ru.digitalhustle.certis.model.transaction.RecurringTransactionRetryState
import ru.digitalhustle.certis.scheduler.RecurringTransactionScheduler
import ru.digitalhustle.certis.service.domain.RecurringTransactionExecutionStateService
import ru.digitalhustle.certis.service.transaction.RecurringTransactionExecutionService
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class RecurringTransactionSchedulerTest {

    private val executionStateService = mock(RecurringTransactionExecutionStateService::class.java)
    private val executionService = mock(RecurringTransactionExecutionService::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-12T18:00:00Z"), ZoneOffset.UTC)
    private val properties = RecurringTransactionProperties(
        scheduler = RecurringTransactionProperties.Scheduler(
            enabled = true,
            delay = Duration.ofMinutes(1),
            batchSize = 3,
            maxCatchUpPerTemplate = 2,
            retryInitialDelay = Duration.ofMinutes(1),
            retryMaxDelay = Duration.ofHours(1),
        ),
    )
    private val meterRegistry = SimpleMeterRegistry()
    private val scheduler = RecurringTransactionScheduler(
        executionStateService = executionStateService,
        executionService = executionService,
        properties = properties,
        clock = clock,
        meterRegistry = meterRegistry,
    )

    @Test
    fun `should continue batch after one template fails`() {
        // given
        val currentDate = LocalDate.now(clock)
        val failedTemplateId = UUID.randomUUID()
        val successfulTemplateId = UUID.randomUUID()
        val currentTime = OffsetDateTime.now(clock)
        val scheduledFor = currentDate.minusDays(1)
        val failure = RecurringTransactionExecutionException(
            templateId = failedTemplateId,
            scheduledFor = scheduledFor,
            cause = IllegalStateException("Persistence failure"),
        )

        `when`(executionService.executeNext(currentDate, currentTime, emptySet()))
            .thenThrow(failure)
        `when`(executionService.executeNext(currentDate, currentTime, setOf(failedTemplateId)))
            .thenReturn(RecurringTransactionExecutionResult(successfulTemplateId, currentDate))
            .thenReturn(null)
        `when`(executionStateService.recordExecutionFailure(failedTemplateId, scheduledFor))
            .thenReturn(RecurringTransactionRetryState(1, currentTime.plusMinutes(1)))

        // when
        scheduler.executeDueTransactions()

        // then
        verify(executionStateService).recordExecutionFailure(failedTemplateId, scheduledFor)
        verify(executionService).executeNext(currentDate, currentTime, emptySet())
        verify(executionService, org.mockito.Mockito.times(2))
            .executeNext(currentDate, currentTime, setOf(failedTemplateId))
        assertThat(
            meterRegistry.get("certis.recurring.transactions.executions")
                .tag("outcome", "success")
                .counter()
                .count(),
        ).isEqualTo(1.0)
        assertThat(
            meterRegistry.get("certis.recurring.transactions.executions")
                .tag("outcome", "failure")
                .counter()
                .count(),
        ).isEqualTo(1.0)
    }

    @Test
    fun `should limit catch-up occurrences per template in one batch`() {
        // given
        val currentDate = LocalDate.now(clock)
        val currentTime = OffsetDateTime.now(clock)
        val overdueTemplateId = UUID.randomUUID()
        val otherTemplateId = UUID.randomUUID()

        `when`(executionService.executeNext(currentDate, currentTime, emptySet()))
            .thenReturn(RecurringTransactionExecutionResult(overdueTemplateId, currentDate.minusDays(2)))
            .thenReturn(RecurringTransactionExecutionResult(overdueTemplateId, currentDate.minusDays(1)))
        `when`(executionService.executeNext(currentDate, currentTime, setOf(overdueTemplateId)))
            .thenReturn(RecurringTransactionExecutionResult(otherTemplateId, currentDate))

        // when
        scheduler.executeDueTransactions()

        // then
        verify(executionService, org.mockito.Mockito.times(2))
            .executeNext(currentDate, currentTime, emptySet())
        verify(executionService).executeNext(currentDate, currentTime, setOf(overdueTemplateId))
    }

    @Test
    fun `should stop batch when selecting next occurrence fails`() {
        // given
        val currentDate = LocalDate.now(clock)
        val currentTime = OffsetDateTime.now(clock)
        `when`(executionService.executeNext(currentDate, currentTime, emptySet()))
            .thenThrow(IllegalStateException("Selection failure"))

        // when
        scheduler.executeDueTransactions()

        // then
        verify(executionService).executeNext(currentDate, currentTime, emptySet())
        verifyNoInteractions(executionStateService)
        assertThat(
            meterRegistry.get("certis.recurring.transactions.executions")
                .tag("outcome", "success")
                .counter()
                .count(),
        ).isZero()
        assertThat(
            meterRegistry.get("certis.recurring.transactions.executions")
                .tag("outcome", "failure")
                .counter()
                .count(),
        ).isZero()
    }
}

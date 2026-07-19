package ru.digitalhustle.certis.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.config.properties.RecurringTransactionProperties
import ru.digitalhustle.certis.exception.custom.RecurringTransactionExecutionException
import ru.digitalhustle.certis.model.transaction.RecurringTransactionRetryState
import ru.digitalhustle.certis.service.domain.RecurringTransactionExecutionStateService
import ru.digitalhustle.certis.service.transaction.RecurringTransactionExecutionService
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Component
@ConditionalOnProperty(
    prefix = "digital-hustle.certis.recurring-transactions.scheduler",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class RecurringTransactionScheduler(
    private val executionStateService: RecurringTransactionExecutionStateService,
    private val executionService: RecurringTransactionExecutionService,
    private val properties: RecurringTransactionProperties,
    private val clock: Clock,
    private val meterRegistry: MeterRegistry,
) {

    private val successfulExecutions: Counter = meterRegistry.counter(
        EXECUTION_METRIC,
        OUTCOME_TAG,
        SUCCESS_OUTCOME,
    )
    private val failedExecutions: Counter = meterRegistry.counter(
        EXECUTION_METRIC,
        OUTCOME_TAG,
        FAILURE_OUTCOME,
    )
    private val batchTimer: Timer = meterRegistry.timer(BATCH_DURATION_METRIC)

    @Scheduled(fixedDelayString = $$"${digital-hustle.certis.recurring-transactions.scheduler.delay:10m}")
    fun executeDueTransactions() {
        val sample = Timer.start(meterRegistry)
        val batchResult = executeBatch()
        sample.stop(batchTimer)

        if (batchResult.attempted > 0) {
            log.info {
                "Recurring transaction scheduler processed ${batchResult.attempted} occurrences: " +
                    "${batchResult.succeeded} succeeded, ${batchResult.failed} failed"
            }
        }
    }

    private fun executeBatch(): BatchResult {
        val currentDate = LocalDate.now(clock)

        val excludedTemplateIds = mutableSetOf<UUID>()
        val executionsByTemplate = mutableMapOf<UUID, Int>()
        var succeeded = 0
        var failed = 0

        while (succeeded + failed < properties.scheduler.batchSize) {
            when (
                val attempt = executeNextOccurrence(
                    currentDate = currentDate,
                    excludedTemplateIds = excludedTemplateIds,
                )
            ) {
                is ExecutionAttempt.Succeeded -> {
                    succeeded++
                    successfulExecutions.increment()

                    val executionCount = executionsByTemplate.merge(attempt.templateId, 1, Int::plus) ?: 1

                    if (executionCount >= properties.scheduler.maxCatchUpPerTemplate) {
                        excludedTemplateIds += attempt.templateId
                    }
                }

                is ExecutionAttempt.Failed -> {
                    failed++
                    failedExecutions.increment()
                    excludedTemplateIds += attempt.exception.templateId

                    handleExecutionFailure(attempt.exception)
                }

                ExecutionAttempt.NoMoreOccurrences,
                is ExecutionAttempt.Aborted,
                -> {
                    if (attempt is ExecutionAttempt.Aborted) {
                        log.error(attempt.exception) {
                            "Failed to select the next recurring transaction occurrence"
                        }
                    }

                    return BatchResult(
                        attempted = succeeded + failed,
                        succeeded = succeeded,
                        failed = failed,
                    )
                }
            }
        }

        return BatchResult(
            attempted = succeeded + failed,
            succeeded = succeeded,
            failed = failed,
        )
    }

    private fun executeNextOccurrence(
        currentDate: LocalDate,
        excludedTemplateIds: Set<UUID>,
    ): ExecutionAttempt = try {
        executionService.executeNext(
            currentDate = currentDate,
            currentTime = OffsetDateTime.now(clock),
            excludedTemplateIds = excludedTemplateIds.toSet(),
        )?.let { result ->
            ExecutionAttempt.Succeeded(result.templateId)
        } ?: ExecutionAttempt.NoMoreOccurrences
    } catch (exception: RecurringTransactionExecutionException) {
        ExecutionAttempt.Failed(exception)
    } catch (exception: RuntimeException) {
        ExecutionAttempt.Aborted(exception)
    }

    private fun handleExecutionFailure(exception: RecurringTransactionExecutionException) {
        val retryState = try {
            executionStateService.recordExecutionFailure(
                id = exception.templateId,
                scheduledFor = exception.scheduledFor,
            )
        } catch (recordingException: RuntimeException) {
            log.error(recordingException) {
                "Failed to record retry state for recurring transaction template ${exception.templateId}"
            }
            null
        }

        logExecutionFailure(exception, retryState)
    }

    private fun logExecutionFailure(
        exception: RecurringTransactionExecutionException,
        retryState: RecurringTransactionRetryState?,
    ) {
        val retryMessage = retryState?.let {
            "; retry ${it.consecutiveFailures} scheduled after ${it.retryAfter}"
        }.orEmpty()

        log.error(exception.cause ?: exception) {
            "Failed to execute recurring transaction template ${exception.templateId}$retryMessage"
        }
    }

    private companion object {
        private val log = KotlinLogging.logger {}
        private const val EXECUTION_METRIC = "certis.recurring.transactions.executions"
        private const val BATCH_DURATION_METRIC = "certis.recurring.transactions.batch.duration"
        private const val OUTCOME_TAG = "outcome"
        private const val SUCCESS_OUTCOME = "success"
        private const val FAILURE_OUTCOME = "failure"
    }

    private data class BatchResult(
        val attempted: Int,
        val succeeded: Int,
        val failed: Int,
    )

    private sealed interface ExecutionAttempt {

        data class Succeeded(
            val templateId: UUID,
        ) : ExecutionAttempt

        data class Failed(
            val exception: RecurringTransactionExecutionException,
        ) : ExecutionAttempt

        data object NoMoreOccurrences : ExecutionAttempt

        data class Aborted(
            val exception: RuntimeException,
        ) : ExecutionAttempt
    }
}

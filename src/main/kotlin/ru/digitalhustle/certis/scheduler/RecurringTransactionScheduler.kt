package ru.digitalhustle.certis.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.config.properties.RecurringTransactionProperties
import ru.digitalhustle.certis.features.transaction.command.service.RecurringTransactionExecutionService
import ru.digitalhustle.certis.features.transaction.command.service.RecurringTransactionExecutionStateService
import ru.digitalhustle.certis.features.transaction.exceptions.RecurringTransactionExecutionException
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.LocalDate
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
    private val applicationClock: ApplicationClock,
    private val metrics: RecurringTransactionSchedulerMetrics,
) {

    @Scheduled(fixedDelayString = $$"${digital-hustle.certis.recurring-transactions.scheduler.delay:10m}")
    fun executeDueTransactions() {
        val batchResult = metrics.recordBatch {
            executeBatch()
        }

        if (batchResult.attempted > 0) {
            log.info {
                "Recurring transaction scheduler processed ${batchResult.attempted} occurrences: " +
                    "${batchResult.succeeded} succeeded, ${batchResult.failed} failed"
            }
        }
    }

    private fun executeBatch(): BatchResult {
        val currentDate = applicationClock.today()

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
                    metrics.recordSuccess()

                    val executionCount = executionsByTemplate.merge(attempt.templateId, 1, Int::plus) ?: 1

                    if (executionCount >= properties.scheduler.maxCatchUpPerTemplate) {
                        excludedTemplateIds += attempt.templateId
                    }
                }

                is ExecutionAttempt.Failed -> {
                    failed++
                    metrics.recordFailure()

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
            currentTime = applicationClock.now(),
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

        val retryMessage = retryState?.let {
            "; retry ${it.consecutiveFailures} scheduled after ${it.retryAfter}"
        }.orEmpty()

        log.error(exception.cause ?: exception) {
            "Failed to execute recurring transaction template ${exception.templateId}$retryMessage"
        }
    }

    private companion object {
        private val log = KotlinLogging.logger {}
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

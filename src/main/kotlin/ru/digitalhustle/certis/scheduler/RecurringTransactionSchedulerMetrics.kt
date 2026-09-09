package ru.digitalhustle.certis.scheduler

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class RecurringTransactionSchedulerMetrics(
    meterRegistry: MeterRegistry,
) {

    private val successfulExecutions = meterRegistry.counter(
        EXECUTION_METRIC,
        OUTCOME_TAG,
        SUCCESS_OUTCOME,
    )

    private val failedExecutions = meterRegistry.counter(
        EXECUTION_METRIC,
        OUTCOME_TAG,
        FAILURE_OUTCOME,
    )

    private val batchTimer = meterRegistry.timer(BATCH_DURATION_METRIC)

    fun recordSuccess() {
        successfulExecutions.increment()
    }

    fun recordFailure() {
        failedExecutions.increment()
    }

    fun <T> recordBatch(block: () -> T): T =
        batchTimer.recordCallable(block)!!

    private companion object {
        const val EXECUTION_METRIC = "certis.recurring.transactions.executions"
        const val BATCH_DURATION_METRIC = "certis.recurring.transactions.batch.duration"

        const val OUTCOME_TAG = "outcome"
        const val SUCCESS_OUTCOME = "success"
        const val FAILURE_OUTCOME = "failure"
    }
}

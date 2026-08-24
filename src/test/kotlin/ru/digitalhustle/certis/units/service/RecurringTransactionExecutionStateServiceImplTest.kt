package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.config.properties.RecurringTransactionProperties
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.repository.RecurringTransactionTemplateRepository
import ru.digitalhustle.certis.service.domain.impl.RecurringTransactionExecutionStateServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class RecurringTransactionExecutionStateServiceImplTest {

    private val repository = mock(RecurringTransactionTemplateRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-12T18:00:00Z"), ZoneOffset.UTC)
    private val properties = RecurringTransactionProperties(
        scheduler = RecurringTransactionProperties.Scheduler(
            enabled = true,
            delay = Duration.ofMinutes(1),
            batchSize = 100,
            maxCatchUpPerTemplate = 10,
            retryInitialDelay = Duration.ofMinutes(1),
            retryMaxDelay = Duration.ofHours(1),
        ),
    )
    private val service = RecurringTransactionExecutionStateServiceImpl(repository, clock, properties)

    @Test
    fun `should get recurring transaction template for update`() {
        // given
        val template = createTemplate()
        `when`(repository.findByIdForUpdate(template.id)).thenReturn(template)

        // when
        val result = service.getByIdForUpdate(template.id)

        // then
        assertThat(result).isEqualTo(template)
    }

    @Test
    fun `should reject missing recurring transaction template for update`() {
        // given
        val templateId = UUID.randomUUID()

        // when, then
        assertThatThrownBy {
            service.getByIdForUpdate(templateId)
        }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Recurring transaction not found")
    }

    @Test
    fun `should find next due recurring transaction template for update`() {
        // given
        val template = createTemplate()
        val currentDate = LocalDate.now(clock)
        val currentTime = OffsetDateTime.now(clock)
        val excludedTemplateIds = setOf(UUID.randomUUID())
        `when`(repository.findDueForUpdate(currentDate, currentTime, excludedTemplateIds))
            .thenReturn(template)

        // when
        val result = service.findDueForUpdate(currentDate, currentTime, excludedTemplateIds)

        // then
        assertThat(result).isEqualTo(template)
    }

    @Test
    fun `should complete template when execution has no next date`() {
        // given
        val template = createTemplate()
        val captor = ArgumentCaptor.forClass(RecurringTransactionTemplate::class.java)
        `when`(repository.update(captureTemplate(captor)))
            .thenAnswer { captor.value }

        // when
        val result = service.recordExecution(
            template = template,
            lastRunDate = template.startDate,
            nextRunDate = null,
        )

        // then
        assertAll(
            { assertThat(result.status).isEqualTo(RecurringTransactionTemplateStatus.COMPLETED) },
            { assertThat(result.lastRunDate).isEqualTo(template.startDate) },
            { assertThat(result.nextRunDate).isNull() },
            { assertThat(result.updatedAt).isEqualTo(OffsetDateTime.now(clock)) },
        )
    }

    @Test
    fun `should exponentially delay retry after execution failure`() {
        // given
        val template = createTemplate()
        val scheduledFor = checkNotNull(template.nextRunDate)
        `when`(repository.findFailureCountForUpdate(template.id, scheduledFor)).thenReturn(2)
        `when`(
            repository.recordExecutionFailure(
                id = template.id,
                scheduledFor = scheduledFor,
                consecutiveFailures = 3,
                retryAfter = OffsetDateTime.now(clock).plusMinutes(4),
            ),
        ).thenReturn(true)

        // when
        val result = service.recordExecutionFailure(template.id, scheduledFor)

        // then
        assertThat(result?.consecutiveFailures).isEqualTo(3)
        assertThat(result?.retryAfter).isEqualTo(OffsetDateTime.now(clock).plusMinutes(4))
    }

    private fun createTemplate(): RecurringTransactionTemplate =
        RecurringTransactionTemplate(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            categoryId = UUID.randomUUID(),
            name = "Rent",
            type = TransactionType.EXPENSE,
            amount = BigDecimal("900.00"),
            merchant = "Landlord",
            note = "Monthly rent",
            status = RecurringTransactionTemplateStatus.ACTIVE,
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
            startDate = LocalDate.parse("2026-09-01"),
            endDate = null,
            lastRunDate = null,
            nextRunDate = LocalDate.parse("2026-09-01"),
            createdAt = OffsetDateTime.parse("2026-08-12T17:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-12T17:00:00Z"),
        )

    private fun captureTemplate(
        captor: ArgumentCaptor<RecurringTransactionTemplate>,
    ): RecurringTransactionTemplate {
        captor.capture()
        return createTemplate()
    }
}

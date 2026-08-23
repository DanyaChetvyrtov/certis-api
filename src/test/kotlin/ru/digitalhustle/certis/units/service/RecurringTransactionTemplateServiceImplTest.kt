package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.repository.RecurringTransactionTemplateRepository
import ru.digitalhustle.certis.service.domain.impl.RecurringTransactionTemplateServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class RecurringTransactionTemplateServiceImplTest {

    private val repository = mock(RecurringTransactionTemplateRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-12T18:00:00Z"), ZoneOffset.UTC)
    private val service = RecurringTransactionTemplateServiceImpl(repository, clock)

    @Test
    fun `should save active recurring transaction starting on requested date`() {
        // given
        val newTemplate = createNewTemplate()
        val captor = ArgumentCaptor.forClass(RecurringTransactionTemplate::class.java)

        `when`(repository.insert(captureTemplate(captor)))
            .thenAnswer { captor.value }

        // when
        val result = service.save(newTemplate)

        // then
        assertAll(
            { assertThat(result.status).isEqualTo(RecurringTransactionTemplateStatus.ACTIVE) },
            { assertThat(result.nextRunDate).isEqualTo(newTemplate.startDate) },
            { assertThat(result.lastRunDate).isNull() },
            { assertThat(result.name).isEqualTo("Rent") },
            { assertThat(result.merchant).isEqualTo("Landlord") },
            { assertThat(result.createdAt).isEqualTo(OffsetDateTime.now(clock)) },
            { assertThat(result.updatedAt).isEqualTo(OffsetDateTime.now(clock)) },
        )
    }

    @Test
    fun `should cancel template without deleting history`() {
        // given
        val template = createTemplate()
        val captor = ArgumentCaptor.forClass(RecurringTransactionTemplate::class.java)

        `when`(repository.update(captureTemplate(captor)))
            .thenAnswer { captor.value }

        // when
        val result = service.cancel(template)

        // then
        assertThat(result.status).isEqualTo(RecurringTransactionTemplateStatus.CANCELLED)
        assertThat(result.nextRunDate).isNull()
    }

    private fun createNewTemplate(): NewRecurringTransactionTemplate =
        NewRecurringTransactionTemplate(
            userId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            categoryId = UUID.randomUUID(),
            name = " Rent ",
            type = TransactionType.EXPENSE,
            amount = BigDecimal("900.00"),
            merchant = " Landlord ",
            note = "Monthly rent",
            frequency = RecurringTransactionFrequency.MONTHLY,
            intervalCount = 1,
            startDate = LocalDate.parse("2026-09-01"),
            endDate = null,
        )

    private fun createTemplate(): RecurringTransactionTemplate {
        val newTemplate = createNewTemplate()
        val now = OffsetDateTime.parse("2026-08-12T17:00:00Z")

        return RecurringTransactionTemplate(
            id = UUID.randomUUID(),
            userId = newTemplate.userId,
            accountId = newTemplate.accountId,
            categoryId = newTemplate.categoryId,
            name = newTemplate.name.trim(),
            type = newTemplate.type,
            amount = newTemplate.amount,
            merchant = newTemplate.merchant?.trim(),
            note = newTemplate.note,
            status = RecurringTransactionTemplateStatus.ACTIVE,
            frequency = newTemplate.frequency,
            intervalCount = newTemplate.intervalCount,
            startDate = newTemplate.startDate,
            endDate = newTemplate.endDate,
            lastRunDate = null,
            nextRunDate = newTemplate.startDate,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun captureTemplate(
        captor: ArgumentCaptor<RecurringTransactionTemplate>,
    ): RecurringTransactionTemplate {
        captor.capture()
        return createTemplate()
    }
}

package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.repository.RecurringTransactionTemplateRepository
import ru.digitalhustle.certis.service.domain.RecurringTransactionTemplateService
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RecurringTransactionTemplateServiceImpl(
    private val recurringTransactionTemplateRepository: RecurringTransactionTemplateRepository,
    private val clock: Clock,
) : RecurringTransactionTemplateService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity(ENTITY_NAME)

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw NotFoundException.entity(ENTITY_NAME)

    override fun getAllByUserId(userId: UUID): List<RecurringTransactionTemplate> =
        recurringTransactionTemplateRepository.findAllByUserId(userId)

    override fun existsSchedulableByAccountId(
        accountId: UUID,
        userId: UUID,
    ): Boolean = recurringTransactionTemplateRepository.existsSchedulableByAccountIdAndUserId(accountId, userId)

    override fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate {
        val now = OffsetDateTime.now(clock)

        return recurringTransactionTemplateRepository.insert(
            RecurringTransactionTemplate(
                id = UUID.randomUUID(),
                userId = template.userId,
                accountId = template.accountId,
                categoryId = template.categoryId,
                name = template.name.trim(),
                type = template.type,
                amount = template.amount,
                merchant = template.merchant?.trim(),
                note = template.note,
                status = RecurringTransactionTemplateStatus.ACTIVE,
                frequency = template.frequency,
                intervalCount = template.intervalCount,
                startDate = template.startDate,
                endDate = template.endDate,
                lastRunDate = null,
                nextRunDate = template.startDate,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override fun update(
        currentTemplate: RecurringTransactionTemplate,
        updateData: UpdateRecurringTransactionTemplateData,
        nextRunDate: LocalDate?,
    ): RecurringTransactionTemplate {
        val status = if (nextRunDate == null) {
            RecurringTransactionTemplateStatus.COMPLETED
        } else {
            updateData.status
        }

        return saveUpdated(
            currentTemplate.copy(
                accountId = updateData.accountId,
                categoryId = updateData.categoryId,
                name = updateData.name.trim(),
                type = updateData.type,
                amount = updateData.amount,
                merchant = updateData.merchant?.trim(),
                note = updateData.note,
                status = status,
                frequency = updateData.frequency,
                intervalCount = updateData.intervalCount,
                startDate = updateData.startDate,
                endDate = updateData.endDate,
                nextRunDate = nextRunDate,
                updatedAt = OffsetDateTime.now(clock),
            ),
        )
    }

    override fun cancel(template: RecurringTransactionTemplate): RecurringTransactionTemplate =
        saveUpdated(
            template.copy(
                status = RecurringTransactionTemplateStatus.CANCELLED,
                nextRunDate = null,
                updatedAt = OffsetDateTime.now(clock),
            ),
        )

    private fun saveUpdated(template: RecurringTransactionTemplate): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.update(template)
            ?: throw NotFoundException.entity(ENTITY_NAME)

    private companion object {
        private const val ENTITY_NAME = "Recurring transaction"
    }
}

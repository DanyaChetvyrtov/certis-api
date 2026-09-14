package ru.digitalhustle.certis.features.transaction.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.command.model.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.command.model.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.features.transaction.command.repository.RecurringTransactionTemplateRepository
import ru.digitalhustle.certis.features.transaction.command.service.RecurringTransactionTemplateService
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.LocalDate
import java.util.UUID

@Service
class RecurringTransactionTemplateServiceImpl(
    private val recurringTransactionTemplateRepository: RecurringTransactionTemplateRepository,
    private val applicationClock: ApplicationClock,
) : RecurringTransactionTemplateService {

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw NotFoundException.entity(ENTITY_NAME)

    override fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate {
        val now = applicationClock.now()

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
                updatedAt = applicationClock.now(),
            ),
        )
    }

    override fun cancel(template: RecurringTransactionTemplate): RecurringTransactionTemplate =
        saveUpdated(
            template.copy(
                status = RecurringTransactionTemplateStatus.CANCELLED,
                nextRunDate = null,
                updatedAt = applicationClock.now(),
            ),
        )

    private fun saveUpdated(template: RecurringTransactionTemplate): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.update(template)
            ?: throw NotFoundException.entity(ENTITY_NAME)

    private companion object {
        private const val ENTITY_NAME = "Recurring transaction"
    }
}

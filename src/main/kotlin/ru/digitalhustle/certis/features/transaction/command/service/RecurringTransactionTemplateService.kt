package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.command.model.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import java.time.LocalDate
import java.util.UUID

interface RecurringTransactionTemplateService {

    fun getByIdForUpdate(id: UUID, userId: UUID): RecurringTransactionTemplate

    fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate

    fun update(
        currentTemplate: RecurringTransactionTemplate,
        updateData: UpdateRecurringTransactionTemplateData,
        nextRunDate: LocalDate?,
    ): RecurringTransactionTemplate

    fun cancel(template: RecurringTransactionTemplate): RecurringTransactionTemplate
}

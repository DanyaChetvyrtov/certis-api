package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.command.model.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import java.util.UUID

interface RecurringTransactionCommandService {

    fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate

    fun update(template: UpdateRecurringTransactionTemplateData): RecurringTransactionTemplate

    fun cancel(id: UUID, userId: UUID)
}

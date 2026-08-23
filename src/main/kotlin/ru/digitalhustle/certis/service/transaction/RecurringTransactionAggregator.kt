package ru.digitalhustle.certis.service.transaction

import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.UpdateRecurringTransactionTemplateData
import java.util.UUID

interface RecurringTransactionAggregator {

    fun getById(id: UUID, userId: UUID): RecurringTransactionTemplate

    fun getAllByUserId(userId: UUID): List<RecurringTransactionTemplate>

    fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate

    fun update(template: UpdateRecurringTransactionTemplateData): RecurringTransactionTemplate

    fun cancel(id: UUID, userId: UUID)
}

package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.UpdateRecurringTransactionTemplateData
import java.time.LocalDate
import java.util.UUID

interface RecurringTransactionTemplateService {

    fun getById(id: UUID, userId: UUID): RecurringTransactionTemplate

    fun getByIdForUpdate(id: UUID, userId: UUID): RecurringTransactionTemplate

    fun getAllByUserId(userId: UUID): List<RecurringTransactionTemplate>

    fun existsSchedulableByAccountId(accountId: UUID, userId: UUID): Boolean

    fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate

    fun update(
        currentTemplate: RecurringTransactionTemplate,
        updateData: UpdateRecurringTransactionTemplateData,
        nextRunDate: LocalDate?,
    ): RecurringTransactionTemplate

    fun cancel(template: RecurringTransactionTemplate): RecurringTransactionTemplate
}

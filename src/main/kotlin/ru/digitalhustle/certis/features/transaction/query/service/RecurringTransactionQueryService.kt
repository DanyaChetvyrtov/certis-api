package ru.digitalhustle.certis.features.transaction.query.service

import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import java.util.UUID

interface RecurringTransactionQueryService {

    fun getById(id: UUID, userId: UUID): RecurringTransactionTemplate

    fun getAllByUserId(userId: UUID): List<RecurringTransactionTemplate>
}

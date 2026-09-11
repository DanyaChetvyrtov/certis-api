package ru.digitalhustle.certis.features.transaction.api

import java.util.UUID

interface RecurringTransactionUsage {

    fun existsSchedulableByAccountId(accountId: UUID, userId: UUID): Boolean
}

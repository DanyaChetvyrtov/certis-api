package ru.digitalhustle.certis.features.account.spi

import java.util.UUID

fun interface RecurringTransactionAccountUsage {

    fun existsSchedulableByAccountId(accountId: UUID, userId: UUID): Boolean
}

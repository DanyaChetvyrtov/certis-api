package ru.digitalhustle.certis.service.account

import ru.digitalhustle.certis.model.account.AccountPreview
import ru.digitalhustle.certis.model.account.NewAccount
import ru.digitalhustle.certis.model.account.UpdateAccountData
import java.util.UUID

interface AccountAggregator {

    fun getById(id: UUID, userId: UUID): AccountPreview

    fun getAllByUserId(userId: UUID): List<AccountPreview>

    fun save(account: NewAccount): AccountPreview

    fun update(account: UpdateAccountData): AccountPreview

    fun close(id: UUID, userId: UUID)
}

package ru.digitalhustle.certis.features.account.command.service

import ru.digitalhustle.certis.features.account.command.model.NewAccount
import ru.digitalhustle.certis.features.account.command.model.UpdateAccountData
import ru.digitalhustle.certis.features.account.model.Account
import ru.digitalhustle.certis.features.account.model.AccountPreview
import java.util.UUID

interface AccountService {

    fun getByIdForShare(id: UUID, userId: UUID): Account

    fun getByIdForUpdate(id: UUID, userId: UUID): Account

    fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<Account>

    fun save(newAccount: NewAccount): AccountPreview

    fun update(account: UpdateAccountData): Account

    fun close(id: UUID, userId: UUID)
}

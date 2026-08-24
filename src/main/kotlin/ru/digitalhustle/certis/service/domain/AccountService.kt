package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.account.AccountPreview
import ru.digitalhustle.certis.model.account.NewAccount
import ru.digitalhustle.certis.model.account.UpdateAccountData
import ru.digitalhustle.certis.model.entity.Account
import java.util.UUID

interface AccountService {

    fun getById(id: UUID, userId: UUID): AccountPreview

    fun getByIdForShare(id: UUID, userId: UUID): Account

    fun getByIdForUpdate(id: UUID, userId: UUID): Account

    fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<Account>

    fun getAllByUserId(userId: UUID): List<AccountPreview>

    fun save(newAccount: NewAccount): AccountPreview

    fun update(account: UpdateAccountData): AccountPreview

    fun close(id: UUID, userId: UUID)
}

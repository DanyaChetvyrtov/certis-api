package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.AccountPreview
import ru.digitalhustle.certis.model.NewAccount
import ru.digitalhustle.certis.model.UpdateAccountData
import ru.digitalhustle.certis.model.entity.Account
import java.util.UUID

interface AccountService {

    fun getById(id: UUID, userId: UUID): AccountPreview

    fun getByIdForShare(id: UUID, userId: UUID): Account

    fun getAllByUserId(userId: UUID): List<AccountPreview>

    fun save(newAccount: NewAccount): AccountPreview

    fun update(account: UpdateAccountData): AccountPreview

    fun close(id: UUID, userId: UUID)
}

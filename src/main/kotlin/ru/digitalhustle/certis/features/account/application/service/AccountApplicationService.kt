package ru.digitalhustle.certis.features.account.application.service

import ru.digitalhustle.certis.features.account.command.model.NewAccount
import ru.digitalhustle.certis.features.account.command.model.UpdateAccountData
import ru.digitalhustle.certis.features.account.model.AccountPreview
import java.util.UUID

interface AccountApplicationService {

    fun save(account: NewAccount): AccountPreview

    fun update(account: UpdateAccountData): AccountPreview

    fun close(id: UUID, userId: UUID)
}

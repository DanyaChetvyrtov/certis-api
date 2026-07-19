package ru.digitalhustle.certis.features.account.query.service

import ru.digitalhustle.certis.features.account.model.AccountPreview
import java.util.UUID

interface AccountQueryService {

    fun getById(id: UUID, userId: UUID): AccountPreview

    fun getAllByUserId(userId: UUID): List<AccountPreview>
}

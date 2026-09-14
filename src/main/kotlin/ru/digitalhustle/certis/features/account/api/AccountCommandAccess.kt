package ru.digitalhustle.certis.features.account.api

import java.util.UUID

interface AccountCommandAccess {

    fun getByIdForShare(id: UUID, userId: UUID): AccountSnapshot

    fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<AccountSnapshot>
}

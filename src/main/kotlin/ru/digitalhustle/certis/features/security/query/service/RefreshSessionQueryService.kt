package ru.digitalhustle.certis.features.security.query.service

import ru.digitalhustle.certis.features.security.model.RefreshSession
import java.util.UUID

interface RefreshSessionQueryService {

    fun getActiveByUserId(userId: UUID): List<RefreshSession>
}

package ru.digitalhustle.certis.features.security.query.service

import ru.digitalhustle.certis.features.security.model.RefreshSession
import java.util.UUID

interface AuthQueryService {

    fun getSessions(userId: UUID): List<RefreshSession>
}

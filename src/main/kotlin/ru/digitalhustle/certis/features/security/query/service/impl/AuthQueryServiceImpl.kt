package ru.digitalhustle.certis.features.security.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.security.model.RefreshSession
import ru.digitalhustle.certis.features.security.query.service.AuthQueryService
import ru.digitalhustle.certis.features.security.query.service.RefreshSessionQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AuthQueryServiceImpl(
    private val refreshSessionService: RefreshSessionQueryService,
) : AuthQueryService {

    override fun getSessions(userId: UUID): List<RefreshSession> =
        refreshSessionService.getActiveByUserId(userId)
}

package ru.digitalhustle.certis.features.security.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.security.model.RefreshSession
import ru.digitalhustle.certis.features.security.query.repository.RefreshSessionQueryRepository
import ru.digitalhustle.certis.features.security.query.service.RefreshSessionQueryService
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RefreshSessionQueryServiceImpl(
    private val refreshSessionRepository: RefreshSessionQueryRepository,
    private val applicationClock: ApplicationClock,
) : RefreshSessionQueryService {

    override fun getActiveByUserId(userId: UUID): List<RefreshSession> =
        refreshSessionRepository.findActiveByUserId(userId, applicationClock.now())
}

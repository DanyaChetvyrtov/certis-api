package ru.digitalhustle.certis.features.profile.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.profile.model.Profile
import ru.digitalhustle.certis.features.profile.query.repository.ProfileQueryRepository
import ru.digitalhustle.certis.features.profile.query.service.ProfileRecordQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProfileRecordQueryServiceImpl(
    private val profileRepository: ProfileQueryRepository,
) : ProfileRecordQueryService {

    override fun getById(id: UUID): Profile =
        profileRepository.findById(id)
            ?: throw NotFoundException.entity("Profile")
}

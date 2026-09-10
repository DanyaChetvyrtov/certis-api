package ru.digitalhustle.certis.features.profile.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.query.repository.ProfilePhotoMetaQueryRepository
import ru.digitalhustle.certis.features.profile.query.service.ProfilePhotoMetaQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProfilePhotoMetaQueryServiceImpl(
    private val profilePhotoMetaRepository: ProfilePhotoMetaQueryRepository,
) : ProfilePhotoMetaQueryService {

    override fun getById(id: UUID): ProfilePhotoMeta =
        profilePhotoMetaRepository.findById(id)
            ?: throw NotFoundException.entity("PhotoMeta")

    override fun getByProfileId(profileId: UUID): ProfilePhotoMeta? =
        profilePhotoMetaRepository.findByProfileId(profileId)
}

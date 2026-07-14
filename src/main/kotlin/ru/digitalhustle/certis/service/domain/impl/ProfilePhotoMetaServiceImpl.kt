package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.NewProfilePhotoMeta
import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
import ru.digitalhustle.certis.repository.ProfilePhotoMetaRepository
import ru.digitalhustle.certis.service.domain.ProfilePhotoMetaService
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ProfilePhotoMetaServiceImpl(
    private val profilePhotoMetaRepository: ProfilePhotoMetaRepository,
) : ProfilePhotoMetaService {

    override fun getById(id: UUID): ProfilePhotoMeta =
        profilePhotoMetaRepository.findById(id)
            ?: throw NotFoundException.entity("PhotoMeta")

    override fun getByProfileId(profileId: UUID): ProfilePhotoMeta? =
        profilePhotoMetaRepository.findByProfileId(profileId)

    override fun save(profilePhotoMeta: NewProfilePhotoMeta): ProfilePhotoMeta =
        profilePhotoMetaRepository.save(
            ProfilePhotoMeta(
                id = profilePhotoMeta.id,
                profileId = profilePhotoMeta.profileId,
                originalFileName = profilePhotoMeta.originalFileName,
                extension = profilePhotoMeta.extension,
                fileSize = profilePhotoMeta.fileSize,
                width = profilePhotoMeta.width,
                height = profilePhotoMeta.height,
                contentType = profilePhotoMeta.contentType,
                url = profilePhotoMeta.url,
                uploadedAt = OffsetDateTime.now(),
            ),
        )

    override fun deleteByProfileId(profileId: UUID): Unit =
        profilePhotoMetaRepository.deleteByProfileId(profileId)
}

package ru.digitalhustle.certis.features.profile.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.features.profile.command.model.NewProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.command.repository.ProfilePhotoMetaRepository
import ru.digitalhustle.certis.features.profile.command.service.ProfilePhotoMetaService
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class ProfilePhotoMetaServiceImpl(
    private val profilePhotoMetaRepository: ProfilePhotoMetaRepository,
    private val applicationClock: ApplicationClock,
) : ProfilePhotoMetaService {

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
                uploadedAt = applicationClock.now(),
            ),
        )

    override fun deleteByProfileId(profileId: UUID): Unit =
        profilePhotoMetaRepository.deleteByProfileId(profileId)
}

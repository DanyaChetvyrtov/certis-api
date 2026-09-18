package ru.digitalhustle.certis.features.profile.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.profile.gateway.MinioGateway
import ru.digitalhustle.certis.features.profile.model.ProfilePreview
import ru.digitalhustle.certis.features.profile.model.objectName
import ru.digitalhustle.certis.features.profile.model.toPreview
import ru.digitalhustle.certis.features.profile.query.model.ProfilePhoto
import ru.digitalhustle.certis.features.profile.query.service.ProfilePhotoMetaQueryService
import ru.digitalhustle.certis.features.profile.query.service.ProfileQueryService
import ru.digitalhustle.certis.features.profile.query.service.ProfileRecordQueryService
import ru.digitalhustle.certis.features.profile.util.ProfilePhotoUrlProvider
import ru.digitalhustle.certis.features.security.api.UserPreferencesQuery
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProfileQueryServiceImpl(
    private val profileService: ProfileRecordQueryService,
    private val userPreferencesQuery: UserPreferencesQuery,
    private val profilePhotoMetaService: ProfilePhotoMetaQueryService,
    private val minioGateway: MinioGateway,
    private val profilePhotoUrlProvider: ProfilePhotoUrlProvider,
) : ProfileQueryService {

    override fun getProfilePreview(profileId: UUID): ProfilePreview {
        val profile = profileService.getById(profileId)
        val preferredCurrency = userPreferencesQuery.getPreferredCurrency(profileId)

        return profile.toPreview(
            photoUrl = getPhotoUrl(profileId),
            preferredCurrency = preferredCurrency,
        )
    }

    override fun getPhoto(profileId: UUID): ProfilePhoto {
        profileService.getById(profileId)

        val photoMeta = profilePhotoMetaService.getByProfileId(profileId)
            ?: throw NotFoundException.Companion.entity("ProfilePhoto")

        return ProfilePhoto(
            content = minioGateway.getPhoto(photoMeta.objectName),
            contentType = photoMeta.contentType,
        )
    }

    private fun getPhotoUrl(profileId: UUID): String? =
        profilePhotoMetaService.getByProfileId(profileId)
            ?.let { profilePhotoUrlProvider.get(profileId) }
}

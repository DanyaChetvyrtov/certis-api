package ru.digitalhustle.certis.api.controller.impl

import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.api.controller.ProfileController
import ru.digitalhustle.certis.api.dto.PhotoMetaInfoDto
import ru.digitalhustle.certis.api.dto.ProfileDto
import ru.digitalhustle.certis.api.dto.request.CreateProfileRq
import ru.digitalhustle.certis.api.dto.request.UpdateProfileRq
import ru.digitalhustle.certis.api.mapper.ProfileMapper
import ru.digitalhustle.certis.features.profile.application.service.ProfileManagementApplicationService
import ru.digitalhustle.certis.features.profile.application.service.ProfilePhotoApplicationService
import ru.digitalhustle.certis.features.profile.query.service.ProfileQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class ProfileControllerImpl(
    private val profileQueryService: ProfileQueryService,
    private val profileManagementApplicationService: ProfileManagementApplicationService,
    private val profilePhotoApplicationService: ProfilePhotoApplicationService,
    private val profileMapper: ProfileMapper,
) : ProfileController {

    override fun getUserProfile(jwtDetails: JwtDetails): ProfileDto {
        val profilePreview = profileQueryService.getProfilePreview(jwtDetails.id)

        return profileMapper.convert(profilePreview)
    }

    override fun getPhoto(profileId: UUID): ResponseEntity<ByteArray> {
        val photo = profileQueryService.getPhoto(profileId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .contentLength(photo.content.size.toLong())
            .cacheControl(CacheControl.noStore())
            .body(photo.content)
    }

    override fun createProfile(
        createProfileRq: CreateProfileRq,
        jwtDetails: JwtDetails,
    ): ProfileDto {
        val profile = profileMapper.convert(createProfileRq, jwtDetails.id)

        val savedProfile = profileManagementApplicationService.saveProfile(
            profile = profile,
            preferredCurrency = createProfileRq.preferredCurrency,
        )

        return profileMapper.convert(savedProfile)
    }

    override fun uploadPhoto(
        profileId: UUID,
        photo: MultipartFile,
    ): PhotoMetaInfoDto {
        val photoMeta = profilePhotoApplicationService.uploadPhoto(profileId, photo)

        return profileMapper.convert(photoMeta)
    }

    override fun updateProfile(
        profileId: UUID,
        updateProfileRq: UpdateProfileRq,
    ): ProfileDto {
        val profile = profileMapper.convert(updateProfileRq, profileId)

        val updatedProfile = profileManagementApplicationService.updateProfile(
            profile = profile,
            preferredCurrency = updateProfileRq.preferredCurrency,
        )

        return profileMapper.convert(updatedProfile)
    }

    override fun updatePhoto(
        profileId: UUID,
        photo: MultipartFile,
    ): PhotoMetaInfoDto {
        val photoMeta = profilePhotoApplicationService.updatePhoto(profileId, photo)

        return profileMapper.convert(photoMeta)
    }

    override fun deleteProfile(profileId: UUID): Unit =
        profileManagementApplicationService.deleteProfile(profileId)

    override fun deletePhoto(profileId: UUID): Unit =
        profilePhotoApplicationService.deletePhotoByProfileId(profileId)
}

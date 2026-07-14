package ru.digitalhustle.certis.controller.impl

import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.controller.ProfileController
import ru.digitalhustle.certis.dto.PhotoMetaInfoDto
import ru.digitalhustle.certis.dto.ProfileDto
import ru.digitalhustle.certis.dto.request.CreateProfileRq
import ru.digitalhustle.certis.dto.request.UpdateProfileRq
import ru.digitalhustle.certis.mapper.ProfileMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.security.OwnProfileOnly
import ru.digitalhustle.certis.service.aggregation.ProfileAggregator
import java.util.UUID

@RestController
class ProfileControllerImpl(
    private val profileAggregator: ProfileAggregator,
    private val profileMapper: ProfileMapper,
) : ProfileController {

    @OwnProfileOnly
    override fun getProfileById(profileId: UUID): ProfileDto {
        val profileWithPhoto = profileAggregator.getProfilePreview(profileId)

        return profileMapper.convert(profileWithPhoto)
    }

    override fun createProfile(
        createProfileRq: CreateProfileRq,
        jwtDetails: JwtDetails,
    ): ProfileDto {
        val profile = profileMapper.convert(createProfileRq, jwtDetails.id)

        return profileMapper.convert(profileAggregator.saveProfile(profile))
    }

    @OwnProfileOnly
    override fun getPhoto(profileId: UUID): ResponseEntity<ByteArray> {
        val photo = profileAggregator.getPhoto(profileId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.contentType))
            .contentLength(photo.content.size.toLong())
            .cacheControl(CacheControl.noStore())
            .body(photo.content)
    }

    @OwnProfileOnly
    override fun uploadPhoto(
        profileId: UUID,
        photo: MultipartFile,
    ): PhotoMetaInfoDto {
        val photoMeta = profileAggregator.uploadPhoto(profileId, photo)

        return profileMapper.convert(photoMeta)
    }

    @OwnProfileOnly
    override fun updateProfile(
        profileId: UUID,
        updateProfileRq: UpdateProfileRq,
    ): ProfileDto {
        val profile = profileMapper.convert(updateProfileRq, profileId)

        profileAggregator.updateProfile(profile)

        return profileMapper.convert(profileAggregator.getProfilePreview(profileId))
    }

    @OwnProfileOnly
    override fun updatePhoto(
        profileId: UUID,
        photo: MultipartFile,
    ): PhotoMetaInfoDto {
        val photoMeta = profileAggregator.updatePhoto(profileId, photo)

        return profileMapper.convert(photoMeta)
    }

    @OwnProfileOnly
    override fun deleteProfile(profileId: UUID): Unit =
        profileAggregator.deleteProfile(profileId)

    @OwnProfileOnly
    override fun deletePhoto(profileId: UUID): Unit =
        profileAggregator.deletePhotoByProfileId(profileId)
}

package ru.digitalhustle.certis.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.PhotoMetaInfoDto
import ru.digitalhustle.certis.dto.ProfileDto
import ru.digitalhustle.certis.dto.request.CreateProfileRq
import ru.digitalhustle.certis.dto.request.UpdateProfileRq
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.security.OwnProfileOnly
import java.util.UUID

@RequestMapping(PathConstants.PROFILES)
interface ProfileController {

    @OwnProfileOnly
    @GetMapping(PathConstants.PROFILE_ID)
    fun getProfileById(
        @PathVariable profileId: UUID,
    ): ProfileDto

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProfile(
        @RequestBody @Valid createProfileRq: CreateProfileRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): ProfileDto

    @OwnProfileOnly
    @GetMapping(PathConstants.PROFILE_PHOTO)
    fun getPhoto(
        @PathVariable profileId: UUID,
    ): ResponseEntity<ByteArray>

    @OwnProfileOnly
    @PostMapping(PathConstants.PROFILE_PHOTO, consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadPhoto(
        @PathVariable profileId: UUID,
        @RequestPart("photo") photo: MultipartFile,
    ): PhotoMetaInfoDto

    @OwnProfileOnly
    @PutMapping(PathConstants.PROFILE_ID)
    fun updateProfile(
        @PathVariable profileId: UUID,
        @RequestBody @Valid updateProfileRq: UpdateProfileRq,
    ): ProfileDto

    @OwnProfileOnly
    @PutMapping(PathConstants.PROFILE_PHOTO, consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updatePhoto(
        @PathVariable profileId: UUID,
        @RequestPart("photo") photo: MultipartFile,
    ): PhotoMetaInfoDto

    @OwnProfileOnly
    @DeleteMapping(PathConstants.PROFILE_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProfile(
        @PathVariable profileId: UUID,
    )

    @OwnProfileOnly
    @DeleteMapping(PathConstants.PROFILE_PHOTO)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePhoto(
        @PathVariable profileId: UUID,
    )
}

package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.dto.request.CreateProfileRq
import ru.digitalhustle.certis.dto.request.UpdateProfileRq
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.PhotoProcessingException
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.profile.objectName
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.imageio.ImageIO

class ProfileControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val NAME = "John"
        private const val SURNAME = "Doe"
        private const val UPDATED_NAME = "Jane"
        private const val UPDATED_SURNAME = "Smith"
        private const val PHOTO_PART = "photo"
        private const val PHOTO_FILE_NAME = "profile-photo.png"
        private const val UPDATED_PHOTO_FILE_NAME = "updated-profile-photo.png"
        private const val PHOTO_EXTENSION = "png"
        private const val IMAGE_WIDTH = 3
        private const val IMAGE_HEIGHT = 2
        private val DATE_OF_BIRTH: LocalDate = LocalDate.of(2000, 1, 1)
        private val UPDATED_DATE_OF_BIRTH: LocalDate = LocalDate.of(2001, 2, 2)
        private val PREFERRED_CURRENCY = Currency.EUR
        private val UPDATED_PREFERRED_CURRENCY = Currency.RUB
    }

    @Test
    fun `should create profile`() {
        // given
        val user = userFixture.createInDb()
        val request = createProfileRequest()

        // when
        mvc.perform(
            post(PathConstants.PROFILES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(user.id.toString()))
            .andExpect(jsonPath("$.name").value(NAME))
            .andExpect(jsonPath("$.surname").value(SURNAME))
            .andExpect(jsonPath("$.dateOfBirth").value(DATE_OF_BIRTH.toString()))
            .andExpect(jsonPath("$.preferredCurrency").value(PREFERRED_CURRENCY.name))
            .andExpect(jsonPath("$.photoUrl").doesNotExist())

        val profile = requireNotNull(profileRepository.findById(user.id))

        assertThat(profile.name).isEqualTo(NAME)
        assertThat(profile.surname).isEqualTo(SURNAME)
        assertThat(profile.dateOfBirth).isEqualTo(DATE_OF_BIRTH)
        assertThat(requireNotNull(userRepository.findById(user.id)).preferredCurrency)
            .isEqualTo(PREFERRED_CURRENCY)
    }

    @Test
    fun `should use default currency when profile creation omits it`() {
        // given
        val user = userFixture.createInDb()
        val request =
            """
            {
              "name": "$NAME",
              "surname": "$SURNAME",
              "dateOfBirth": "$DATE_OF_BIRTH"
            }
            """.trimIndent()

        // when
        mvc.perform(
            post(PathConstants.PROFILES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.preferredCurrency").value(Currency.USD.name))

        assertThat(requireNotNull(userRepository.findById(user.id)).preferredCurrency)
            .isEqualTo(Currency.USD)
    }

    @Test
    fun `should return 409 when profile already exists`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)

        // when
        mvc.perform(
            post(PathConstants.PROFILES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createProfileRequest())),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
            .andExpect(jsonPath("$.message").value("Profile with such id already exists"))
    }

    @Test
    fun `should get profile`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        uploadPhoto(user)

        // when
        mvc.perform(
            get("${PathConstants.PROFILES}/${PathConstants.MY_PROFILE}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id.toString()))
            .andExpect(jsonPath("$.name").value(NAME))
            .andExpect(jsonPath("$.surname").value(SURNAME))
            .andExpect(jsonPath("$.dateOfBirth").value(DATE_OF_BIRTH.toString()))
            .andExpect(jsonPath("$.preferredCurrency").value(PREFERRED_CURRENCY.name))
            .andExpect(
                jsonPath("$.photoUrl")
                    .value("http://localhost:8080${PathConstants.profilePhoto(user.id)}"),
            )
    }

    @Test
    fun `should update profile`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)

        val request = UpdateProfileRq(
            name = UPDATED_NAME,
            surname = UPDATED_SURNAME,
            dateOfBirth = UPDATED_DATE_OF_BIRTH,
            preferredCurrency = UPDATED_PREFERRED_CURRENCY,
        )

        // when
        mvc.perform(
            put("${PathConstants.PROFILES}/${user.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id.toString()))
            .andExpect(jsonPath("$.name").value(UPDATED_NAME))
            .andExpect(jsonPath("$.surname").value(UPDATED_SURNAME))
            .andExpect(jsonPath("$.dateOfBirth").value(UPDATED_DATE_OF_BIRTH.toString()))
            .andExpect(jsonPath("$.preferredCurrency").value(UPDATED_PREFERRED_CURRENCY.name))

        val profile = requireNotNull(profileRepository.findById(user.id))

        assertThat(profile.name).isEqualTo(UPDATED_NAME)
        assertThat(profile.surname).isEqualTo(UPDATED_SURNAME)
        assertThat(profile.dateOfBirth).isEqualTo(UPDATED_DATE_OF_BIRTH)
        assertThat(requireNotNull(userRepository.findById(user.id)).preferredCurrency)
            .isEqualTo(UPDATED_PREFERRED_CURRENCY)
    }

    @Test
    fun `should preserve preferred currency when update omits it`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)

        val request =
            """
            {
              "name": "$UPDATED_NAME",
              "surname": "$UPDATED_SURNAME",
              "dateOfBirth": "$UPDATED_DATE_OF_BIRTH"
            }
            """.trimIndent()

        // when
        mvc.perform(
            put("${PathConstants.PROFILES}/${user.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.preferredCurrency").value(PREFERRED_CURRENCY.name))

        assertThat(requireNotNull(userRepository.findById(user.id)).preferredCurrency)
            .isEqualTo(PREFERRED_CURRENCY)
    }

    @Test
    fun `should return 400 when updating profile with invalid data`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)

        val request = UpdateProfileRq(
            name = "",
            surname = SURNAME,
            dateOfBirth = LocalDate.now(),
            preferredCurrency = PREFERRED_CURRENCY,
        )

        // when
        mvc.perform(
            put("${PathConstants.PROFILES}/${user.id}")
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.message").value(ErrorMessages.VALIDATION_FAILED))
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.dateOfBirth").exists())
    }

    @Test
    fun `should upload profile photo`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        val photoBytes = createPngBytes()

        // when
        mvc.perform(
            multipart(PathConstants.profilePhoto(user.id))
                .file(createPhoto(photoBytes = photoBytes))
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.profileId").value(user.id.toString()))
            .andExpect(jsonPath("$.originalFileName").value("profile-photo"))
            .andExpect(jsonPath("$.extension").value(PHOTO_EXTENSION))
            .andExpect(jsonPath("$.fileSize").value(photoBytes.size))
            .andExpect(jsonPath("$.width").value(IMAGE_WIDTH))
            .andExpect(jsonPath("$.height").value(IMAGE_HEIGHT))
            .andExpect(jsonPath("$.contentType").value(MediaType.IMAGE_PNG_VALUE))
            .andExpect(
                jsonPath("$.url")
                    .value("http://localhost:8080${PathConstants.profilePhoto(user.id)}"),
            )

        val photoMeta = requireNotNull(
            profilePhotoMetaRepository.findByProfileId(user.id),
        )

        verify(minioGateway).savePhoto(
            eqString(photoMeta.objectName),
            anyMultipartFile(),
            eqString(MediaType.IMAGE_PNG_VALUE),
        )
    }

    @Test
    fun `should return 409 when profile photo already exists`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        uploadPhoto(user)

        // when
        mvc.perform(
            multipart(PathConstants.profilePhoto(user.id))
                .file(createPhoto())
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
            .andExpect(
                jsonPath("$.message")
                    .value("ProfilePhoto with such profileId already exists"),
            )
    }

    @Test
    fun `should get profile photo`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        val photoBytes = createPngBytes()
        uploadPhoto(user, photoBytes)

        val photoMeta = requireNotNull(
            profilePhotoMetaRepository.findByProfileId(user.id),
        )

        `when`(minioGateway.getPhoto(photoMeta.objectName))
            .thenReturn(photoBytes)

        // when
        mvc.perform(
            get(PathConstants.profilePhoto(user.id))
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes(photoBytes))

        verify(minioGateway)
            .getPhoto(photoMeta.objectName)
    }

    @Test
    fun `should return 415 when photo extension is unsupported`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        val photo = createPhoto(fileName = "profile-photo.gif")

        // when
        mvc.perform(
            multipart(PathConstants.profilePhoto(user.id))
                .file(photo)
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(
                jsonPath("$.status")
                    .value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()),
            )
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_FILE_EXTENSION))

        assertThat(profilePhotoMetaRepository.findByProfileId(user.id))
            .isNull()
    }

    @Test
    fun `should update profile photo`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        uploadPhoto(user)

        val oldPhotoMeta = requireNotNull(
            profilePhotoMetaRepository.findByProfileId(user.id),
        )
        clearInvocations(minioGateway)

        // when
        mvc.perform(
            multipart(PathConstants.profilePhoto(user.id))
                .file(createPhoto(fileName = UPDATED_PHOTO_FILE_NAME))
                .cookie(accessTokenCookie(user))
                .with { request ->
                    request.method = HttpMethod.PUT.name()
                    request
                },
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.profileId").value(user.id.toString()))
            .andExpect(jsonPath("$.originalFileName").value("updated-profile-photo"))

        val updatedPhotoMeta = requireNotNull(
            profilePhotoMetaRepository.findByProfileId(user.id),
        )

        assertThat(updatedPhotoMeta.id).isNotEqualTo(oldPhotoMeta.id)
        verify(minioGateway).deletePhoto(oldPhotoMeta.objectName)
        verify(minioGateway).savePhoto(
            eqString(updatedPhotoMeta.objectName),
            anyMultipartFile(),
            eqString(MediaType.IMAGE_PNG_VALUE),
        )
    }

    @Test
    fun `should delete profile photo`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        uploadPhoto(user)

        val photoMeta = requireNotNull(
            profilePhotoMetaRepository.findByProfileId(user.id),
        )
        clearInvocations(minioGateway)

        // when
        mvc.perform(
            delete(PathConstants.profilePhoto(user.id))
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(profilePhotoMetaRepository.findByProfileId(user.id))
            .isNull()

        verify(minioGateway)
            .deletePhoto(photoMeta.objectName)
    }

    @Test
    fun `should rollback photo metadata when storage is unavailable`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)

        doThrow(PhotoProcessingException(ErrorMessages.PHOTO_UPLOAD_FAILED))
            .`when`(minioGateway)
            .savePhoto(
                anyString(),
                anyMultipartFile(),
                eqString(MediaType.IMAGE_PNG_VALUE),
            )

        // when
        mvc.perform(
            multipart(PathConstants.profilePhoto(user.id))
                .file(createPhoto())
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isServiceUnavailable)
            .andExpect(
                jsonPath("$.status")
                    .value(HttpStatus.SERVICE_UNAVAILABLE.value()),
            )
            .andExpect(jsonPath("$.message").value(ErrorMessages.PHOTO_STORAGE_UNAVAILABLE))

        assertThat(profilePhotoMetaRepository.findByProfileId(user.id))
            .isNull()

        val objectNameCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(minioGateway).savePhoto(
            captureString(objectNameCaptor),
            anyMultipartFile(),
            eqString(MediaType.IMAGE_PNG_VALUE),
        )
        verify(minioGateway)
            .deletePhoto(objectNameCaptor.value)
    }

    @Test
    fun `should delete profile but keep user`() {
        // given
        val user = userFixture.createInDb()
        createProfile(user)
        uploadPhoto(user)

        val photoMeta = requireNotNull(
            profilePhotoMetaRepository.findByProfileId(user.id),
        )
        clearInvocations(minioGateway)

        // when
        mvc.perform(
            delete("${PathConstants.PROFILES}/${user.id}")
                .cookie(accessTokenCookie(user)),
        )
            // then
            .andExpect(status().isNoContent)

        assertThat(profileRepository.findById(user.id)).isNull()
        assertThat(profilePhotoMetaRepository.findByProfileId(user.id)).isNull()
        assertThat(userRepository.findById(user.id)).isNotNull()

        verify(minioGateway)
            .deletePhoto(photoMeta.objectName)
    }

    private fun createProfile(user: User) {
        mvc.perform(
            post(PathConstants.PROFILES)
                .cookie(accessTokenCookie(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(createProfileRequest())),
        )
            .andExpect(status().isCreated)
    }

    private fun uploadPhoto(
        user: User,
        photoBytes: ByteArray = createPngBytes(),
    ) {
        mvc.perform(
            multipart(PathConstants.profilePhoto(user.id))
                .file(createPhoto(photoBytes = photoBytes))
                .cookie(accessTokenCookie(user)),
        )
            .andExpect(status().isCreated)
    }

    private fun createProfileRequest(): CreateProfileRq =
        CreateProfileRq(
            name = NAME,
            surname = SURNAME,
            dateOfBirth = DATE_OF_BIRTH,
            preferredCurrency = PREFERRED_CURRENCY,
        )

    private fun accessTokenCookie(user: User): Cookie =
        Cookie(
            SecurityConstants.ACCESS_TOKEN_COOKIE,
            jwtTokenProvider.createAccessToken(user.id, user.email),
        )

    private fun createPhoto(
        fileName: String = PHOTO_FILE_NAME,
        photoBytes: ByteArray = createPngBytes(),
    ): MockMultipartFile =
        MockMultipartFile(
            PHOTO_PART,
            fileName,
            MediaType.IMAGE_PNG_VALUE,
            photoBytes,
        )

    private fun createPngBytes(): ByteArray {
        val image = BufferedImage(
            IMAGE_WIDTH,
            IMAGE_HEIGHT,
            BufferedImage.TYPE_INT_RGB,
        )
        val output = ByteArrayOutputStream()

        ImageIO.write(image, PHOTO_EXTENSION, output)

        return output.toByteArray()
    }
}

private fun anyMultipartFile(): MultipartFile {
    any(MultipartFile::class.java)
    return MockMultipartFile("ignored", byteArrayOf())
}

private fun eqString(value: String): String {
    eq(value)
    return value
}

private fun captureString(captor: ArgumentCaptor<String>): String {
    captor.capture()
    return ""
}

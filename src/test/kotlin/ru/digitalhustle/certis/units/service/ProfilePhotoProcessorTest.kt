package ru.digitalhustle.certis.units.service

import org.apache.tika.Tika
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.config.properties.AppApiProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PhotoConstants
import ru.digitalhustle.certis.exception.custom.InvalidPhotoException
import ru.digitalhustle.certis.exception.custom.UnsupportedPhotoMediaTypeException
import ru.digitalhustle.certis.service.profile.ProfilePhotoProcessor
import ru.digitalhustle.certis.service.profile.ProfilePhotoUrlProvider
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

class ProfilePhotoProcessorTest {

    private val tika = mock(Tika::class.java)
    private val profilePhotoUrlProvider = ProfilePhotoUrlProvider(
        AppApiProperties(publicUrl = PUBLIC_API_URL),
    )

    private val profilePhotoProcessor = ProfilePhotoProcessor(
        profilePhotoUrlProvider = profilePhotoUrlProvider,
        tika = tika,
    )

    private companion object {
        private const val ORIGINAL_FILE_NAME = "profile-photo.png"
        private const val BASE_FILE_NAME = "profile-photo"
        private const val EXTENSION = "png"
        private const val WIDTH = 3
        private const val HEIGHT = 2
        private const val PUBLIC_API_URL = "https://api.certis.test"
    }

    @Test
    fun `should process profile photo`() {
        // given
        val profileId = UUID.randomUUID()
        val photoBytes = createPngBytes()
        val photo = createPhoto(photoBytes = photoBytes)

        `when`(tika.detect(photoBytes))
            .thenReturn(MediaType.IMAGE_PNG_VALUE)

        // when
        val processedPhoto = profilePhotoProcessor.process(profileId, photo)

        // then
        assertAll(
            { assertThat(processedPhoto.meta.profileId).isEqualTo(profileId) },
            { assertThat(processedPhoto.meta.originalFileName).isEqualTo(BASE_FILE_NAME) },
            { assertThat(processedPhoto.meta.extension).isEqualTo(EXTENSION) },
            { assertThat(processedPhoto.meta.fileSize).isEqualTo(photoBytes.size.toLong()) },
            { assertThat(processedPhoto.meta.width).isEqualTo(WIDTH) },
            { assertThat(processedPhoto.meta.height).isEqualTo(HEIGHT) },
            { assertThat(processedPhoto.meta.contentType).isEqualTo(MediaType.IMAGE_PNG_VALUE) },
            {
                assertThat(processedPhoto.meta.url)
                    .isEqualTo("$PUBLIC_API_URL/api/v1/profiles/$profileId/photo")
            },
            { assertThat(processedPhoto.objectName).isEqualTo("${processedPhoto.meta.id}.$EXTENSION") },
            { assertThat(processedPhoto.contentType).isEqualTo(MediaType.IMAGE_PNG_VALUE) },
        )
    }

    @Test
    fun `should sanitize original file name`() {
        // given
        val profileId = UUID.randomUUID()
        val photoBytes = createPngBytes()
        val photo = createPhoto(
            originalFileName = "../unsafe/profile-photo.png",
            photoBytes = photoBytes,
        )

        `when`(tika.detect(photoBytes))
            .thenReturn(MediaType.IMAGE_PNG_VALUE)

        // when
        val processedPhoto = profilePhotoProcessor.process(profileId, photo)

        // then
        assertThat(processedPhoto.meta.originalFileName).isEqualTo(BASE_FILE_NAME)
    }

    @Test
    fun `should throw invalid photo exception when file name is missing`() {
        // given
        val photo = createPhoto(originalFileName = null)

        // when, then
        assertThatThrownBy {
            profilePhotoProcessor.process(UUID.randomUUID(), photo)
        }.isInstanceOf(InvalidPhotoException::class.java)
            .hasMessage(ErrorMessages.INVALID_FILE_NAME)

        verifyNoInteractions(tika)
    }

    @Test
    fun `should throw invalid photo exception when photo is empty`() {
        // given
        val photo = createPhoto(photoBytes = byteArrayOf())

        // when, then
        assertThatThrownBy {
            profilePhotoProcessor.process(UUID.randomUUID(), photo)
        }.isInstanceOf(InvalidPhotoException::class.java)
            .hasMessage(ErrorMessages.EMPTY_PHOTO)

        verifyNoInteractions(tika)
    }

    @Test
    fun `should throw unsupported media type exception when extension is invalid`() {
        // given
        val photo = createPhoto(originalFileName = "profile-photo.gif")

        // when, then
        assertThatThrownBy {
            profilePhotoProcessor.process(UUID.randomUUID(), photo)
        }.isInstanceOf(UnsupportedPhotoMediaTypeException::class.java)
            .hasMessage(ErrorMessages.INVALID_FILE_EXTENSION)

        verifyNoInteractions(tika)
    }

    @Test
    fun `should throw unsupported media type exception when content type is invalid`() {
        // given
        val photoBytes = createPngBytes()
        val photo = createPhoto(photoBytes = photoBytes)

        `when`(tika.detect(photoBytes))
            .thenReturn(MediaType.TEXT_PLAIN_VALUE)

        // when, then
        assertThatThrownBy {
            profilePhotoProcessor.process(UUID.randomUUID(), photo)
        }.isInstanceOf(UnsupportedPhotoMediaTypeException::class.java)
            .hasMessage(ErrorMessages.INVALID_CONTENT_TYPE)
    }

    @Test
    fun `should throw unsupported media type exception when extension does not match content`() {
        // given
        val photoBytes = createPngBytes()
        val photo = createPhoto(
            originalFileName = "profile-photo.jpg",
            photoBytes = photoBytes,
        )

        `when`(tika.detect(photoBytes))
            .thenReturn(MediaType.IMAGE_PNG_VALUE)

        // when, then
        assertThatThrownBy {
            profilePhotoProcessor.process(UUID.randomUUID(), photo)
        }.isInstanceOf(UnsupportedPhotoMediaTypeException::class.java)
            .hasMessage(ErrorMessages.FILE_EXTENSION_CONTENT_TYPE_MISMATCH)
    }

    @Test
    fun `should throw invalid photo exception when image dimensions cannot be read`() {
        // given
        val photoBytes = "not an image".toByteArray()
        val photo = createPhoto(photoBytes = photoBytes)

        `when`(tika.detect(photoBytes))
            .thenReturn(MediaType.IMAGE_PNG_VALUE)

        // when, then
        assertThatThrownBy {
            profilePhotoProcessor.process(UUID.randomUUID(), photo)
        }.isInstanceOf(InvalidPhotoException::class.java)
            .hasMessage(ErrorMessages.INVALID_IMAGE_DIMENSIONS)
    }

    @Test
    fun `should throw invalid photo exception when photo is too large`() {
        // given
        val photo = createPhoto(
            size = PhotoConstants.MAX_FILE_SIZE_BYTES + 1,
        )

        // when, then
        assertThatThrownBy {
            profilePhotoProcessor.process(UUID.randomUUID(), photo)
        }.isInstanceOf(InvalidPhotoException::class.java)
            .hasMessage(ErrorMessages.PHOTO_TOO_LARGE)

        verifyNoInteractions(tika)
    }

    private fun createPhoto(
        originalFileName: String? = ORIGINAL_FILE_NAME,
        photoBytes: ByteArray = createPngBytes(),
        size: Long = photoBytes.size.toLong(),
    ): MultipartFile =
        mock(MultipartFile::class.java).also { photo ->
            `when`(photo.originalFilename)
                .thenReturn(originalFileName)

            `when`(photo.bytes)
                .thenReturn(photoBytes)

            `when`(photo.size)
                .thenReturn(size)

            `when`(photo.isEmpty)
                .thenReturn(size <= 0)
        }

    private fun createPngBytes(): ByteArray {
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val output = ByteArrayOutputStream()

        ImageIO.write(image, EXTENSION, output)

        return output.toByteArray()
    }
}

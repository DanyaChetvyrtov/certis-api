package ru.digitalhustle.certis.service.photo

import org.apache.commons.io.FilenameUtils
import org.apache.tika.Tika
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PhotoConstants
import ru.digitalhustle.certis.exception.custom.InvalidPhotoException
import ru.digitalhustle.certis.exception.custom.UnsupportedPhotoMediaTypeException
import ru.digitalhustle.certis.model.NewProfilePhotoMeta
import ru.digitalhustle.certis.model.ProcessedProfilePhoto
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

@Component
class ProfilePhotoProcessor(
    private val profilePhotoUrlProvider: ProfilePhotoUrlProvider,
    private val tika: Tika,
) {

    fun process(profileId: UUID, photo: MultipartFile): ProcessedProfilePhoto {
        photo.validateSize()

        val originalFileName = photo.cleanOriginalFileName()
        val baseFileName = originalFileName.validBaseName()
        val extension = originalFileName.validExtension()
        val photoBytes = photo.bytes.also { it.validateSize() }
        val contentType = photoBytes.detectContentType()
        validateExtensionMatchesContentType(extension, contentType)

        val dimensions = photoBytes.extractImageDimensions().validate()
        val id = UUID.randomUUID()
        val objectName = "$id.$extension"

        return ProcessedProfilePhoto(
            meta = NewProfilePhotoMeta(
                id = id,
                profileId = profileId,
                originalFileName = baseFileName,
                extension = extension,
                fileSize = photoBytes.size.toLong(),
                width = dimensions.width,
                height = dimensions.height,
                contentType = contentType,
                url = profilePhotoUrlProvider.get(profileId),
            ),
            objectName = objectName,
            contentType = contentType,
        )
    }

    private fun MultipartFile.validateSize() {
        if (isEmpty || size <= 0) {
            throw InvalidPhotoException(ErrorMessages.EMPTY_PHOTO)
        }
        if (size > PhotoConstants.MAX_FILE_SIZE_BYTES) {
            throw InvalidPhotoException(ErrorMessages.PHOTO_TOO_LARGE)
        }
    }

    private fun ByteArray.validateSize() {
        if (isEmpty()) {
            throw InvalidPhotoException(ErrorMessages.EMPTY_PHOTO)
        }
        if (size > PhotoConstants.MAX_FILE_SIZE_BYTES) {
            throw InvalidPhotoException(ErrorMessages.PHOTO_TOO_LARGE)
        }
    }

    private fun MultipartFile.cleanOriginalFileName(): String =
        originalFilename
            ?.takeIf(String::isNotBlank)
            ?.let(FilenameUtils::getName)
            ?.takeIf(String::isNotBlank)
            ?: throw InvalidPhotoException(ErrorMessages.INVALID_FILE_NAME)

    private fun String.validBaseName(): String =
        FilenameUtils.getBaseName(this)
            .takeIf(String::isNotBlank)
            ?.takeIf { it.length <= PhotoConstants.MAX_ORIGINAL_FILE_NAME_LENGTH }
            ?: throw InvalidPhotoException(ErrorMessages.INVALID_FILE_NAME)

    private fun String.validExtension(): String =
        FilenameUtils.getExtension(this)
            .lowercase()
            .takeIf(contentTypesByExtension::containsKey)
            ?: throw UnsupportedPhotoMediaTypeException(ErrorMessages.INVALID_FILE_EXTENSION)

    private fun ByteArray.detectContentType(): String =
        tika.detect(this)
            ?.lowercase()
            ?.takeIf(allowedContentTypes::contains)
            ?: throw UnsupportedPhotoMediaTypeException(ErrorMessages.INVALID_CONTENT_TYPE)

    private fun validateExtensionMatchesContentType(
        extension: String,
        contentType: String,
    ) {
        if (contentTypesByExtension[extension] != contentType) {
            throw UnsupportedPhotoMediaTypeException(
                ErrorMessages.FILE_EXTENSION_CONTENT_TYPE_MISMATCH,
            )
        }
    }

    private fun ByteArray.extractImageDimensions(): ImageDimensions {
        val input = ImageIO.createImageInputStream(ByteArrayInputStream(this))
            ?: throw InvalidPhotoException(ErrorMessages.INVALID_IMAGE_DIMENSIONS)

        return input.use(::readImageDimensions)
    }

    private fun readImageDimensions(input: ImageInputStream): ImageDimensions {
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) {
            throw InvalidPhotoException(ErrorMessages.INVALID_IMAGE_DIMENSIONS)
        }

        val reader = readers.next()
        try {
            reader.input = input

            return ImageDimensions(
                width = reader.getWidth(FIRST_IMAGE_INDEX),
                height = reader.getHeight(FIRST_IMAGE_INDEX),
            )
        } finally {
            reader.dispose()
        }
    }

    private fun ImageDimensions.validate(): ImageDimensions {
        val pixels = width.toLong() * height
        val hasInvalidDimensions = width <= 0 || height <= 0
        val exceedsDimensionLimit =
            width > PhotoConstants.MAX_IMAGE_WIDTH ||
                height > PhotoConstants.MAX_IMAGE_HEIGHT
        val exceedsPixelLimit = pixels > PhotoConstants.MAX_IMAGE_PIXELS

        if (hasInvalidDimensions || exceedsDimensionLimit || exceedsPixelLimit) {
            throw InvalidPhotoException(ErrorMessages.PHOTO_DIMENSIONS_TOO_LARGE)
        }

        return this
    }

    private data class ImageDimensions(
        val width: Int,
        val height: Int,
    )

    companion object {
        private const val FIRST_IMAGE_INDEX = 0

        private val contentTypesByExtension = mapOf(
            "jpg" to MediaType.IMAGE_JPEG_VALUE,
            "jpeg" to MediaType.IMAGE_JPEG_VALUE,
            "png" to MediaType.IMAGE_PNG_VALUE,
            "webp" to "image/webp",
        )
        private val allowedContentTypes = contentTypesByExtension.values.toSet()
    }
}

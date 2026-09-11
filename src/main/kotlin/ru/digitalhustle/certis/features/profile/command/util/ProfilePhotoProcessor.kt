package ru.digitalhustle.certis.features.profile.command.util

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.profile.command.model.NewProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.command.model.ProcessedProfilePhoto
import ru.digitalhustle.certis.features.profile.exceptions.InvalidPhotoException
import ru.digitalhustle.certis.features.profile.util.ProfilePhotoUrlProvider
import ru.digitalhustle.certis.features.profile.validator.PhotoValidator
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

@Component
class ProfilePhotoProcessor(
    private val profilePhotoUrlProvider: ProfilePhotoUrlProvider,
    private val photoValidator: PhotoValidator,
    private val photoFormatDetector: ProfilePhotoFormatDetector,
) {

    fun process(profileId: UUID, photo: MultipartFile): ProcessedProfilePhoto {
        photoValidator.validateSize(photo)

        val baseFileName = photoValidator.validateOriginalFileName(photo.originalFilename)
        val photoBytes = photo.bytes.also { photoValidator.validateSize(it) }
        val format = photoFormatDetector.detect(photoBytes)
        val (width, height) = photoBytes.extractImageDimensions()

        photoValidator.validateDimensions(
            width = width,
            height = height,
        )

        val id = UUID.randomUUID()
        val objectName = "$id.${format.extension}"

        return ProcessedProfilePhoto(
            meta = NewProfilePhotoMeta(
                id = id,
                profileId = profileId,
                originalFileName = baseFileName,
                extension = format.extension,
                fileSize = photoBytes.size.toLong(),
                width = width,
                height = height,
                contentType = format.contentType,
                url = profilePhotoUrlProvider.get(profileId),
            ),
            objectName = objectName,
            contentType = format.contentType,
        )
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

    private data class ImageDimensions(
        val width: Int,
        val height: Int,
    )

    companion object {
        private const val FIRST_IMAGE_INDEX = 0
    }
}

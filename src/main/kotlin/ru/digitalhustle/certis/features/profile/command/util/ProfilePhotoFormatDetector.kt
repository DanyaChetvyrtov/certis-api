package ru.digitalhustle.certis.features.profile.command.util

import org.apache.tika.Tika
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.profile.exceptions.UnsupportedPhotoMediaTypeException

@Component
class ProfilePhotoFormatDetector(
    private val tika: Tika,
) {

    fun detect(bytes: ByteArray): ProfilePhotoFormat {
        val contentType = tika.detect(bytes)?.lowercase()
            ?: throw UnsupportedPhotoMediaTypeException(ErrorMessages.INVALID_CONTENT_TYPE)

        return FORMATS_BY_CONTENT_TYPE[contentType]
            ?: throw UnsupportedPhotoMediaTypeException(ErrorMessages.INVALID_CONTENT_TYPE)
    }

    private companion object {
        private val FORMATS_BY_CONTENT_TYPE = listOf(
            ProfilePhotoFormat(MediaType.IMAGE_JPEG_VALUE, "jpg"),
            ProfilePhotoFormat(MediaType.IMAGE_PNG_VALUE, "png"),
            ProfilePhotoFormat("image/webp", "webp"),
        ).associateBy(ProfilePhotoFormat::contentType)
    }
}

data class ProfilePhotoFormat(
    val contentType: String,
    val extension: String,
)

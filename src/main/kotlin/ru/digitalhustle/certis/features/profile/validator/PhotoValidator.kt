package ru.digitalhustle.certis.features.profile.validator

import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PhotoConstants
import ru.digitalhustle.certis.features.profile.exceptions.InvalidPhotoException

@Component
class PhotoValidator {

    fun validateOriginalFileName(originalFileName: String?): String {
        val sanitizedFileName = originalFileName
            ?.takeIf(String::isNotBlank)
            ?.let(FilenameUtils::getName)
            ?.takeIf(String::isNotBlank)
            ?: throw InvalidPhotoException(ErrorMessages.INVALID_FILE_NAME)

        return FilenameUtils.getBaseName(sanitizedFileName)
            .takeIf(String::isNotBlank)
            ?.takeIf { it.length <= PhotoConstants.MAX_ORIGINAL_FILE_NAME_LENGTH }
            ?: throw InvalidPhotoException(ErrorMessages.INVALID_FILE_NAME)
    }

    fun validateDimensions(width: Int, height: Int) {
        val pixels = width.toLong() * height
        val hasInvalidDimensions = width <= 0 || height <= 0
        val exceedsDimensionLimit =
            width > PhotoConstants.MAX_IMAGE_WIDTH ||
                height > PhotoConstants.MAX_IMAGE_HEIGHT
        val exceedsPixelLimit = pixels > PhotoConstants.MAX_IMAGE_PIXELS

        if (hasInvalidDimensions || exceedsDimensionLimit || exceedsPixelLimit) {
            throw InvalidPhotoException(ErrorMessages.PHOTO_DIMENSIONS_TOO_LARGE)
        }
    }

    fun validateSize(bytes: ByteArray) {
        if (bytes.isEmpty()) {
            throw InvalidPhotoException(ErrorMessages.EMPTY_PHOTO)
        }
        if (bytes.size > PhotoConstants.MAX_FILE_SIZE_BYTES) {
            throw InvalidPhotoException(ErrorMessages.PHOTO_TOO_LARGE)
        }
    }

    fun validateSize(file: MultipartFile) {
        if (file.isEmpty || file.size <= 0) {
            throw InvalidPhotoException(ErrorMessages.EMPTY_PHOTO)
        }
        if (file.size > PhotoConstants.MAX_FILE_SIZE_BYTES) {
            throw InvalidPhotoException(ErrorMessages.PHOTO_TOO_LARGE)
        }
    }
}

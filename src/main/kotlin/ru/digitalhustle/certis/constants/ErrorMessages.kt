package ru.digitalhustle.certis.constants

object ErrorMessages {

    const val BUCKET_CREATION_FAILED = "Bucket creation failed"
    const val PHOTO_UPLOAD_FAILED = "Photo upload failed"
    const val PHOTO_DOWNLOAD_FAILED = "Photo download failed"
    const val PHOTO_DELETE_FAILED = "Photo delete failed"
    const val PHOTO_STORAGE_UNAVAILABLE = "Photo storage is unavailable"
    const val EMPTY_PHOTO = "Photo is empty"
    const val PHOTO_TOO_LARGE = "Photo size must not exceed 5 MB"
    const val PHOTO_DIMENSIONS_TOO_LARGE = "Photo dimensions are too large"
    const val INVALID_FILE_NAME = "Failed to read file name"
    const val INVALID_FILE_EXTENSION = "Invalid file extension"
    const val INVALID_CONTENT_TYPE = "Invalid content type"
    const val FILE_EXTENSION_CONTENT_TYPE_MISMATCH = "File extension does not match its content type"
    const val INVALID_IMAGE_DIMENSIONS = "Failed to read image dimensions"

    const val INVALID_TOKEN = "Invalid or expired token"
    const val INVALID_CREDENTIALS = "Invalid email or password"
    const val AUTHENTICATION_REQUIRED = "Authentication is required"
    const val ACCESS_DENIED = "Access denied"
    const val TOO_MANY_REQUESTS = "Too many requests"
    const val PASSWORDS_MISMATCH = "Passwords don't match"

    const val ACCOUNT_CLOSED = "Closed account cannot be updated"

    const val ERROR_MESSAGES_SEPARATOR = "; "
    const val VALIDATION_FAILED = "Validation failed"
}

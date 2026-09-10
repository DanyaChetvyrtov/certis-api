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
    const val INVALID_CONTENT_TYPE = "Invalid content type"
    const val INVALID_IMAGE_DIMENSIONS = "Failed to read image dimensions"

    const val INVALID_TOKEN = "Invalid or expired token"
    const val INVALID_CREDENTIALS = "Invalid email or password"
    const val AUTHENTICATION_REQUIRED = "Authentication is required"
    const val ACCESS_DENIED = "Access denied"
    const val TOO_MANY_REQUESTS = "Too many requests"
    const val PASSWORDS_MISMATCH = "Passwords don't match"

    const val ACCOUNT_CLOSED = "Closed account cannot be updated"
    const val ACCOUNT_IN_USE = "Account is used by an active or paused recurring transaction"

    const val CATEGORY_ARCHIVED = "Archived category cannot be updated"
    const val CATEGORY_IN_USE = "Category is used by an active budget or recurring transaction template"

    const val BUDGET_ALLOCATIONS_EXCEED_INCOME = "Allocations and savings target must not exceed monthly income"
    const val BUDGET_DUPLICATE_CATEGORIES = "Each category can be allocated only once per budget"
    const val BUDGET_CATEGORY_INVALID = "Budget categories must be active expense categories owned by the user"
    const val BUDGET_CONSTRAINT_VIOLATION = "Budget violates allocation constraints"
    const val BUDGET_OPTIMIZATION_NOT_PROPOSED = "Only a proposed budget optimization can be changed"
    const val BUDGET_OPTIMIZATION_STALE = "Budget has changed since the optimization was generated"

    const val GOAL_ACCOUNT_CLOSED = "Closed account cannot be used for a goal contribution"
    const val GOAL_ACCOUNT_CURRENCY_MISMATCH = "Goal and account currencies must match"
    const val GOAL_TERMINAL = "Achieved or cancelled goal cannot be changed"
    const val GOAL_NOT_ACTIVE = "Only an active goal can receive contributions"
    const val GOAL_PLAN_INVALID = "Custom plan requires a monthly amount; recommended plan calculates it"
    const val GOAL_TARGET_MONTH_PAST = "Target month must not be in the past"
    const val GOAL_INITIAL_AMOUNT_INVALID = "Initial contribution must be less than target amount"
    const val GOAL_REVERSAL_INVALID = "Only an unreversed contribution can be refunded"
    const val GOAL_IDEMPOTENCY_CONFLICT = "Idempotency key is already used for another contribution"

    const val ERROR_MESSAGES_SEPARATOR = "; "
    const val VALIDATION_FAILED = "Validation failed"
}

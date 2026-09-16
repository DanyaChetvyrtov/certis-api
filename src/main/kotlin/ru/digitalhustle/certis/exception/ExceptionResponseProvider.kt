package ru.digitalhustle.certis.exception

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.api.dto.response.ExceptionRs
import ru.digitalhustle.certis.util.time.ApplicationClock

@Component
class ExceptionResponseProvider(
    private val applicationClock: ApplicationClock,
) {

    fun createResponse(
        status: HttpStatus,
        message: String,
        errors: Map<String, String>? = null,
        code: String? = null,
        details: Map<String, Any?>? = null,
    ): ExceptionRs =
        ExceptionRs(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            timestamp = applicationClock.now(),
            errors = errors,
            code = code,
            details = details,
        )

    fun createBadRequest(message: String, errors: Map<String, String>? = null): ExceptionRs =
        createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = message,
            errors = errors,
        )

    fun createUnauthorized(message: String): ExceptionRs =
        createResponse(
            status = HttpStatus.UNAUTHORIZED,
            message = message,
        )

    fun createNotFound(
        message: String,
        code: String? = null,
        details: Map<String, Any?>? = null,
    ): ExceptionRs =
        createResponse(
            status = HttpStatus.NOT_FOUND,
            message = message,
            code = code,
            details = details,
        )

    fun createConflict(
        message: String,
        code: String? = null,
        details: Map<String, Any?>? = null,
    ): ExceptionRs =
        createResponse(
            status = HttpStatus.CONFLICT,
            message = message,
            code = code,
            details = details,
        )

    fun createInternalServerError(): ExceptionRs =
        createResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
        )
}

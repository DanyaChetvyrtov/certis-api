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
    ): ExceptionRs =
        ExceptionRs(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            timestamp = applicationClock.now(),
            errors = errors,
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

    fun createNotFound(message: String): ExceptionRs =
        createResponse(
            status = HttpStatus.NOT_FOUND,
            message = message,
        )

    fun createConflict(message: String): ExceptionRs =
        createResponse(
            status = HttpStatus.CONFLICT,
            message = message,
        )

    fun createInternalServerError(): ExceptionRs =
        createResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
        )
}

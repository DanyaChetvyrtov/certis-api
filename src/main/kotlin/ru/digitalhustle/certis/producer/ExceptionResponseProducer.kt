package ru.digitalhustle.certis.producer

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.dto.response.ExceptionRs
import java.time.Clock
import java.time.OffsetDateTime

@Component
class ExceptionResponseProducer(
    private val clock: Clock,
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
            timestamp = OffsetDateTime.now(clock),
            errors = errors,
        )

    fun createBadRequest(message: String, errors: Map<String, String>? = null): ExceptionRs =
        createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = message,
            errors = errors
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

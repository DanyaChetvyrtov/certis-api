package ru.digitalhustle.certis.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.dto.response.ExceptionRs
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.exception.custom.MissedTokenException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.exception.custom.PasswordsDoNotMatchException
import java.time.Clock
import java.time.OffsetDateTime

@RestControllerAdvice
class BaseExceptionHandler(
    private val clock: Clock,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = ErrorMessages.VALIDATION_FAILED,
            errors = exception.extractFieldErrors(),
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = ErrorMessages.VALIDATION_FAILED,
            errors = exception.extractFieldErrors(),
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = "${ErrorMessages.VALIDATION_FAILED}. Invalid value.",
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(exception: MethodArgumentTypeMismatchException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = exception.message ?: ErrorMessages.VALIDATION_FAILED,
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PasswordsDoNotMatchException::class)
    fun handlePasswordsDoNotMatchException(exception: PasswordsDoNotMatchException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = exception.message ?: HttpStatus.BAD_REQUEST.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidTokenException::class, MissedTokenException::class)
    fun handleTokenException(exception: RuntimeException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createUnauthorizedResponse(exception)
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(exception: BadCredentialsException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createUnauthorizedResponse(exception)
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(exception: NotFoundException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.NOT_FOUND,
            message = exception.message ?: HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EntityAlreadyExistsException::class)
    fun handleEntityAlreadyExistsException(exception: EntityAlreadyExistsException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.CONFLICT,
            message = exception.message ?: HttpStatus.CONFLICT.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ExceptionRs {
        log.error(exception) { exception.message.orEmpty() }

        return createResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
        )
    }

    private fun createUnauthorizedResponse(exception: RuntimeException): ExceptionRs =
        createResponse(
            status = HttpStatus.UNAUTHORIZED,
            message = exception.message ?: HttpStatus.UNAUTHORIZED.reasonPhrase,
        )

    private fun createResponse(
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
}

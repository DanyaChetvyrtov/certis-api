package ru.digitalhustle.certis.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.dto.response.ExceptionRs
import ru.digitalhustle.certis.exception.custom.PhotoProcessingException
import ru.digitalhustle.certis.provider.ExceptionResponseProvider

@RestControllerAdvice
class BaseExceptionHandler(
    private val exceptionResponseProvider: ExceptionResponseProvider,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createBadRequest(
            message = ErrorMessages.VALIDATION_FAILED,
            errors = exception.extractFieldErrors(),
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createBadRequest(
            message = ErrorMessages.VALIDATION_FAILED,
            errors = exception.extractFieldErrors(),
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = "${ErrorMessages.VALIDATION_FAILED}. Invalid value.",
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(exception: MethodArgumentTypeMismatchException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = ErrorMessages.VALIDATION_FAILED,
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(exception: MissingServletRequestParameterException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = ErrorMessages.VALIDATION_FAILED,
        )
    }

    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(exception: MaxUploadSizeExceededException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.PAYLOAD_TOO_LARGE,
            message = ErrorMessages.PHOTO_TOO_LARGE,
        )
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(exception: AuthorizationDeniedException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.FORBIDDEN,
            message = ErrorMessages.ACCESS_DENIED,
        )
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(PhotoProcessingException::class)
    fun handlePhotoProcessingException(exception: PhotoProcessingException): ExceptionRs {
        log.error(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            message = ErrorMessages.PHOTO_STORAGE_UNAVAILABLE,
        )
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createNotFound(
            message = HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ExceptionRs {
        log.error(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createInternalServerError()
    }
}

package ru.digitalhustle.certis.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.dto.response.ExceptionRs
import ru.digitalhustle.certis.producer.ExceptionResponseProducer

@RestControllerAdvice
class BaseExceptionHandler(
    private val exceptionResponseProducer: ExceptionResponseProducer,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createBadRequest(
            message = ErrorMessages.VALIDATION_FAILED,
            errors = exception.extractFieldErrors(),
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(exception: HandlerMethodValidationException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createBadRequest(
            message = ErrorMessages.VALIDATION_FAILED,
            errors = exception.extractFieldErrors(),
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = "${ErrorMessages.VALIDATION_FAILED}. Invalid value.",
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(exception: MethodArgumentTypeMismatchException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = ErrorMessages.VALIDATION_FAILED,
        )
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ExceptionRs {
        log.error(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createInternalServerError()
    }
}

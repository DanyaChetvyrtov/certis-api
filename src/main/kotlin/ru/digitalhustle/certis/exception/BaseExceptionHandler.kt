package ru.digitalhustle.certis.exception

import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.digitalhustle.certis.dto.response.ExceptionRs
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import java.time.Clock

@RestControllerAdvice
class BaseExceptionHandler(
    private val clock: Clock,
) {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException
    ): ExceptionRs {
        log.warn(exception) {
            ExceptionConstants.LOG_MESSAGE.format(exception.message)
        }

        return ExceptionResponseFactory.create(
            status = HttpStatus.BAD_REQUEST,
            message = ErrorMessages.VALIDATION_FAILED,
            clock = clock,
            errors = exception.extractFieldErrors()
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(
        exception: HandlerMethodValidationException
    ): ExceptionRs {
        log.warn(exception) {
            ExceptionConstants.LOG_MESSAGE.format(exception.message)
        }

        return ExceptionResponseFactory.create(
            status = HttpStatus.BAD_REQUEST,
            message = ErrorMessages.VALIDATION_FAILED,
            clock = clock,
            errors = exception.extractFieldErrors()
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        exception: HttpMessageNotReadableException
    ): ExceptionRs {
        log.warn(exception) {
            ExceptionConstants.LOG_MESSAGE.format(exception.message)
        }

        return ExceptionResponseFactory.create(
            status = HttpStatus.BAD_REQUEST,
            message = "${ErrorMessages.VALIDATION_FAILED} Invalid value.",
            clock = clock
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        exception: MethodArgumentTypeMismatchException
    ): ExceptionRs {
        log.warn(exception) {
            ExceptionConstants.LOG_MESSAGE.format(exception.message)
        }

        return ExceptionResponseFactory.create(
            status = HttpStatus.BAD_REQUEST,
            message = exception.message ?: ErrorMessages.VALIDATION_FAILED,
            clock = clock
        )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        exception: NotFoundException
    ): ExceptionRs {
        log.warn(exception) {
            ExceptionConstants.LOG_MESSAGE.format(exception.message)
        }

        return ExceptionResponseFactory.create(
            status = HttpStatus.NOT_FOUND,
            message = exception.message ?: HttpStatus.NOT_FOUND.reasonPhrase,
            clock = clock
        )
    }

    @ExceptionHandler(EntityAlreadyExistsException::class)
    fun handleConflictException(
        exception: NotFoundException
    ): ExceptionRs {
        log.warn(exception) {
            ExceptionConstants.LOG_MESSAGE.format(exception.message)
        }

        return ExceptionResponseFactory.create(
            status = HttpStatus.NOT_FOUND,
            message = exception.message ?: HttpStatus.NOT_FOUND.reasonPhrase,
            clock = clock
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception
    ): ExceptionRs {
        log.error(exception) {
            ExceptionConstants.LOG_MESSAGE.format(exception.message)
        }

        return ExceptionResponseFactory.create(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            clock = clock
        )
    }
}
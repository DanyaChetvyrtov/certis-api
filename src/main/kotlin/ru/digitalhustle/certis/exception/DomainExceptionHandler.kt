package ru.digitalhustle.certis.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.dto.response.ExceptionRs
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.exception.custom.MissedTokenException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.exception.custom.PasswordsDoNotMatchException
import ru.digitalhustle.certis.producer.ExceptionResponseProducer

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class DomainExceptionHandler(
    private val exceptionResponseProducer: ExceptionResponseProducer,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PasswordsDoNotMatchException::class)
    fun handlePasswordsDoNotMatchException(exception: PasswordsDoNotMatchException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createBadRequest(
            message = exception.message ?: ErrorMessages.PASSWORDS_MISMATCH,
        )
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidTokenException::class, MissedTokenException::class)
    fun handleTokenException(exception: RuntimeException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createUnauthorized(
            message = exception.message ?: HttpStatus.UNAUTHORIZED.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(exception: BadCredentialsException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createUnauthorized(
            message = exception.message ?: HttpStatus.UNAUTHORIZED.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(exception: NotFoundException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createNotFound(
            message = exception.message ?: HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EntityAlreadyExistsException::class)
    fun handleEntityAlreadyExistsException(exception: EntityAlreadyExistsException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProducer.createConflict(
            message = exception.message ?: HttpStatus.CONFLICT.reasonPhrase,
        )
    }
}

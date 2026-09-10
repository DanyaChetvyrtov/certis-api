package ru.digitalhustle.certis.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.digitalhustle.certis.api.dto.response.ExceptionRs
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.DomainException
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.account.exceptions.AccountClosedException
import ru.digitalhustle.certis.features.account.exceptions.AccountInUseException
import ru.digitalhustle.certis.features.budget.exceptions.BudgetOptimizationConflictException
import ru.digitalhustle.certis.features.budget.exceptions.InvalidBudgetException
import ru.digitalhustle.certis.features.category.exceptions.CategoryArchivedException
import ru.digitalhustle.certis.features.category.exceptions.CategoryInUseException
import ru.digitalhustle.certis.features.goal.exceptions.InvalidGoalException
import ru.digitalhustle.certis.features.profile.exceptions.InvalidPhotoException
import ru.digitalhustle.certis.features.profile.exceptions.UnsupportedPhotoMediaTypeException
import ru.digitalhustle.certis.features.security.exceptions.InvalidTokenException
import ru.digitalhustle.certis.features.security.exceptions.MissedTokenException
import ru.digitalhustle.certis.features.security.exceptions.PasswordsDoNotMatchException
import ru.digitalhustle.certis.features.transaction.exceptions.InvalidRecurringTransactionException
import ru.digitalhustle.certis.features.transaction.exceptions.InvalidTransactionException
import ru.digitalhustle.certis.features.transaction.exceptions.InvalidTransferException

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class DomainExceptionHandler(
    private val exceptionResponseProvider: ExceptionResponseProvider,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PasswordsDoNotMatchException::class)
    fun handlePasswordsDoNotMatchException(exception: PasswordsDoNotMatchException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createBadRequest(
            message = exception.message ?: ErrorMessages.PASSWORDS_MISMATCH,
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidPhotoException::class)
    fun handleInvalidPhotoException(exception: InvalidPhotoException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createBadRequest(
            message = exception.message ?: ErrorMessages.VALIDATION_FAILED,
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(
        InvalidTransactionException::class,
        InvalidRecurringTransactionException::class,
        InvalidTransferException::class,
        InvalidGoalException::class,
    )
    fun handleInvalidFinancialOperationException(exception: DomainException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createBadRequest(
            message = exception.message ?: ErrorMessages.VALIDATION_FAILED,
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(InvalidBudgetException::class)
    fun handleInvalidBudgetException(exception: InvalidBudgetException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            message = exception.message ?: ErrorMessages.VALIDATION_FAILED,
        )
    }

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(UnsupportedPhotoMediaTypeException::class)
    fun handleUnsupportedPhotoMediaTypeException(exception: UnsupportedPhotoMediaTypeException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createResponse(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            message = exception.message ?: HttpStatus.UNSUPPORTED_MEDIA_TYPE.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidTokenException::class, MissedTokenException::class)
    fun handleTokenException(exception: RuntimeException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createUnauthorized(
            message = exception.message ?: HttpStatus.UNAUTHORIZED.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(exception: AuthenticationException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createUnauthorized(
            message = ErrorMessages.INVALID_CREDENTIALS,
        )
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(exception: NotFoundException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createNotFound(
            message = exception.message ?: HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(
        EntityAlreadyExistsException::class,
        AccountClosedException::class,
        AccountInUseException::class,
        BudgetOptimizationConflictException::class,
        CategoryArchivedException::class,
        CategoryInUseException::class,
    )
    fun handleConflictException(exception: DomainException): ExceptionRs {
        log.warn(exception) { exception.message.orEmpty() }

        return exceptionResponseProvider.createConflict(
            message = exception.message ?: HttpStatus.CONFLICT.reasonPhrase,
        )
    }
}

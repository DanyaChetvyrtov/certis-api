package ru.digitalhustle.certis.exception

import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import ru.digitalhustle.certis.constants.ErrorMessages

fun MethodArgumentNotValidException.extractFieldErrors(): Map<String, String> =
    bindingResult.fieldErrors
        .mapNotNull { error ->
            error.defaultMessage?.let { message -> error.field to message }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, messages) ->
            messages.joinToString(ErrorMessages.ERROR_MESSAGES_SEPARATOR)
        }

fun HandlerMethodValidationException.extractFieldErrors(): Map<String, String> =
    parameterValidationResults
        .mapNotNull { result ->
            val parameterName = result.methodParameter.parameterName ?: return@mapNotNull null

            val message = result.resolvableErrors
                .mapNotNull { it.defaultMessage }
                .filter { it.isNotBlank() }
                .joinToString(ErrorMessages.ERROR_MESSAGES_SEPARATOR)

            parameterName to message
        }
        .filter { (_, message) -> message.isNotBlank() }
        .toMap()

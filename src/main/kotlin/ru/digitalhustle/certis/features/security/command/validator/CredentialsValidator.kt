package ru.digitalhustle.certis.features.security.command.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.security.command.model.UserCredentials
import ru.digitalhustle.certis.features.security.exceptions.PasswordsDoNotMatchException

@Component
class CredentialsValidator {

    fun validateRegistration(credentials: UserCredentials) {
        if (credentials.password != credentials.passwordConfirmation) {
            throw PasswordsDoNotMatchException(ErrorMessages.PASSWORDS_MISMATCH)
        }
    }
}

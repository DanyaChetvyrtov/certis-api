package ru.digitalhustle.certis.provider

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.dto.request.RegisterRq

@Component
class SecurityRequestProvider {

    companion object {
        const val NORMALIZED_EMAIL = "john@test.com"
        const val PASSWORD = "password"
    }

    fun defaultRegistrationRq(): RegisterRq = RegisterRq(
        email = NORMALIZED_EMAIL,
        password = PASSWORD,
        passwordConfirmation = PASSWORD,
    )
}

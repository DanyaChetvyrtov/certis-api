package ru.digitalhustle.certis.features.security.query.service

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.security.model.User
import java.util.UUID

interface UserQueryService {

    fun getUserById(id: UUID): User

    fun getUserByEmail(email: String): User

    fun getPreferredCurrency(id: UUID): Currency
}

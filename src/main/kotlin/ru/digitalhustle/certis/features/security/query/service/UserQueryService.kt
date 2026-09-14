package ru.digitalhustle.certis.features.security.query.service

import ru.digitalhustle.certis.features.security.model.User
import ru.digitalhustle.certis.shared.enums.Currency
import java.util.UUID

interface UserQueryService {

    fun getUserById(id: UUID): User

    fun getUserByEmail(email: String): User

    fun getPreferredCurrency(id: UUID): Currency
}

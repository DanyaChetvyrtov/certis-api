package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.entity.User
import java.util.UUID

interface UserService {

    fun getUserById(id: UUID): User

    fun getUserByEmail(email: String): User

    fun save(email: String, password: String): User

    fun updateLastLogin(id: UUID)

    fun updatePreferredCurrency(id: UUID, preferredCurrency: Currency)

    fun delete(id: UUID)
}

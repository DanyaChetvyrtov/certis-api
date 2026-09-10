package ru.digitalhustle.certis.features.security.command.service

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.security.model.User
import java.util.UUID

interface UserService {

    fun getUserById(id: UUID): User

    fun save(email: String, password: String): User

    fun updateLastLogin(id: UUID)

    fun updatePreferredCurrency(id: UUID, preferredCurrency: Currency)

    fun delete(id: UUID)

    fun findPreferredCurrencyForUpdate(userId: UUID): Currency?
}

package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.User
import java.util.*

interface UserService {

    fun getUser(email: String): User

    fun save(email: String, password: String): User

    fun updateLastLogin(id: UUID)
}
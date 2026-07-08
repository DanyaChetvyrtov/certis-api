package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.User
import java.util.UUID

interface UserService {

    fun getById(id: UUID): User

    fun create(user: User): User

    fun update(user: User): User

    fun deleteById(id: UUID)
}
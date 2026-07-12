package ru.digitalhustle.certis.service.security

import ru.digitalhustle.certis.model.UserCredentials
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtData

interface AuthService {

    fun register(userWithCredentials: UserCredentials): User

    fun login(user: User): JwtData

    fun refreshAccess(refreshToken: String): JwtData

    fun refreshTokens(refreshToken: String): JwtData
}

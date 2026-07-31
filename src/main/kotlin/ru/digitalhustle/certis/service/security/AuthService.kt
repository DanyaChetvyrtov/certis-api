package ru.digitalhustle.certis.service.security

import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.model.security.UserCredentials

interface AuthService {

    fun register(userCredentials: UserCredentials): User

    fun login(userCredentials: UserCredentials): JwtData

    fun refreshAccess(refreshToken: String): JwtData

    fun refreshTokens(refreshToken: String): JwtData
}

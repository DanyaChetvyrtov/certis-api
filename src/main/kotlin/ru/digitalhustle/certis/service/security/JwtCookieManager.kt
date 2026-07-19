package ru.digitalhustle.certis.service.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie

interface JwtCookieManager {

    fun createAccessTokenCookie(accessToken: String): ResponseCookie

    fun createRefreshTokenCookie(refreshToken: String): ResponseCookie

    fun createAccessTokenRemovalCookie(): ResponseCookie

    fun createRefreshTokenRemovalCookie(): ResponseCookie

    fun getAccessTokenFromRequest(request: HttpServletRequest): String?
}

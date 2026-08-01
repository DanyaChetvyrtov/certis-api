package ru.digitalhustle.certis.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.producer.ExceptionResponseProducer

@Component
class RestSecurityErrorHandler(
    private val objectMapper: ObjectMapper,
    private val exceptionResponseProducer: ExceptionResponseProducer,
) : AuthenticationEntryPoint,
    AccessDeniedHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        writeError(response, HttpStatus.UNAUTHORIZED, ErrorMessages.AUTHENTICATION_REQUIRED)
    }

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        writeError(response, HttpStatus.FORBIDDEN, ErrorMessages.ACCESS_DENIED)
    }

    private fun writeError(response: HttpServletResponse, status: HttpStatus, message: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.writer,
            exceptionResponseProducer.createResponse(status, message),
        )
    }
}

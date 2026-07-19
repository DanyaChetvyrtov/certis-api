package ru.digitalhustle.certis.units.producer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.http.HttpStatus
import ru.digitalhustle.certis.producer.ExceptionResponseProducer
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ExceptionResponseProducerTest {

    private val exceptionResponseProducer = ExceptionResponseProducer(
        Clock.fixed(NOW, ZoneOffset.UTC),
    )

    private companion object {
        private const val MESSAGE = "Exception message"

        private val NOW = Instant.parse("2026-07-19T10:00:00Z")
    }

    @Test
    fun `should create response`() {
        // given
        val errors = mapOf("email" to "must be a well-formed email address")

        // when
        val response = exceptionResponseProducer.createResponse(
            status = HttpStatus.BAD_REQUEST,
            message = MESSAGE,
            errors = errors,
        )

        // then
        assertAll(
            { assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value()) },
            { assertThat(response.error).isEqualTo(HttpStatus.BAD_REQUEST.reasonPhrase) },
            { assertThat(response.message).isEqualTo(MESSAGE) },
            { assertThat(response.timestamp).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)) },
            { assertThat(response.errors).isEqualTo(errors) },
        )
    }

    @Test
    fun `should create bad request response`() {
        // when
        val response = exceptionResponseProducer.createBadRequest(MESSAGE)

        // then
        assertAll(
            { assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value()) },
            { assertThat(response.error).isEqualTo(HttpStatus.BAD_REQUEST.reasonPhrase) },
            { assertThat(response.message).isEqualTo(MESSAGE) },
        )
    }

    @Test
    fun `should create unauthorized response`() {
        // when
        val response = exceptionResponseProducer.createUnauthorized(MESSAGE)

        // then
        assertAll(
            { assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value()) },
            { assertThat(response.error).isEqualTo(HttpStatus.UNAUTHORIZED.reasonPhrase) },
            { assertThat(response.message).isEqualTo(MESSAGE) },
        )
    }

    @Test
    fun `should create not found response`() {
        // when
        val response = exceptionResponseProducer.createNotFound(MESSAGE)

        // then
        assertAll(
            { assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND.value()) },
            { assertThat(response.error).isEqualTo(HttpStatus.NOT_FOUND.reasonPhrase) },
            { assertThat(response.message).isEqualTo(MESSAGE) },
        )
    }

    @Test
    fun `should create conflict response`() {
        // when
        val response = exceptionResponseProducer.createConflict(MESSAGE)

        // then
        assertAll(
            { assertThat(response.status).isEqualTo(HttpStatus.CONFLICT.value()) },
            { assertThat(response.error).isEqualTo(HttpStatus.CONFLICT.reasonPhrase) },
            { assertThat(response.message).isEqualTo(MESSAGE) },
        )
    }

    @Test
    fun `should create internal server error response without leaking exception message`() {
        // when
        val response = exceptionResponseProducer.createInternalServerError()

        // then
        assertAll(
            { assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value()) },
            { assertThat(response.error).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase) },
            { assertThat(response.message).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase) },
        )
    }
}

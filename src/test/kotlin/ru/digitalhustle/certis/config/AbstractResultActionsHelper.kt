package ru.digitalhustle.certis.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.ResultActions
import java.nio.charset.StandardCharsets

inline fun <reified T> ResultActions.body(
    objectMapper: ObjectMapper,
): T =
    objectMapper.readValue(
        andReturn().response.contentAsByteArray,
        object : TypeReference<T>() {},
    )

fun ResultActions.bodyAsString(): String =
    andReturn().response.getContentAsString(StandardCharsets.UTF_8)

inline fun <reified T> ResultActions.bodyList(
    objectMapper: ObjectMapper,
): List<T> =
    objectMapper.readValue(
        andReturn().response.contentAsByteArray,
        object : TypeReference<List<T>>() {},
    )

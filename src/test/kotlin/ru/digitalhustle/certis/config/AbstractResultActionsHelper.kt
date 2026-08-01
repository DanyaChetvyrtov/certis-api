package ru.digitalhustle.certis.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.ResultActions

abstract class AbstractResultActionsHelper {

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected fun <T> getBody(resultActions: ResultActions, clazz: Class<T>): T =
        objectMapper.readValue(
            resultActions.andReturn().response.contentAsByteArray,
            clazz,
        )

    protected fun getBody(resultActions: ResultActions): String =
        resultActions
            .andReturn().response
            .getContentAsString(Charsets.UTF_8)

    protected fun <T> getListBody(resultActions: ResultActions, clazz: Class<T>): List<T> =
        objectMapper.readValue(
            resultActions.andReturn().response.contentAsByteArray,
            objectMapper.typeFactory
                .constructParametricType(List::class.java, clazz),
        )

    protected fun getHeaders(resultActions: ResultActions, headersName: String): List<String> =
        resultActions.andReturn().response.getHeaders(headersName)
}

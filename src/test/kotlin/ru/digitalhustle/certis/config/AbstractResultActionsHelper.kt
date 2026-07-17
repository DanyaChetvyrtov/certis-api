package ru.digitalhustle.certis.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.ResultActions
import java.nio.charset.StandardCharsets

abstract class AbstractResultActionsHelper(
    @Autowired
    protected open var objectMapper: ObjectMapper,
) {

    protected fun <T> getBody(resultActions: ResultActions, clazz: Class<T?>?): T? = objectMapper.readValue<T?>(
        resultActions.andReturn().response.contentAsByteArray,
        clazz,
    )

    protected fun getBody(resultActions: ResultActions): String = resultActions.andReturn().response.getContentAsString(
        StandardCharsets.UTF_8,
    )

    protected fun <T> getListBody(resultActions: ResultActions, clazz: Class<T?>?): MutableList<T?>? =
        objectMapper.readValue<MutableList<T?>?>(
            resultActions.andReturn().response.contentAsByteArray,
            objectMapper.typeFactory.constructParametricType(MutableList::class.java, clazz),
        )
}

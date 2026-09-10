package ru.digitalhustle.certis.features.category.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.category.exceptions.CategoryInUseException

@Component
class CategoryArchiveValidator {

    fun validate(isRequired: Boolean) {
        if (isRequired) {
            throw CategoryInUseException(ErrorMessages.CATEGORY_IN_USE)
        }
    }
}

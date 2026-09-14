package ru.digitalhustle.certis.features.category.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class CategoryInUseException(
    message: String,
) : DomainException(message)

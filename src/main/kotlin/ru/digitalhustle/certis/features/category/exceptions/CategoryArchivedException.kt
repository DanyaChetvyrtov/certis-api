package ru.digitalhustle.certis.features.category.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class CategoryArchivedException(
    message: String,
) : DomainException(message)

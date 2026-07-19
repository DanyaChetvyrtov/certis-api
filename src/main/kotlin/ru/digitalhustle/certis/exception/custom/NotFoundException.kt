package ru.digitalhustle.certis.exception.custom

class NotFoundException(
    message: String,
) : DomainException(message) {

    companion object {
        fun entity(entityName: String) =
            NotFoundException("$entityName not found")
    }
}

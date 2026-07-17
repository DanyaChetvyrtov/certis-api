package ru.digitalhustle.certis.exception.custom

class EntityAlreadyExistsException(
    message: String,
) : DomainException(message) {

    companion object {
        fun entity(entityName: String, parameterName: String) =
            EntityAlreadyExistsException("$entityName with such $parameterName already exists")
    }
}

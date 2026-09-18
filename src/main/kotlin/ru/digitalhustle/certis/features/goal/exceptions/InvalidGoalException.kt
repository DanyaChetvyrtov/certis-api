package ru.digitalhustle.certis.features.goal.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class InvalidGoalException(
    message: String,
) : DomainException(message)

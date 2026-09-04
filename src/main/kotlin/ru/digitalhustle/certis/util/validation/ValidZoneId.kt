package ru.digitalhustle.certis.util.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.time.DateTimeException
import java.time.ZoneId
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [ZoneIdValidator::class])
annotation class ValidZoneId(
    val message: String = "must be a valid time zone ID",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class ZoneIdValidator : ConstraintValidator<ValidZoneId, String> {

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean =
        value == null || tryParse(value)

    private fun tryParse(value: String): Boolean =
        try {
            ZoneId.of(value)
            true
        } catch (_: DateTimeException) {
            false
        }
}

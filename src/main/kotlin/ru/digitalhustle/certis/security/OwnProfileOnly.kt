package ru.digitalhustle.certis.security

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("#profileId == authentication.principal.id")
annotation class OwnProfileOnly

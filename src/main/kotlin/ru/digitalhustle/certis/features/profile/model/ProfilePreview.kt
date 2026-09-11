package ru.digitalhustle.certis.features.profile.model

import ru.digitalhustle.certis.enums.Currency
import java.time.LocalDate
import java.util.UUID

data class ProfilePreview(

    val id: UUID,

    val name: String,

    val surname: String,

    val dateOfBirth: LocalDate,

    val preferredCurrency: Currency,

    val photoUrl: String?,
)

package ru.digitalhustle.certis.features.profile.model

import ru.digitalhustle.certis.enums.Currency

fun Profile.toPreview(
    photoUrl: String?,
    preferredCurrency: Currency,
): ProfilePreview =
    ProfilePreview(
        id = id,
        name = name,
        surname = surname,
        dateOfBirth = dateOfBirth,
        preferredCurrency = preferredCurrency,
        photoUrl = photoUrl,
    )

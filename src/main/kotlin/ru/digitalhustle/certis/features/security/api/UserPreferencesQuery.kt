package ru.digitalhustle.certis.features.security.api

import ru.digitalhustle.certis.enums.Currency
import java.util.UUID

interface UserPreferencesQuery {

    fun getPreferredCurrency(userId: UUID): Currency
}

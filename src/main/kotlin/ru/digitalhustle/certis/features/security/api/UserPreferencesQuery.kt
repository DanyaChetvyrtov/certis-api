package ru.digitalhustle.certis.features.security.api

import ru.digitalhustle.certis.shared.enums.Currency
import java.util.UUID

interface UserPreferencesQuery {

    fun getPreferredCurrency(userId: UUID): Currency
}

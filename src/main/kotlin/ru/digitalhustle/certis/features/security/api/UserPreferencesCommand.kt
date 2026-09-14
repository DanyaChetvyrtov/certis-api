package ru.digitalhustle.certis.features.security.api

import ru.digitalhustle.certis.shared.enums.Currency
import java.util.UUID

interface UserPreferencesCommand {

    fun findPreferredCurrencyForUpdate(userId: UUID): Currency?

    fun updatePreferredCurrency(userId: UUID, currency: Currency)
}

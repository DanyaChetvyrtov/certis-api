package ru.digitalhustle.certis.features.security.api.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.security.api.UserPreferencesCommand
import ru.digitalhustle.certis.features.security.command.service.UserService
import java.util.UUID

@Service
class UserPreferencesCommandImpl(
    private val userService: UserService,
) : UserPreferencesCommand {

    override fun findPreferredCurrencyForUpdate(userId: UUID): Currency? =
        userService.findPreferredCurrencyForUpdate(userId)

    override fun updatePreferredCurrency(userId: UUID, currency: Currency): Unit =
        userService.updatePreferredCurrency(userId, currency)
}

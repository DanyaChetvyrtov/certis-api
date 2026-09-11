package ru.digitalhustle.certis.features.security.api.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.security.api.UserPreferencesQuery
import ru.digitalhustle.certis.features.security.query.service.UserQueryService
import java.util.UUID

@Service
class UserPreferencesQueryImpl(
    private val userQueryService: UserQueryService,
) : UserPreferencesQuery {

    override fun getPreferredCurrency(userId: UUID): Currency = userQueryService.getPreferredCurrency(userId)
}

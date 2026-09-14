package ru.digitalhustle.certis.features.account.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.account.constants.AccountErrorMessages
import ru.digitalhustle.certis.features.account.exceptions.AccountInUseException

@Component
class AccountClosureValidator {

    fun validate(hasSchedulableRecurringTransactions: Boolean) {
        if (hasSchedulableRecurringTransactions) {
            throw AccountInUseException(AccountErrorMessages.ACCOUNT_IN_USE)
        }
    }
}

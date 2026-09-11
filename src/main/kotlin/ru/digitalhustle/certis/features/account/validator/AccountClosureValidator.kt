package ru.digitalhustle.certis.features.account.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.account.exceptions.AccountInUseException

@Component
class AccountClosureValidator {

    fun validate(hasSchedulableRecurringTransactions: Boolean) {
        if (hasSchedulableRecurringTransactions) {
            throw AccountInUseException(ErrorMessages.ACCOUNT_IN_USE)
        }
    }
}

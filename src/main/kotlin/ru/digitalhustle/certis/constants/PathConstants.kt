package ru.digitalhustle.certis.constants

import java.util.UUID

object PathConstants {

    const val API = "/api"
    const val API_V1 = "$API/v1"

    const val AUTH = "$API_V1/auth"
    const val REGISTRATION = "/registration"
    const val TOKENS = "/tokens"
    const val LOGOUT = "/logout"
    const val SESSIONS = "/sessions"
    const val SESSION_ID = "/{sessionId}"

    const val AUTH_REGISTRATION = "$AUTH$REGISTRATION"
    const val AUTH_TOKEN = "$AUTH$TOKENS"
    const val AUTH_LOGOUT = "$AUTH$LOGOUT"
    const val AUTH_SESSIONS = "$AUTH$SESSIONS"
    const val SESSIONS_WITH_ID = "${SESSIONS}${SESSION_ID}"

    const val PROFILES = "$API_V1/profiles"
    const val PROFILE_ID = "/{profileId}"
    const val MY_PROFILE = "/me"
    const val PROFILE_PHOTO = "$PROFILE_ID/photo"

    fun profilePhoto(profileId: UUID): String = "$PROFILES/$profileId/photo"

    const val ACCOUNTS = "$API_V1/accounts"
    const val ACCOUNT_ID = "/{accountId}"

    const val CATEGORIES = "$API_V1/categories"
    const val CATEGORY_ANALYTICS = "/analytics"
    const val CATEGORY_OPTIONS = "/options"
    const val CATEGORY_ID = "/{categoryId}"
    const val CATEGORY_RESTORE = "$CATEGORY_ID/restore"

    const val TRANSACTIONS = "$API_V1/transactions"
    const val TRANSACTION_UNCATEGORIZED = "/uncategorized"
    const val TRANSACTION_CATEGORY_ASSIGNMENTS = "/category-assignments"
    const val TRANSACTION_ID = "/{transactionId}"

    const val TRANSFERS = "$API_V1/transfers"
    const val TRANSFER_ID = "/{transferId}"
    const val TRANSFER_REVERSAL = "$TRANSFER_ID/reversal"

    const val RECURRING_TRANSACTIONS = "$API_V1/recurring-transactions"
    const val RECURRING_TRANSACTION_ID = "/{recurringTransactionId}"

    const val BUDGETS = "$API_V1/budgets"
    const val BUDGET_MONTH = "/{budgetMonth}"
    const val BUDGET_OPTIMIZATIONS = "$BUDGET_MONTH/optimizations"
    const val BUDGET_OPTIMIZATIONS_LATEST = "$BUDGET_OPTIMIZATIONS/latest"
    const val BUDGET_OPTIMIZATION_ID = "$BUDGET_OPTIMIZATIONS/{optimizationId}"
    const val BUDGET_OPTIMIZATION_APPLY = "$BUDGET_OPTIMIZATION_ID/apply"
    const val BUDGET_OPTIMIZATION_DISMISS = "$BUDGET_OPTIMIZATION_ID/dismiss"
}

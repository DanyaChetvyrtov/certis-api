package ru.digitalhustle.certis.features.transaction.query.specification

import org.jooq.Condition
import org.jooq.generated.Tables
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import java.time.OffsetDateTime
import java.util.UUID

fun interface TransactionSpecification {

    fun toCondition(): Condition

    infix fun and(other: TransactionSpecification): TransactionSpecification =
        TransactionSpecification {
            toCondition().and(other.toCondition())
        }

    infix fun or(other: TransactionSpecification): TransactionSpecification =
        TransactionSpecification {
            toCondition().or(other.toCondition())
        }

    operator fun not(): TransactionSpecification =
        TransactionSpecification {
            toCondition().not()
        }
}

object TransactionSpecifications {

    fun hasId(id: UUID): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.ID.eq(id)
        }

    fun ownedBy(userId: UUID): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.USER_ID.eq(userId)
        }

    fun active(): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.DELETED_AT.isNull()
        }

    fun notTransfer(): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.TRANSFER_ID.isNull()
        }

    fun uncategorized(): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.CATEGORY_ID.isNull()
        }

    fun withoutRecurringTemplate(): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.RECURRING_TRANSACTION_TEMPLATE_ID.isNull()
        }

    fun fromAccount(accountId: UUID): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.ACCOUNT_ID.eq(accountId)
        }

    fun inCategory(categoryId: UUID): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.CATEGORY_ID.eq(categoryId)
        }

    fun hasType(type: TransactionType): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.TYPE.eq(type.name)
        }

    fun occurredAtOrAfter(from: OffsetDateTime): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.OCCURRED_AT.ge(from)
        }

    fun occurredAtOrBefore(to: OffsetDateTime): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.OCCURRED_AT.le(to)
        }

    fun occurredBefore(toExclusive: OffsetDateTime): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.OCCURRED_AT.lt(toExclusive)
        }

    fun matchesSearchText(search: String): TransactionSpecification =
        TransactionSpecification {
            Tables.TRANSACTIONS.MERCHANT.containsIgnoreCase(search)
                .or(Tables.TRANSACTIONS.NOTE.containsIgnoreCase(search))
        }
}

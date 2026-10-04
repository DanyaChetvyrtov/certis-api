package ru.digitalhustle.certis.units.specification

import org.assertj.core.api.Assertions.assertThat
import org.jooq.generated.Tables
import org.junit.jupiter.api.Test
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.query.specification.TransactionSpecification
import ru.digitalhustle.certis.features.transaction.query.specification.TransactionSpecifications
import java.time.OffsetDateTime
import java.util.UUID

class TransactionSpecificationTest {

    @Test
    fun `should compose transaction specifications with and`() {
        // given
        val userId = UUID.randomUUID()
        val from = OffsetDateTime.parse("2026-09-01T00:00:00Z")
        val toExclusive = OffsetDateTime.parse("2026-10-01T00:00:00Z")

        // when
        val condition = (
            TransactionSpecifications.ownedBy(userId) and
                TransactionSpecifications.active() and
                TransactionSpecifications.notTransfer() and
                TransactionSpecifications.hasType(TransactionType.EXPENSE) and
                TransactionSpecifications.occurredAtOrAfter(from) and
                TransactionSpecifications.occurredBefore(toExclusive)
            ).toCondition()

        // then
        val expected =
            Tables.TRANSACTIONS.USER_ID.eq(userId)
                .and(Tables.TRANSACTIONS.DELETED_AT.isNull())
                .and(Tables.TRANSACTIONS.TRANSFER_ID.isNull())
                .and(Tables.TRANSACTIONS.TYPE.eq(TransactionType.EXPENSE.name))
                .and(Tables.TRANSACTIONS.OCCURRED_AT.ge(from))
                .and(Tables.TRANSACTIONS.OCCURRED_AT.lt(toExclusive))

        assertThat(condition).isEqualTo(expected)
    }

    @Test
    fun `should compose transaction specifications with or and not`() {
        // given
        val accountId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        // when
        val condition = (
            TransactionSpecifications.fromAccount(accountId) or
                !TransactionSpecifications.inCategory(categoryId)
            ).toCondition()

        // then
        val expected =
            Tables.TRANSACTIONS.ACCOUNT_ID.eq(accountId)
                .or(Tables.TRANSACTIONS.CATEGORY_ID.eq(categoryId).not())

        assertThat(condition).isEqualTo(expected)
    }

    @Test
    fun `should allow custom transaction specification composition`() {
        // given
        val custom = TransactionSpecification {
            Tables.TRANSACTIONS.MERCHANT.isNotNull()
        }

        // when
        val condition = (
            TransactionSpecifications.active() and custom
            ).toCondition()

        // then
        val expected =
            Tables.TRANSACTIONS.DELETED_AT.isNull()
                .and(Tables.TRANSACTIONS.MERCHANT.isNotNull())

        assertThat(condition).isEqualTo(expected)
    }
}

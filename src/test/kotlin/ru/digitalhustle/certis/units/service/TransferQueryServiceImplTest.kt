package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.command.model.NewTransfer
import ru.digitalhustle.certis.features.transaction.model.Transfer
import ru.digitalhustle.certis.features.transaction.query.repository.TransferQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.impl.TransferQueryServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class TransferQueryServiceImplTest {

    private val transferRepository = mock(TransferQueryRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC)
    private val transferService = TransferQueryServiceImpl(transferRepository)

    @Test
    fun `should get transfer owned by user`() {
        // given
        val transfer = createTransfer()
        `when`(transferRepository.findByIdAndUserId(transfer.id, transfer.userId))
            .thenReturn(transfer)

        // when
        val result = transferService.getById(transfer.id, transfer.userId)

        // then
        assertThat(result).isEqualTo(transfer)
    }

    @Test
    fun `should reject missing transfer`() {
        // given
        val transferId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        // when, then
        assertThatThrownBy {
            transferService.getById(transferId, userId)
        }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Transfer not found")
    }

    @Test
    fun `should list transfers scoped to user in repository order`() {
        // given
        val userId = UUID.randomUUID()
        val transfers = listOf(createTransfer(), createTransfer())
        `when`(transferRepository.findAllByUserId(userId)).thenReturn(transfers)

        // when
        val result = transferService.getAllByUserId(userId)

        // then
        assertThat(result).containsExactlyElementsOf(transfers)
    }

    private fun createNewTransfer(reversalOfTransferId: UUID? = null): NewTransfer =
        NewTransfer(
            userId = UUID.randomUUID(),
            sourceAccountId = UUID.randomUUID(),
            destinationAccountId = UUID.randomUUID(),
            currency = Currency.EUR,
            amount = BigDecimal("25.50"),
            note = "Move to savings",
            occurredAt = OffsetDateTime.parse("2026-08-16T10:30:00Z"),
            reversalOfTransferId = reversalOfTransferId,
        )

    private fun createTransfer(): Transfer {
        val newTransfer = createNewTransfer()

        return Transfer(
            id = UUID.randomUUID(),
            userId = newTransfer.userId,
            sourceAccountId = newTransfer.sourceAccountId,
            destinationAccountId = newTransfer.destinationAccountId,
            currency = newTransfer.currency,
            amount = newTransfer.amount,
            note = newTransfer.note,
            occurredAt = newTransfer.occurredAt,
            createdAt = OffsetDateTime.now(clock),
        )
    }
}

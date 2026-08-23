package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.transfer.NewTransfer
import ru.digitalhustle.certis.repository.TransferRepository
import ru.digitalhustle.certis.service.domain.impl.TransferServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class TransferServiceImplTest {

    private val transferRepository = mock(TransferRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC)
    private val transferService = TransferServiceImpl(transferRepository, clock)

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
    fun `should get transfer for update`() {
        // given
        val transfer = createTransfer()
        `when`(transferRepository.findByIdAndUserIdForUpdate(transfer.id, transfer.userId))
            .thenReturn(transfer)

        // when
        val result = transferService.getByIdForUpdate(transfer.id, transfer.userId)

        // then
        assertThat(result).isEqualTo(transfer)
    }

    @Test
    fun `should reject missing transfer for update`() {
        // given
        val transferId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        // when, then
        assertThatThrownBy {
            transferService.getByIdForUpdate(transferId, userId)
        }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Transfer not found")
    }

    @Test
    fun `should find reversal`() {
        // given
        val original = createTransfer()
        val reversal = createTransfer().copy(reversalOfTransferId = original.id)
        `when`(transferRepository.findByReversalOfTransferIdAndUserId(original.id, original.userId))
            .thenReturn(reversal)

        // when
        val result = transferService.findReversal(original.id, original.userId)

        // then
        assertThat(result).isEqualTo(reversal)
    }

    @Test
    fun `should save transfer with generated identity and current time`() {
        // given
        val newTransfer = createNewTransfer(reversalOfTransferId = UUID.randomUUID())
        val captor = ArgumentCaptor.forClass(Transfer::class.java)
        `when`(transferRepository.insert(captureTransfer(captor)))
            .thenAnswer { invocation -> invocation.getArgument(0) }

        // when
        val result = transferService.save(newTransfer)

        // then
        assertThat(result.id).isNotNull()
        assertThat(result.userId).isEqualTo(newTransfer.userId)
        assertThat(result.sourceAccountId).isEqualTo(newTransfer.sourceAccountId)
        assertThat(result.destinationAccountId).isEqualTo(newTransfer.destinationAccountId)
        assertThat(result.currency).isEqualTo(newTransfer.currency)
        assertThat(result.amount).isEqualByComparingTo(newTransfer.amount)
        assertThat(result.note).isEqualTo(newTransfer.note)
        assertThat(result.occurredAt).isEqualTo(newTransfer.occurredAt)
        assertThat(result.createdAt).isEqualTo(OffsetDateTime.now(clock))
        assertThat(result.reversalOfTransferId).isEqualTo(newTransfer.reversalOfTransferId)
        verify(transferRepository).insert(result)
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

    private fun captureTransfer(captor: ArgumentCaptor<Transfer>): Transfer {
        captor.capture()
        return createTransfer()
    }
}

package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.query.repository.RecurringTransactionTemplateQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.impl.RecurringTransactionQueryServiceImpl
import java.util.UUID

class RecurringTransactionQueryServiceImplTest {

    private val repository = mock(RecurringTransactionTemplateQueryRepository::class.java)
    private val service = RecurringTransactionQueryServiceImpl(repository)

    @Test
    fun `should find recurring template scoped to user`() {
        // given
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val template = mock(RecurringTransactionTemplate::class.java)
        `when`(repository.findByIdAndUserId(id, userId)).thenReturn(template)

        // when
        val result = service.getById(id, userId)

        // then
        assertThat(result).isSameAs(template)
    }

    @Test
    fun `should preserve not found response for inaccessible template`() {
        // given
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()

        // when, then
        assertThatThrownBy { service.getById(id, userId) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Recurring transaction not found")
    }

    @Test
    fun `should preserve repository ordering when listing templates`() {
        // given
        val userId = UUID.randomUUID()
        val templates = listOf(
            mock(RecurringTransactionTemplate::class.java),
            mock(RecurringTransactionTemplate::class.java),
        )
        `when`(repository.findAllByUserId(userId)).thenReturn(templates)

        // when
        val result = service.getAllByUserId(userId)

        // then
        assertThat(result).containsExactlyElementsOf(templates)
    }

    @Test
    fun `should expose owner scoped account usage without command dependency`() {
        // given
        val accountId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        `when`(repository.existsSchedulableByAccountIdAndUserId(accountId, ownerId)).thenReturn(true)

        // when, then
        assertThat(service.existsSchedulableByAccountId(accountId, ownerId)).isTrue()
        assertThat(service.existsSchedulableByAccountId(accountId, otherUserId)).isFalse()
    }
}

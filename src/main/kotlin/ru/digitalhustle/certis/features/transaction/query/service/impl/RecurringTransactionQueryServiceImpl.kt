package ru.digitalhustle.certis.features.transaction.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.api.RecurringTransactionUsage
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.query.repository.RecurringTransactionTemplateQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.RecurringTransactionQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RecurringTransactionQueryServiceImpl(
    private val recurringTransactionTemplateRepository: RecurringTransactionTemplateQueryRepository,
) : RecurringTransactionQueryService,
    RecurringTransactionUsage {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Recurring transaction")

    override fun getAllByUserId(userId: UUID): List<RecurringTransactionTemplate> =
        recurringTransactionTemplateRepository.findAllByUserId(userId)

    override fun existsSchedulableByAccountId(
        accountId: UUID,
        userId: UUID,
    ): Boolean = recurringTransactionTemplateRepository.existsSchedulableByAccountIdAndUserId(accountId, userId)
}

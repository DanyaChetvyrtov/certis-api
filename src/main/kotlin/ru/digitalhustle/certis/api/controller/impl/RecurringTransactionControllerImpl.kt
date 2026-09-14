package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.RecurringTransactionController
import ru.digitalhustle.certis.api.dto.RecurringTransactionDto
import ru.digitalhustle.certis.api.dto.request.CreateRecurringTransactionRq
import ru.digitalhustle.certis.api.dto.request.UpdateRecurringTransactionRq
import ru.digitalhustle.certis.api.dto.response.RecurringTransactionsRs
import ru.digitalhustle.certis.api.mapper.RecurringTransactionMapper
import ru.digitalhustle.certis.features.security.model.JwtDetails
import ru.digitalhustle.certis.features.transaction.command.service.RecurringTransactionCommandService
import ru.digitalhustle.certis.features.transaction.query.service.RecurringTransactionQueryService
import java.util.UUID

@RestController
class RecurringTransactionControllerImpl(
    private val recurringTransactionQueryService: RecurringTransactionQueryService,
    private val recurringTransactionCommandService: RecurringTransactionCommandService,
    private val recurringTransactionMapper: RecurringTransactionMapper,
) : RecurringTransactionController {

    override fun getRecurringTransactions(jwtDetails: JwtDetails): RecurringTransactionsRs =
        RecurringTransactionsRs(
            recurringTransactionQueryService.getAllByUserId(jwtDetails.id)
                .map(recurringTransactionMapper::convert),
        )

    override fun getRecurringTransactionById(
        recurringTransactionId: UUID,
        jwtDetails: JwtDetails,
    ): RecurringTransactionDto =
        recurringTransactionMapper.convert(
            recurringTransactionQueryService.getById(recurringTransactionId, jwtDetails.id),
        )

    override fun createRecurringTransaction(
        createRecurringTransactionRq: CreateRecurringTransactionRq,
        jwtDetails: JwtDetails,
    ): RecurringTransactionDto =
        recurringTransactionMapper.convert(
            recurringTransactionCommandService.save(
                recurringTransactionMapper.convert(createRecurringTransactionRq, jwtDetails.id),
            ),
        )

    override fun updateRecurringTransaction(
        recurringTransactionId: UUID,
        updateRecurringTransactionRq: UpdateRecurringTransactionRq,
        jwtDetails: JwtDetails,
    ): RecurringTransactionDto =
        recurringTransactionMapper.convert(
            recurringTransactionCommandService.update(
                recurringTransactionMapper.convert(
                    updateRecurringTransactionRq,
                    recurringTransactionId,
                    jwtDetails.id,
                ),
            ),
        )

    override fun cancelRecurringTransaction(
        recurringTransactionId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = recurringTransactionCommandService.cancel(recurringTransactionId, jwtDetails.id)
}

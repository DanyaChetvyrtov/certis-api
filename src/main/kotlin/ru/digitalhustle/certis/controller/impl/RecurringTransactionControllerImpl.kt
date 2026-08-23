package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.RecurringTransactionController
import ru.digitalhustle.certis.dto.RecurringTransactionDto
import ru.digitalhustle.certis.dto.request.CreateRecurringTransactionRq
import ru.digitalhustle.certis.dto.request.UpdateRecurringTransactionRq
import ru.digitalhustle.certis.mapper.RecurringTransactionMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.transaction.RecurringTransactionAggregator
import java.util.UUID

@RestController
class RecurringTransactionControllerImpl(
    private val recurringTransactionAggregator: RecurringTransactionAggregator,
    private val recurringTransactionMapper: RecurringTransactionMapper,
) : RecurringTransactionController {

    override fun getRecurringTransactions(jwtDetails: JwtDetails): List<RecurringTransactionDto> =
        recurringTransactionAggregator.getAllByUserId(jwtDetails.id)
            .map(recurringTransactionMapper::convert)

    override fun getRecurringTransactionById(
        recurringTransactionId: UUID,
        jwtDetails: JwtDetails,
    ): RecurringTransactionDto =
        recurringTransactionMapper.convert(
            recurringTransactionAggregator.getById(recurringTransactionId, jwtDetails.id),
        )

    override fun createRecurringTransaction(
        createRecurringTransactionRq: CreateRecurringTransactionRq,
        jwtDetails: JwtDetails,
    ): RecurringTransactionDto =
        recurringTransactionMapper.convert(
            recurringTransactionAggregator.save(
                recurringTransactionMapper.convert(createRecurringTransactionRq, jwtDetails.id),
            ),
        )

    override fun updateRecurringTransaction(
        recurringTransactionId: UUID,
        updateRecurringTransactionRq: UpdateRecurringTransactionRq,
        jwtDetails: JwtDetails,
    ): RecurringTransactionDto =
        recurringTransactionMapper.convert(
            recurringTransactionAggregator.update(
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
    ): Unit = recurringTransactionAggregator.cancel(recurringTransactionId, jwtDetails.id)
}

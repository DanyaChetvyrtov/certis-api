package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.api.dto.RecurringTransactionDto
import ru.digitalhustle.certis.api.dto.request.CreateRecurringTransactionRq
import ru.digitalhustle.certis.api.dto.request.UpdateRecurringTransactionRq
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.transaction.command.model.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.command.model.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface RecurringTransactionMapper {

    fun convert(source: CreateRecurringTransactionRq, userId: UUID): NewRecurringTransactionTemplate

    fun convert(
        source: UpdateRecurringTransactionRq,
        id: UUID,
        userId: UUID,
    ): UpdateRecurringTransactionTemplateData

    fun convert(source: RecurringTransactionTemplate): RecurringTransactionDto
}

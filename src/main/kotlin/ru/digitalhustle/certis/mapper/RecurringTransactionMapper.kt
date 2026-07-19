package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.RecurringTransactionDto
import ru.digitalhustle.certis.dto.request.CreateRecurringTransactionRq
import ru.digitalhustle.certis.dto.request.UpdateRecurringTransactionRq
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.UpdateRecurringTransactionTemplateData
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

package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.TransactionDto
import ru.digitalhustle.certis.api.dto.request.CreateTransactionRq
import ru.digitalhustle.certis.api.dto.request.TransactionFilterRq
import ru.digitalhustle.certis.api.dto.request.UpdateTransactionRq
import ru.digitalhustle.certis.api.dto.response.TransactionPageRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.transaction.command.model.NewTransaction
import ru.digitalhustle.certis.features.transaction.command.model.UpdateTransactionData
import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.features.transaction.query.model.TransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.TransactionPage
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface TransactionMapper {

    @Mapping(target = "transferId", ignore = true)
    fun convert(source: CreateTransactionRq, userId: UUID): NewTransaction

    fun convert(source: TransactionFilterRq): TransactionFilter

    fun convert(source: UpdateTransactionRq, id: UUID, userId: UUID): UpdateTransactionData

    fun convert(source: Transaction): TransactionDto

    fun convert(source: TransactionPage): TransactionPageRs
}

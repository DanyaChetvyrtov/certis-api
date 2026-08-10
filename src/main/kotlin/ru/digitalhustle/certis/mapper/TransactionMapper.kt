package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.TransactionDto
import ru.digitalhustle.certis.dto.request.CreateTransactionRq
import ru.digitalhustle.certis.dto.request.TransactionFilterRq
import ru.digitalhustle.certis.dto.request.UpdateTransactionRq
import ru.digitalhustle.certis.dto.response.TransactionPageRs
import ru.digitalhustle.certis.model.NewTransaction
import ru.digitalhustle.certis.model.TransactionFilter
import ru.digitalhustle.certis.model.TransactionPage
import ru.digitalhustle.certis.model.UpdateTransactionData
import ru.digitalhustle.certis.model.entity.Transaction
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface TransactionMapper {

    fun convert(source: CreateTransactionRq, userId: UUID): NewTransaction

    fun convert(source: TransactionFilterRq): TransactionFilter

    fun convert(source: UpdateTransactionRq, id: UUID, userId: UUID): UpdateTransactionData

    fun convert(source: Transaction): TransactionDto

    fun convert(source: TransactionPage): TransactionPageRs
}

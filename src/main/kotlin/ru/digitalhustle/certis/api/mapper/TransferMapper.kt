package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.api.dto.TransferDto
import ru.digitalhustle.certis.api.dto.request.CreateTransferRq
import ru.digitalhustle.certis.api.dto.request.ReverseTransferRq
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.transaction.command.model.CreateTransferData
import ru.digitalhustle.certis.features.transaction.command.model.ReverseTransferData
import ru.digitalhustle.certis.features.transaction.model.Transfer
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface TransferMapper {

    fun convert(source: CreateTransferRq, userId: UUID): CreateTransferData

    fun convert(
        source: ReverseTransferRq,
        transferId: UUID,
        userId: UUID,
    ): ReverseTransferData

    fun convert(source: Transfer): TransferDto

    fun convert(source: List<Transfer>): List<TransferDto>
}

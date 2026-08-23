package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.TransferDto
import ru.digitalhustle.certis.dto.request.CreateTransferRq
import ru.digitalhustle.certis.dto.request.ReverseTransferRq
import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.transfer.CreateTransferData
import ru.digitalhustle.certis.model.transfer.ReverseTransferData
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

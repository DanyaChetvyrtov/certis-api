package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.TransferController
import ru.digitalhustle.certis.dto.TransferDto
import ru.digitalhustle.certis.dto.request.CreateTransferRq
import ru.digitalhustle.certis.dto.request.ReverseTransferRq
import ru.digitalhustle.certis.mapper.TransferMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.transaction.TransferAggregator
import java.util.UUID

@RestController
class TransferControllerImpl(
    private val transferAggregator: TransferAggregator,
    private val transferMapper: TransferMapper,
) : TransferController {

    override fun getTransferById(
        transferId: UUID,
        jwtDetails: JwtDetails,
    ): TransferDto =
        transferMapper.convert(
            transferAggregator.getById(transferId, jwtDetails.id),
        )

    override fun getTransfers(jwtDetails: JwtDetails): List<TransferDto> =
        transferMapper.convert(transferAggregator.getAllByUserId(jwtDetails.id))

    override fun createTransfer(
        createTransferRq: CreateTransferRq,
        jwtDetails: JwtDetails,
    ): TransferDto =
        transferMapper.convert(
            transferAggregator.save(
                transferMapper.convert(createTransferRq, jwtDetails.id),
            ),
        )

    override fun reverseTransfer(
        transferId: UUID,
        reverseTransferRq: ReverseTransferRq,
        jwtDetails: JwtDetails,
    ): TransferDto =
        transferMapper.convert(
            transferAggregator.reverse(
                transferMapper.convert(reverseTransferRq, transferId, jwtDetails.id),
            ),
        )
}

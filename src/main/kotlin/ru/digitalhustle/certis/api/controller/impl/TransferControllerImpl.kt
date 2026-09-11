package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.TransferController
import ru.digitalhustle.certis.api.dto.TransferDto
import ru.digitalhustle.certis.api.dto.request.CreateTransferRq
import ru.digitalhustle.certis.api.dto.request.ReverseTransferRq
import ru.digitalhustle.certis.api.dto.response.TransfersRs
import ru.digitalhustle.certis.api.mapper.TransferMapper
import ru.digitalhustle.certis.features.security.model.JwtDetails
import ru.digitalhustle.certis.features.transaction.command.service.TransferCommandService
import ru.digitalhustle.certis.features.transaction.query.service.TransferQueryService
import java.util.UUID

@RestController
class TransferControllerImpl(
    private val transferQueryService: TransferQueryService,
    private val transferCommandService: TransferCommandService,
    private val transferMapper: TransferMapper,
) : TransferController {

    override fun getTransferById(
        transferId: UUID,
        jwtDetails: JwtDetails,
    ): TransferDto =
        transferMapper.convert(
            transferQueryService.getById(transferId, jwtDetails.id),
        )

    override fun getTransfers(jwtDetails: JwtDetails): TransfersRs =
        TransfersRs(
            transferMapper.convert(transferQueryService.getAllByUserId(jwtDetails.id)),
        )

    override fun createTransfer(
        createTransferRq: CreateTransferRq,
        jwtDetails: JwtDetails,
    ): TransferDto =
        transferMapper.convert(
            transferCommandService.save(
                transferMapper.convert(createTransferRq, jwtDetails.id),
            ),
        )

    override fun reverseTransfer(
        transferId: UUID,
        reverseTransferRq: ReverseTransferRq,
        jwtDetails: JwtDetails,
    ): TransferDto =
        transferMapper.convert(
            transferCommandService.reverse(
                transferMapper.convert(reverseTransferRq, transferId, jwtDetails.id),
            ),
        )
}

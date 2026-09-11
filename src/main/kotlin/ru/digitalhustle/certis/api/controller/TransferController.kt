package ru.digitalhustle.certis.api.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.api.dto.TransferDto
import ru.digitalhustle.certis.api.dto.request.CreateTransferRq
import ru.digitalhustle.certis.api.dto.request.ReverseTransferRq
import ru.digitalhustle.certis.api.dto.response.TransfersRs
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RequestMapping(PathConstants.TRANSFERS)
interface TransferController {

    @GetMapping(PathConstants.TRANSFER_ID)
    fun getTransferById(
        @PathVariable transferId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransferDto

    @GetMapping
    fun getTransfers(
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransfersRs

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTransfer(
        @RequestBody @Valid createTransferRq: CreateTransferRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransferDto

    @PostMapping(PathConstants.TRANSFER_REVERSAL)
    @ResponseStatus(HttpStatus.CREATED)
    fun reverseTransfer(
        @PathVariable transferId: UUID,
        @RequestBody @Valid reverseTransferRq: ReverseTransferRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransferDto
}

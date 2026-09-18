package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.TransferDto

data class TransfersRs(

    val transfers: List<TransferDto>,
)

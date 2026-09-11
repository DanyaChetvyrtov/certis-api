package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.api.dto.AccountShortInfoDto
import ru.digitalhustle.certis.api.dto.UncategorizedTransactionDto
import ru.digitalhustle.certis.api.dto.request.AssignTransactionsCategoryRq
import ru.digitalhustle.certis.api.dto.request.TransactionCategoryAssignmentRq
import ru.digitalhustle.certis.api.dto.request.UncategorizedTransactionFilterRq
import ru.digitalhustle.certis.api.dto.response.UncategorizedTransactionPageRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.transaction.command.model.AssignTransactionsCategory
import ru.digitalhustle.certis.features.transaction.command.model.TransactionCategoryAssignment
import ru.digitalhustle.certis.features.transaction.query.model.TransactionAccountView
import ru.digitalhustle.certis.features.transaction.query.model.UncategorizedTransaction
import ru.digitalhustle.certis.features.transaction.query.model.UncategorizedTransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.UncategorizedTransactionPage
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface TransactionCategorizationMapper {

    fun convert(source: UncategorizedTransactionFilterRq): UncategorizedTransactionFilter

    fun convert(source: AssignTransactionsCategoryRq, userId: UUID): AssignTransactionsCategory

    fun convert(source: TransactionCategoryAssignmentRq): TransactionCategoryAssignment

    fun convert(source: TransactionAccountView): AccountShortInfoDto

    fun convert(source: UncategorizedTransaction): UncategorizedTransactionDto

    fun convert(source: UncategorizedTransactionPage): UncategorizedTransactionPageRs
}

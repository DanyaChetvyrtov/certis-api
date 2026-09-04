package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.AccountShortInfoDto
import ru.digitalhustle.certis.dto.UncategorizedTransactionDto
import ru.digitalhustle.certis.dto.request.AssignTransactionsCategoryRq
import ru.digitalhustle.certis.dto.request.TransactionCategoryAssignmentRq
import ru.digitalhustle.certis.dto.request.UncategorizedTransactionFilterRq
import ru.digitalhustle.certis.dto.response.UncategorizedTransactionPageRs
import ru.digitalhustle.certis.model.account.AccountShortInfo
import ru.digitalhustle.certis.model.transaction.AssignTransactionsCategory
import ru.digitalhustle.certis.model.transaction.TransactionCategoryAssignment
import ru.digitalhustle.certis.model.transaction.UncategorizedTransaction
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface TransactionCategorizationMapper {

    fun convert(source: UncategorizedTransactionFilterRq): UncategorizedTransactionFilter

    fun convert(source: AssignTransactionsCategoryRq, userId: UUID): AssignTransactionsCategory

    fun convert(source: TransactionCategoryAssignmentRq): TransactionCategoryAssignment

    fun convert(source: AccountShortInfo): AccountShortInfoDto

    fun convert(source: UncategorizedTransaction): UncategorizedTransactionDto

    fun convert(source: UncategorizedTransactionPage): UncategorizedTransactionPageRs
}

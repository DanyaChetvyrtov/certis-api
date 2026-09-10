package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CategoryCardsRs(

    val month: String,

    val currency: Currency,

    val categories: List<CategoryCardDto>,

    val page: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,
)

data class CategoryCardDto(

    val id: UUID,

    val name: String,

    val type: CategoryType,

    val icon: String,

    val color: String,

    val archivedAt: OffsetDateTime?,

    val monthlyTransactionCount: Int,

    val monthlyAmount: BigDecimal,

    val monthlySharePercentage: BigDecimal,
)

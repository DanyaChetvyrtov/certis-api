package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.request.SaveBudgetAllocationRq
import ru.digitalhustle.certis.dto.request.SaveBudgetRq
import ru.digitalhustle.certis.dto.response.BudgetAllocationRs
import ru.digitalhustle.certis.dto.response.BudgetRs
import ru.digitalhustle.certis.model.budget.BudgetAllocationDetails
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.SaveBudgetAllocationData
import ru.digitalhustle.certis.model.budget.SaveBudgetData
import java.time.YearMonth
import java.util.UUID

@Mapper(
    config = BaseMapperConfig::class,
    imports = [YearMonth::class],
)
interface BudgetMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "budgetMonth", expression = "java(budgetMonth.atDay(1))")
    @Mapping(target = "plannedIncome", source = "source.monthlyIncome")
    @Mapping(target = "savingsTarget", source = "source.savingsTarget")
    @Mapping(target = "allocations", source = "source.allocations")
    fun convert(
        source: SaveBudgetRq,
        userId: UUID,
        budgetMonth: YearMonth,
    ): SaveBudgetData

    @Mapping(target = "expenseType", source = "type")
    @Mapping(target = "limitAmount", source = "limit")
    fun convert(source: SaveBudgetAllocationRq): SaveBudgetAllocationData

    @Mapping(target = "month", expression = "java(YearMonth.from(source.getBudgetMonth()).toString())")
    @Mapping(target = "monthlyIncome", source = "plannedIncome")
    fun convert(source: BudgetDetails): BudgetRs

    @Mapping(target = "type", source = "expenseType")
    @Mapping(target = "limit", source = "limitAmount")
    @Mapping(target = "spent", source = "spentAmount")
    fun convert(source: BudgetAllocationDetails): BudgetAllocationRs
}

package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.BudgetOptimizationController
import ru.digitalhustle.certis.dto.response.BudgetOptimizationRs
import ru.digitalhustle.certis.dto.response.BudgetRs
import ru.digitalhustle.certis.mapper.BudgetMapper
import ru.digitalhustle.certis.mapper.BudgetOptimizationMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.budget.BudgetOptimizationAggregator
import java.time.YearMonth
import java.util.UUID

@RestController
class BudgetOptimizationControllerImpl(
    private val budgetOptimizationAggregator: BudgetOptimizationAggregator,
    private val budgetOptimizationMapper: BudgetOptimizationMapper,
    private val budgetMapper: BudgetMapper,
) : BudgetOptimizationController {

    override fun getLatestOptimization(
        budgetMonth: YearMonth,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationRs =
        budgetOptimizationMapper.convert(
            budgetOptimizationAggregator.getLatest(jwtDetails.id, budgetMonth.atDay(1)),
        )

    override fun generateOptimization(
        budgetMonth: YearMonth,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationRs =
        budgetOptimizationMapper.convert(
            budgetOptimizationAggregator.generate(jwtDetails.id, budgetMonth.atDay(1)),
        )

    override fun applyOptimization(
        budgetMonth: YearMonth,
        optimizationId: UUID,
        jwtDetails: JwtDetails,
    ): BudgetRs =
        budgetMapper.convert(
            budgetOptimizationAggregator.apply(
                id = optimizationId,
                userId = jwtDetails.id,
                budgetMonth = budgetMonth.atDay(1),
            ),
        )

    override fun dismissOptimization(
        budgetMonth: YearMonth,
        optimizationId: UUID,
        jwtDetails: JwtDetails,
    ) {
        budgetOptimizationAggregator.dismiss(
            id = optimizationId,
            userId = jwtDetails.id,
            budgetMonth = budgetMonth.atDay(1),
        )
    }
}

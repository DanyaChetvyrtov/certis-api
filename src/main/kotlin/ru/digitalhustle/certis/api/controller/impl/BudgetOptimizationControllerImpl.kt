package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetOptimizationController
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationRs
import ru.digitalhustle.certis.api.dto.response.BudgetRs
import ru.digitalhustle.certis.api.mapper.BudgetMapper
import ru.digitalhustle.certis.api.mapper.BudgetOptimizationMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetOptimizationApplicationService
import ru.digitalhustle.certis.features.budget.query.service.BudgetOptimizationQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.time.YearMonth
import java.util.UUID

@RestController
class BudgetOptimizationControllerImpl(
    private val budgetOptimizationQueryService: BudgetOptimizationQueryService,
    private val budgetOptimizationApplicationService: BudgetOptimizationApplicationService,
    private val budgetOptimizationMapper: BudgetOptimizationMapper,
    private val budgetMapper: BudgetMapper,
) : BudgetOptimizationController {

    override fun getLatestOptimization(
        budgetMonth: YearMonth,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationRs =
        budgetOptimizationMapper.convert(
            budgetOptimizationQueryService.getLatest(jwtDetails.id, budgetMonth.atDay(1)),
        )

    override fun generateOptimization(
        budgetMonth: YearMonth,
        jwtDetails: JwtDetails,
    ): BudgetOptimizationRs =
        budgetOptimizationMapper.convert(
            budgetOptimizationApplicationService.generate(jwtDetails.id, budgetMonth.atDay(1)),
        )

    override fun applyOptimization(
        budgetMonth: YearMonth,
        optimizationId: UUID,
        jwtDetails: JwtDetails,
    ): BudgetRs =
        budgetMapper.convert(
            budgetOptimizationApplicationService.apply(
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
        budgetOptimizationApplicationService.dismiss(
            id = optimizationId,
            userId = jwtDetails.id,
            budgetMonth = budgetMonth.atDay(1),
        )
    }
}

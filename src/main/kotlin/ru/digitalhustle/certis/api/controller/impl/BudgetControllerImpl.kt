package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetController
import ru.digitalhustle.certis.api.dto.request.SaveBudgetRq
import ru.digitalhustle.certis.api.dto.response.BudgetRs
import ru.digitalhustle.certis.api.mapper.BudgetMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetApplicationService
import ru.digitalhustle.certis.features.budget.query.service.BudgetQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.time.YearMonth

@RestController
class BudgetControllerImpl(
    private val budgetQueryService: BudgetQueryService,
    private val budgetService: BudgetApplicationService,
    private val budgetMapper: BudgetMapper,
) : BudgetController {

    override fun getBudget(
        budgetMonth: YearMonth,
        jwtDetails: JwtDetails,
    ): BudgetRs =
        budgetMapper.convert(
            budgetQueryService.getByMonth(
                userId = jwtDetails.id,
                budgetMonth = budgetMonth.atDay(1),
            ),
        )

    override fun saveBudget(
        budgetMonth: YearMonth,
        saveBudgetRq: SaveBudgetRq,
        jwtDetails: JwtDetails,
    ): BudgetRs =
        budgetMapper.convert(
            budgetService.save(
                budgetMapper.convert(
                    source = saveBudgetRq,
                    userId = jwtDetails.id,
                    budgetMonth = budgetMonth,
                ),
            ),
        )
}

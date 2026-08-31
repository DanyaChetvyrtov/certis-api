package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.BudgetController
import ru.digitalhustle.certis.dto.request.SaveBudgetRq
import ru.digitalhustle.certis.dto.response.BudgetRs
import ru.digitalhustle.certis.mapper.BudgetMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.domain.BudgetService
import java.time.YearMonth

@RestController
class BudgetControllerImpl(
    private val budgetService: BudgetService,
    private val budgetMapper: BudgetMapper,
) : BudgetController {

    override fun getBudget(
        budgetMonth: YearMonth,
        jwtDetails: JwtDetails,
    ): BudgetRs =
        budgetMapper.convert(
            budgetService.getByMonth(
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

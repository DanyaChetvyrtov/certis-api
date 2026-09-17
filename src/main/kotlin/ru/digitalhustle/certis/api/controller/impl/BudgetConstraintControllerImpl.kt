package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.BudgetConstraintController
import ru.digitalhustle.certis.api.dto.request.SaveBudgetConstraintsRq
import ru.digitalhustle.certis.api.dto.response.BudgetConstraintSetRs
import ru.digitalhustle.certis.api.mapper.BudgetConstraintMapper
import ru.digitalhustle.certis.features.budget.application.service.BudgetConstraintApplicationService
import ru.digitalhustle.certis.features.budget.query.service.BudgetConstraintQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class BudgetConstraintControllerImpl(
    private val queryService: BudgetConstraintQueryService,
    private val applicationService: BudgetConstraintApplicationService,
    private val mapper: BudgetConstraintMapper,
) : BudgetConstraintController {

    override fun getConstraints(planId: UUID, jwtDetails: JwtDetails): BudgetConstraintSetRs =
        mapper.convert(queryService.getConstraints(planId, jwtDetails.id))

    override fun saveConstraints(
        planId: UUID,
        saveBudgetConstraintsRq: SaveBudgetConstraintsRq,
        jwtDetails: JwtDetails,
    ): BudgetConstraintSetRs =
        mapper.convert(
            applicationService.save(
                mapper.convert(
                    source = saveBudgetConstraintsRq,
                    planId = planId,
                    userId = jwtDetails.id,
                ),
            ),
        )
}

package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.goal.GoalFilter
import ru.digitalhustle.certis.model.goal.GoalPage
import ru.digitalhustle.certis.model.goal.GoalView
import ru.digitalhustle.certis.repository.GoalQueryRepository
import ru.digitalhustle.certis.service.domain.GoalQueryService
import ru.digitalhustle.certis.service.goal.GoalCalculator
import java.util.UUID

@Service
class GoalQueryServiceImpl(
    private val goalQueryRepository: GoalQueryRepository,
    private val goalCalculator: GoalCalculator,
) : GoalQueryService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): GoalView =
        goalQueryRepository.findByIdAndUserId(id, userId)
            ?.let(goalCalculator::toView)
            ?: throw NotFoundException.entity("Goal")

    override fun getPage(
        userId: UUID,
        currency: Currency,
        filter: GoalFilter,
    ): GoalPage =
        GoalPage(
            currency = currency,
            items = goalQueryRepository.findPageByUserId(userId, currency, filter).map(goalCalculator::toView),
            statusCounts = goalQueryRepository.statusCounts(userId, currency),
            page = filter.page,
            size = filter.size,
            totalElements = goalQueryRepository.countByUserIdAndCurrencyAndStatus(userId, currency, filter.status),
        )

    override fun getAllActive(
        userId: UUID,
        currency: Currency,
    ): List<GoalView> =
        goalQueryRepository.findAllActiveByUserIdAndCurrency(userId, currency).map(goalCalculator::toView)

    override fun getAll(
        userId: UUID,
        currency: Currency,
    ): List<GoalView> =
        goalQueryRepository.findAllByUserIdAndCurrency(userId, currency).map(goalCalculator::toView)
}

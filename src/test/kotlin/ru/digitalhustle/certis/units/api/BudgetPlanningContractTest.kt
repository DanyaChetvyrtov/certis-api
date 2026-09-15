package ru.digitalhustle.certis.units.api

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.api.constants.PathConstants
import ru.digitalhustle.certis.api.controller.BudgetConstraintController
import ru.digitalhustle.certis.api.controller.BudgetForecastController
import ru.digitalhustle.certis.api.controller.BudgetPlanningController
import ru.digitalhustle.certis.api.controller.BudgetPlanningOptimizationController
import ru.digitalhustle.certis.api.dto.BudgetForecastOperationType
import ru.digitalhustle.certis.api.dto.request.BudgetForecastManualAdjustmentRq
import ru.digitalhustle.certis.api.dto.request.ConfirmBudgetForecastRq
import java.math.BigDecimal
import java.util.UUID
import kotlin.reflect.KClass

class BudgetPlanningContractTest {

    @Test
    fun `should use noun resources for planning state transitions`() {
        assertThat(PathConstants.BUDGET_PLAN_CANCELLATION)
            .isEqualTo("/{planId}/cancellation")
        assertThat(PathConstants.BUDGET_PLAN_OPTIMIZATION_DISMISSAL)
            .isEqualTo("/{planId}/optimizations/{optimizationId}/dismissal")
        assertThat(PathConstants.BUDGET_PLAN_OPTIMIZATION_BUDGET_APPLICATION)
            .isEqualTo("/{planId}/optimizations/{optimizationId}/budget-application")
    }

    @Test
    fun `should expose planning session endpoint contract`() {
        assertBasePath(BudgetPlanningController::class)
        assertGet(BudgetPlanningController::class, "getCurrentPlan", PathConstants.BUDGET_PLAN_CURRENT)
        assertGet(BudgetPlanningController::class, "getPlan", PathConstants.BUDGET_PLAN_ID)
        assertGet(BudgetPlanningController::class, "getPlanRevisions", null)
        assertPost(BudgetPlanningController::class, "createPlan", null)
        assertPut(
            BudgetPlanningController::class,
            "cancelPlan",
            PathConstants.BUDGET_PLAN_CANCELLATION,
        )
        assertCreated(BudgetPlanningController::class, "createPlan")
    }

    @Test
    fun `should expose forecast and constraint endpoint contracts`() {
        assertBasePath(BudgetForecastController::class)
        assertGet(
            BudgetForecastController::class,
            "getForecastPreview",
            PathConstants.BUDGET_PLAN_FORECAST_PREVIEW,
        )
        assertGet(BudgetForecastController::class, "getForecast", PathConstants.BUDGET_PLAN_FORECAST)
        assertPut(BudgetForecastController::class, "confirmForecast", PathConstants.BUDGET_PLAN_FORECAST)

        assertBasePath(BudgetConstraintController::class)
        assertGet(BudgetConstraintController::class, "getConstraints", PathConstants.BUDGET_PLAN_CONSTRAINTS)
        assertPut(BudgetConstraintController::class, "saveConstraints", PathConstants.BUDGET_PLAN_CONSTRAINTS)
    }

    @Test
    fun `should expose optimization endpoint contract`() {
        assertBasePath(BudgetPlanningOptimizationController::class)
        assertGet(
            BudgetPlanningOptimizationController::class,
            "getLatestOptimization",
            PathConstants.BUDGET_PLAN_OPTIMIZATIONS_LATEST,
        )
        assertGet(
            BudgetPlanningOptimizationController::class,
            "getOptimization",
            PathConstants.BUDGET_PLAN_OPTIMIZATION_ID,
        )
        assertPost(
            BudgetPlanningOptimizationController::class,
            "generateOptimization",
            PathConstants.BUDGET_PLAN_OPTIMIZATIONS,
        )
        assertPut(
            BudgetPlanningOptimizationController::class,
            "dismissOptimization",
            PathConstants.BUDGET_PLAN_OPTIMIZATION_DISMISSAL,
        )
        assertPut(
            BudgetPlanningOptimizationController::class,
            "applyOptimization",
            PathConstants.BUDGET_PLAN_OPTIMIZATION_BUDGET_APPLICATION,
        )
        assertCreated(BudgetPlanningOptimizationController::class, "generateOptimization")
    }

    @Test
    fun `should validate version fingerprint and nested monetary input`() {
        val request = ConfirmBudgetForecastRq(
            expectedVersion = -1,
            sourceFingerprint = "stale",
            manualAdjustments = listOf(
                BudgetForecastManualAdjustmentRq(
                    clientId = UUID.randomUUID(),
                    operationType = BudgetForecastOperationType.EXPENSE,
                    title = "Car maintenance",
                    categoryId = UUID.randomUUID(),
                    expectedDate = null,
                    amount = BigDecimal("-1.00"),
                ),
            ),
        )

        Validation.buildDefaultValidatorFactory().use { factory ->
            val invalidProperties = factory.validator.validate(request)
                .map { violation -> violation.propertyPath.toString() }

            assertThat(invalidProperties).contains(
                "expectedVersion",
                "sourceFingerprint",
                "manualAdjustments[0].amount",
            )
        }
    }

    private fun assertBasePath(controller: KClass<*>) {
        val mapping = controller.java.getAnnotation(RequestMapping::class.java)
        assertThat(mapping.value).containsExactly(PathConstants.BUDGET_PLANS)
    }

    private fun assertGet(
        controller: KClass<*>,
        methodName: String,
        path: String?,
    ) {
        val mapping = method(controller, methodName).getAnnotation(GetMapping::class.java)
        assertPath(mapping.value, path)
    }

    private fun assertPost(
        controller: KClass<*>,
        methodName: String,
        path: String?,
    ) {
        val mapping = method(controller, methodName).getAnnotation(PostMapping::class.java)
        assertPath(mapping.value, path)
    }

    private fun assertPut(
        controller: KClass<*>,
        methodName: String,
        path: String,
    ) {
        val mapping = method(controller, methodName).getAnnotation(PutMapping::class.java)
        assertThat(mapping.value).containsExactly(path)
    }

    private fun assertCreated(
        controller: KClass<*>,
        methodName: String,
    ) {
        val status = method(controller, methodName).getAnnotation(ResponseStatus::class.java)
        assertThat(status.value).isEqualTo(HttpStatus.CREATED)
    }

    private fun assertPath(
        paths: Array<String>,
        expectedPath: String?,
    ) {
        if (expectedPath == null) {
            assertThat(paths).isEmpty()
        } else {
            assertThat(paths).containsExactly(expectedPath)
        }
    }

    private fun method(
        controller: KClass<*>,
        methodName: String,
    ) = controller.java.methods.single { method -> method.name == methodName }
}

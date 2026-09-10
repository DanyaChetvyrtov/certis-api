package ru.digitalhustle.certis.units.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

class FeatureArchitectureTest {

    @Test
    fun `queries must not depend on commands or write orchestration`() {
        noClasses().that().resideInAPackage("$FEATURES.*.query..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "$FEATURES.*.command..",
                "$FEATURES.*.application..",
            )
            .check(classes)
    }

    @Test
    fun `commands must not depend on query services or write orchestration`() {
        noClasses().that().resideInAPackage("$FEATURES.*.command..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "$FEATURES.*.query..",
                "$FEATURES.*.application..",
            )
            .check(classes)
    }

    @Test
    fun `features may only use another features public API and value types`() {
        val violations = classes.filter { it.packageName.startsWith("$FEATURES.") }
            .flatMap { it.directDependenciesFromSelf }
            .filter { dependency ->
                val source = dependency.originClass.packageName.removePrefix("$FEATURES.").substringBefore('.')
                val target = dependency.targetClass.packageName
                if (!target.startsWith("$FEATURES.")) {
                    false
                } else {
                    val targetParts = target.removePrefix("$FEATURES.").split('.')
                    val targetsImplementation = targetParts.drop(1).take(2) == listOf("api", "impl")
                    source != targetParts.first() &&
                        (targetParts.getOrNull(1) !in PUBLIC_PACKAGES || targetsImplementation)
                }
            }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `query services must start read only transactions`() {
        val queryServices = classes.filter {
            it.packageName.contains(".query.service") && it.isAnnotatedWith(Service::class.java)
        }
        assertThat(queryServices).isNotEmpty()

        val violations = queryServices.filter {
            it.reflect().getAnnotation(Transactional::class.java)?.readOnly != true
        }
        assertThat(violations).isEmpty()
    }

    @Test
    fun `queries must not mutate or lock database rows`() {
        val violations = classes.filter { it.packageName.contains(".query.") }
            .flatMap { it.methodCallsFromSelf }
            .filter { call ->
                call.target.owner.packageName.startsWith("org.jooq") &&
                    FORBIDDEN_QUERY_OPERATIONS.any { call.target.name.startsWith(it) }
            }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `only owner services may access repositories`() {
        noClasses().that().resideOutsideOfPackages(
            "$FEATURES.*.command.service..",
            "$FEATURES.*.query.service..",
            "$FEATURES.*.command.repository..",
            "$FEATURES.*.query.repository..",
        )
            .should().dependOnClassesThat().resideInAnyPackage(
                "$FEATURES.*.command.repository..",
                "$FEATURES.*.query.repository..",
            )
            .check(classes)
    }

    @Test
    fun `public shared lock adapters must require an active transaction`() {
        val lockMethods = classes.filter { it.packageName.endsWith(".api.impl") }
            .flatMap { javaClass -> javaClass.reflect().declaredMethods.asList() }
            .filter { method -> method.name.endsWith("ForShare") }
        assertThat(lockMethods).isNotEmpty()

        val violations = lockMethods.filter { method ->
            val methodPropagation = method.getAnnotation(Transactional::class.java)?.propagation
            val classPropagation = method.declaringClass.getAnnotation(Transactional::class.java)?.propagation
            methodPropagation != Propagation.MANDATORY && classPropagation != Propagation.MANDATORY
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `command repositories may access only their owned table`() {
        val repositories = classes.filter { it.packageName.endsWith(".command.repository") }
        assertThat(repositories).isNotEmpty()

        val violations = repositories.flatMap { repository ->
            val className = repository.name.substringAfterLast('.').substringBefore('$')
            val table = checkNotNull(OWNED_TABLES[className]) { "Declare table ownership for ${repository.name}" }
            val tableClass = table.lowercase().split('_').joinToString("") { it.replaceFirstChar(Char::uppercase) }

            repository.fieldAccessesFromSelf.filter { access ->
                val owner = access.target.owner
                when (owner.name) {
                    "org.jooq.generated.Tables" -> access.target.name != table
                    else -> owner.packageName == "org.jooq.generated.tables" && owner.simpleName != tableClass
                }
            }
        }

        assertThat(violations).isEmpty()
    }

    private companion object {
        private const val FEATURES = "ru.digitalhustle.certis.features"
        private val classes = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("ru.digitalhustle.certis")
        private val PUBLIC_PACKAGES = setOf("api", "enums", "exceptions")
        private val FORBIDDEN_QUERY_OPERATIONS = setOf(
            "insert",
            "update",
            "delete",
            "merge",
            "truncate",
            "batch",
            "execute",
            "store",
            "forUpdate",
            "forShare",
            "create",
            "alter",
            "drop",
            "loadInto",
            "query",
            "resultQuery",
        )
        private val OWNED_TABLES = mapOf(
            "AccountRepository" to "ACCOUNTS",
            "CategoryRepository" to "CATEGORIES",
            "BudgetRepository" to "BUDGETS",
            "BudgetAllocationRepository" to "BUDGET_CATEGORIES",
            "BudgetOptimizationRepository" to "BUDGET_OPTIMIZATIONS",
            "GoalRepository" to "GOALS",
            "GoalTransactionRepository" to "GOAL_TRANSACTIONS",
            "ProfileRepository" to "PROFILES",
            "ProfilePhotoMetaRepository" to "PROFILE_PHOTOS",
            "UserRepository" to "USERS",
            "RefreshSessionRepository" to "REFRESH_SESSIONS",
            "TransactionRepository" to "TRANSACTIONS",
            "TransferRepository" to "TRANSFERS",
            "RecurringTransactionTemplateRepository" to "RECURRING_TRANSACTION_TEMPLATES",
        )
    }
}

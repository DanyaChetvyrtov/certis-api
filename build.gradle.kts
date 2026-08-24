import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Logging

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)

    alias(libs.plugins.liquibase)
    alias(libs.plugins.jooq.plugin)

    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.kapt)

    jacoco
}

group = "ru.digital-hustle"
version = "0.0.1-SNAPSHOT"
description = "certis-api"

fun dbProperty(
    gradleProperty: String,
    environmentVariable: String,
    defaultValue: String = "",
) = providers
    .gradleProperty(gradleProperty)
    .orElse(providers.environmentVariable(environmentVariable))
    .orElse(defaultValue)

val dbHost = dbProperty("db.host", "DB_HOST", "localhost")
val dbPort = dbProperty("db.port", "DB_PORT", "5432")
val dbName = dbProperty("db.name", "DB_NAME", "certis")
val dbUser = dbProperty("db.user", "DB_USER")
val dbPassword = dbProperty("db.password", "DB_PASSWORD")
val dbSchema = dbProperty("db.schema", "DB_SCHEMA", "keeper")

val dbDriver = "org.postgresql.Driver"
val dbUrl = providers.provider {
    "jdbc:postgresql://${dbHost.get()}:${dbPort.get()}/${dbName.get()}"
}

val changelogMasterPath = "src/main/resources/db/changelog"
val changelogFileName = "db.changelog-master.yaml"
val jooqVersion = libs.versions.jooq.version.get()
val sarif4kVersion = libs.versions.sarif4k.get()

val jacocoExcludes = listOf(
    "org/jooq/generated/**",
)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jooq.jackson.extensions)

    runtimeOnly(libs.postgresql)
    jooqGenerator(libs.postgresql)
    liquibaseRuntime(libs.bundles.liquibase.runtime)
    implementation(libs.liquibase.core)

    // minio
    implementation(libs.minio)

    // persist
    implementation(libs.jakarta.persistence)

    // swagger
    implementation(libs.springdoc)

    // utils
    implementation(libs.commons.io)
    implementation(libs.kotlin.logging)
    implementation(libs.tika.core)
    implementation(libs.twelvemonkeys.imageio.webp)
    implementation(libs.bucket4j.core)
    implementation(libs.caffeine)

    // mapper
    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor)

    // JWT
    implementation(libs.bundles.jwt)

    // tests
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.assertj.core)
    testImplementation(libs.zonky.embedded.postgres)
    testImplementation(libs.zonky.embedded.database.spring.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

buildscript {
    dependencies {
        classpath(libs.liquibase.core)
    }
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jooq") {
            useVersion(jooqVersion)
        }
    }

    if (name.startsWith("ktlint")) {
        resolutionStrategy.force(
            "io.github.detekt.sarif4k:sarif4k:$sarif4kVersion",
            "io.github.detekt.sarif4k:sarif4k-jvm:$sarif4kVersion",
        )
    }
}

liquibase {
    activities {
        create("main") {
            this.arguments = mapOf(
                "changelogFile" to "$changelogMasterPath/$changelogFileName",
                "url" to dbUrl.get(),
                "username" to dbUser.get(),
                "password" to dbPassword.get(),
                "driver" to dbDriver,
            )
        }
    }
    runList = "main"
}

jooq {
    version.set(jooqVersion)
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc = Jdbc().apply {
                    driver = dbDriver
                    url = dbUrl.get()
                    user = dbUser.get()
                    password = dbPassword.get()
                }
                generator = Generator().apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database = org.jooq.meta.jaxb.Database().apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = dbSchema.get()
                        excludes = "databasechangelog|databasechangeloglock"
                    }
                    generate = org.jooq.meta.jaxb.Generate().apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = false
                        isImplicitJoinPathsToMany = false
                    }
                    target = org.jooq.meta.jaxb.Target().apply {
                        packageName = "org.jooq.generated"
                        directory = "build/generated-sources"
                    }
                    strategy = org.jooq.meta.jaxb.Strategy().apply {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}

val databaseConfiguration = mapOf(
    "db.host / DB_HOST" to dbHost,
    "db.port / DB_PORT" to dbPort,
    "db.name / DB_NAME" to dbName,
    "db.user / DB_USER" to dbUser,
    "db.password / DB_PASSWORD" to dbPassword,
    "db.schema / DB_SCHEMA" to dbSchema,
)

val validateDatabaseConfiguration = tasks.register("validateDatabaseConfiguration") {
    group = "database"
    description = "Validates database configuration required by database tasks."

    doLast {
        val missingConfiguration = databaseConfiguration
            .filterValues { it.get().isBlank() }
            .keys

        if (missingConfiguration.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Missing required database configuration:")
                    missingConfiguration.forEach { appendLine(" - $it") }
                    appendLine()
                    append("Use Gradle properties (-Pdb.*) or DB_* environment variables.")
                },
            )
        }
    }
}

tasks.named("update") {
    dependsOn(validateDatabaseConfiguration)
}

tasks.matching { it.name.startsWith("generateJooq") }.configureEach {
    dependsOn(validateDatabaseConfiguration)
}

ktlint {
    relative.set(true)

    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.SARIF)
    }
}

detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<JacocoReport>().configureEach {
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it).apply { exclude(jacocoExcludes) }
            },
        ),
    )
}

tasks.withType<JacocoCoverageVerification>().configureEach {
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it).apply { exclude(jacocoExcludes) }
            },
        ),
    )

    violationRules {
        rule {
            limit {
                minimum = "0.7".toBigDecimal()
            }
        }
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
    dependsOn(tasks.detekt)
    dependsOn(tasks.ktlintCheck)
}

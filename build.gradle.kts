import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Logging
import java.sql.DriverManager

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

val dbHost = providers.gradleProperty("db.host").get()
val dbPort = providers.gradleProperty("db.port").get()
val dbName = providers.gradleProperty("db.name").get()
val dbUser = providers.gradleProperty("db.user").get()
val dbPassword = providers.gradleProperty("db.password").get()
val dbSchema = providers.gradleProperty("db.schema").get()
val dbDriver = "org.postgresql.Driver"
val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
val changelogMasterPath = "src/main/resources/db/changelog"
val changelogFileName = "db.changelog-master.yaml"
val changelogFilePath = "/db/changelog/$changelogFileName"
val jooqPackageName = "ru.digitalhustle.certis.jooq"

val lombokMapstructBindingVersion = "0.2.0"
val jakartaPersistenceVersion = "3.2.0"
val kotlinLoggingVersion = "8.0.02"

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
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jooq.jackson.extensions)

    runtimeOnly(libs.postgresql)
    jooqGenerator(libs.postgresql)
    liquibaseRuntime(libs.bundles.liquibase.runtime)

    // persist
    implementation("jakarta.persistence:jakarta.persistence-api:$jakartaPersistenceVersion")

    // swagger
    implementation(libs.springdoc)

    // utils
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")

    // mapper
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    implementation("org.mapstruct:mapstruct:1.6.3")
    kapt("org.mapstruct:mapstruct-processor:1.6.3")

    // JWT
    implementation(libs.bundles.jwt)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.7.8")
        classpath("org.liquibase:liquibase-core:4.33.0")
    }
}

liquibase {
    activities {
        create("main") {
            this.arguments = mapOf(
                "changelogFile" to "$changelogMasterPath/$changelogFileName",
                "url" to dbUrl,
                "username" to dbUser,
                "password" to dbPassword,
                "driver" to dbDriver,
                "defaultSchemaName" to dbSchema,
                "liquibaseSchemaName" to dbSchema,
            )
        }
    }
    runList = "main"
}

jooq {
    version.set(libs.versions.jooq.version.get())
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc = Jdbc().apply {
                    driver = dbDriver
                    url = dbUrl
                    user = dbUser
                    password = dbPassword
                }
                generator = Generator().apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database = org.jooq.meta.jaxb.Database().apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = dbSchema
                        excludes = "databasechangelog|databasechangeloglock"
                    }
                    generate = org.jooq.meta.jaxb.Generate().apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = false
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

detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
}

tasks.register("ensureSchemaExists") {
    group = "database"
    description = "Creates the application schema in PostgreSQL before running Liquibase."

    doLast {
        DriverManager.getConnection(dbUrl, dbUser, dbPassword).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE SCHEMA IF NOT EXISTS $dbSchema")
            }
        }
    }
}

tasks.named("update") {
    dependsOn("ensureSchemaExists")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.7".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.check {
    dependsOn(tasks.detekt)
}

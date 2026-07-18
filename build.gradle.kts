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
val jooqVersion = libs.versions.jooq.version.get()

val lombokMapstructBindingVersion = "0.2.0"
val jakartaPersistenceVersion = "3.2.0"
val kotlinLoggingVersion = "8.0.02"

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
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jooq.jackson.extensions)

    runtimeOnly(libs.postgresql)
    jooqGenerator(libs.postgresql)
    liquibaseRuntime(libs.bundles.liquibase.runtime)
    implementation(libs.liquibase.core)

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

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
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.kotlin.test.junit5)
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation("io.zonky.test:embedded-postgres:2.2.0")
    testImplementation("io.zonky.test:embedded-database-spring-test:2.7.1")
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jooq") {
            useVersion(jooqVersion)
        }
    }
}

buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.7.11")
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

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JacocoReport> {
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it).apply { exclude(jacocoExcludes) }
        })
    )
}

tasks.withType<JacocoCoverageVerification> {
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it).apply { exclude(jacocoExcludes) }
        })
    )

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
    dependsOn(tasks.detekt)
    dependsOn(tasks.ktlintCheck)
}

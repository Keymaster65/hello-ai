import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("nu.studer.jooq") version "10.2.1"
    // Separate source sets for additional test types (see ADR 0006).
    id("org.unbroken-dome.test-sets") version "4.1.0"
    jacoco
}

group = "io.github.keymaster65"
version = "0.0.1-SNAPSHOT"
description = "Recipe management backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Align the jOOQ runtime managed by Spring Boot with the version used for code generation.
extra["jooq.version"] = "3.21.6"

repositories {
    mavenCentral()
}

// Black-box tests against a running application (src/systemtest/java), run via `./gradlew systemtest`.
testSets {
    create("systemtest")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    // OpenAPI 3.1 documentation + Swagger UI; the 3.x line is the one built against Spring Boot 4.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
    // Spring Boot 4 provides Liquibase auto-configuration in a dedicated module.
    implementation("org.springframework.boot:spring-boot-liquibase")
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")

    // Code generation: LiquibaseDatabase interprets the changelog in-memory, so no live DB is required at build time.
    jooqGenerator("org.jooq:jooq-meta-extensions-liquibase:3.21.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        // jqwik brings its own JUnit Platform engine; keep vintage out of the way.
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    // Spring Boot 4 ships the @WebMvcTest slice in a dedicated module.
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("net.jqwik:jqwik:1.9.3")
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
    testImplementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.4.0"))

    // System tests talk to the application over HTTP only; the JDK HttpClient is enough.
    // Spring is needed solely to boot the application when no external instance is given.
    // The configurations are created by the testSets block above, so they are addressed by name.
    "systemtestImplementation"("org.junit.jupiter:junit-jupiter")
    "systemtestImplementation"("org.assertj:assertj-core")
    "systemtestImplementation"("io.zonky.test:embedded-postgres:2.1.0")
    "systemtestImplementation"(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.4.0"))
    "systemtestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

jooq {
    version = "3.21.6"
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation = true
            jooqConfiguration.apply {
                logging = Logging.WARN
                generator.apply {
                    name = "org.jooq.codegen.JavaGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.liquibase.LiquibaseDatabase"
                        properties = listOf(
                            // Resolve the changelog (and its relative includes) from the resources root.
                            Property()
                                .withKey("rootPath")
                                .withValue("${project.projectDir}/src/main/resources"),
                            Property()
                                .withKey("scripts")
                                .withValue("db/changelog/db.changelog-master.xml"),
                            // Keep Liquibase's own bookkeeping tables out of the generated code.
                            Property()
                                .withKey("includeLiquibaseTypes")
                                .withValue("false"),
                            // Fold unquoted names to lower case so the generated names match
                            // PostgreSQL at runtime (see ADR 0004).
                            Property()
                                .withKey("defaultNameCase")
                                .withValue("lower"),
                            Property()
                                .withKey("unqualifiedSchema")
                                .withValue("none"),
                        )
                    }
                    generate.apply {
                        isRecords = true
                        isPojos = false
                        isDaos = false
                        isDeprecated = false
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "io.github.keymaster65.helloai.adapter.out.persistence.jooq"
                        directory = layout.buildDirectory
                            .dir("generated-src/jooq/main").get().asFile.absolutePath
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Frontend (React/Vite in frontend/), built into the Boot jar – see ADR 0007.
// Use -PskipFrontend to keep the Java-only inner loop fast.
// ---------------------------------------------------------------------------
val frontendDir = file("frontend")
val npmExecutable =
    if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
val frontendEnabled = !providers.gradleProperty("skipFrontend").isPresent

/** Inputs shared by the tasks that consume the frontend sources. */
fun Exec.frontendSources() {
    inputs.file("$frontendDir/package.json")
    inputs.file("$frontendDir/vite.config.ts")
    inputs.file("$frontendDir/tsconfig.json")
    inputs.file("$frontendDir/index.html")
    inputs.dir("$frontendDir/src")
}

val npmInstall by tasks.registering(Exec::class) {
    group = "frontend"
    description = "Installs the frontend dependencies (npm ci)."
    onlyIf { frontendEnabled }
    workingDir = frontendDir
    commandLine(npmExecutable, "ci", "--no-audit", "--no-fund")
    inputs.file("$frontendDir/package.json")
    inputs.file("$frontendDir/package-lock.json")
    outputs.dir("$frontendDir/node_modules")
}

val frontendBuild by tasks.registering(Exec::class) {
    group = "frontend"
    description = "Type-checks and bundles the frontend (vite build)."
    onlyIf { frontendEnabled }
    dependsOn(npmInstall)
    workingDir = frontendDir
    commandLine(npmExecutable, "run", "build")
    frontendSources()
    outputs.dir("$frontendDir/dist")
}

val frontendTest by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs the frontend unit tests (vitest)."
    onlyIf { frontendEnabled }
    dependsOn(npmInstall)
    workingDir = frontendDir
    commandLine(npmExecutable, "run", "test")
    frontendSources()
    outputs.upToDateWhen { false }
}

// End-to-end tests in a real browser (see ADR 0008). Not wired into `check`: they are the
// slowest layer and need a reachable PostgreSQL, so they stay an explicit step.
tasks.register<Exec>("e2eTest") {
    group = "verification"
    description = "Runs the Playwright end-to-end tests against the Boot jar."
    onlyIf { frontendEnabled }
    dependsOn(npmInstall, tasks.named("bootJar"))
    workingDir = frontendDir
    commandLine(npmExecutable, "run", "test:e2e")
    // With -Pe2e.baseUrl the suite runs against an already deployed instance and starts nothing.
    environment("E2E_BASE_URL", providers.gradleProperty("e2e.baseUrl").getOrElse(""))
    inputs.dir("$frontendDir/e2e")
    inputs.file("$frontendDir/playwright.config.ts")
    outputs.upToDateWhen { false }
}

// The bundle is served by Spring Boot from the classpath, so the SPA and the API
// share one origin and one deployable artifact.
tasks.named<ProcessResources>("processResources") {
    dependsOn(frontendBuild)
    from("$frontendDir/dist") {
        into("static")
    }
}

tasks.named("check") {
    dependsOn(frontendTest)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<Test>("systemtest") {
    useJUnitPlatform()
    // Run against an already deployed instance with -Psystemtest.baseUrl=http://host:port;
    // without it the task boots the application itself on a free port.
    systemProperty("systemtest.baseUrl", providers.gradleProperty("systemtest.baseUrl").getOrElse(""))
    shouldRunAfter(tasks.named("test"))
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

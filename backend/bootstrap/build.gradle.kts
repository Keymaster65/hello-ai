plugins {
    java
    id("org.springframework.boot")
    // Separate source set for the system tests (see ADR 0006).
    id("org.unbroken-dome.test-sets") version "4.1.0"
}

description = "Spring Boot entry point – wires the layers and produces the deployable"

// Keep the artifact name independent of the module name (see ADR 0011).
val artifactName = "recipe-backend"

// Black-box tests against a running application, run via `./gradlew systemtest`.
testSets {
    create("systemtest")
}

dependencies {
    // Only the outermost adapter layer; :application and :domain arrive transitively.
    implementation(project(":backend:adapter"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    // Spring Boot 4 provides Liquibase auto-configuration in a dedicated module.
    implementation("org.springframework.boot:spring-boot-liquibase")
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        // jqwik brings its own JUnit Platform engine; keep vintage out of the way.
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    // Spring Boot 4 ships the @WebMvcTest slice in a dedicated module.
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
    testImplementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.4.0"))
    // Enforces the layering rule (see ADR 0012). It runs here because only this module sees
    // every layer on its classpath.
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")

    // System tests talk to the application over HTTP only; the JDK HttpClient is enough.
    // Spring is needed solely to boot the application when no external instance is given.
    // The configurations are created by the testSets block above, so they are addressed by name.
    "systemtestImplementation"("io.zonky.test:embedded-postgres:2.1.0")
    "systemtestImplementation"(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.4.0"))
}

// ---------------------------------------------------------------------------
// Frontend (React/Vite in frontend/), built into the Boot jar – see ADR 0007.
// It belongs to this module because this is where the deployable is assembled.
// Use -PskipFrontend to keep the Java-only inner loop fast.
// ---------------------------------------------------------------------------
val frontendDir = rootProject.file("frontend")
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
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<Test>("systemtest") {
    // Run against an already deployed instance with -Psystemtest.baseUrl=http://host:port;
    // without it the task boots the application itself on a free port.
    systemProperty("systemtest.baseUrl", providers.gradleProperty("systemtest.baseUrl").getOrElse(""))
    shouldRunAfter(tasks.named("test"))
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName = artifactName
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName = artifactName
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

plugins {
    java
    // Version and `apply false` come from the root project, so no alias here.
    id("org.springframework.boot")
    // Separate source set for the system tests (see ADR 0006).
    alias(libs.plugins.test.sets)
}

description = "Spring Boot entry point – wires the layers and produces the deployable"

// Keep the artifact name independent of the module name (see ADR 0011).
val artifactName = "recipes"

// Black-box tests against a running application, run via `./gradlew systemtest`.
testSets {
    create("systemtest")
}

dependencies {
    // Only the outermost adapter layer; :application and :domain arrive transitively.
    implementation(project(":modules:backend:adapter"))

    implementation(libs.spring.boot.starter.web)
    // Spring Boot 4 provides Liquibase auto-configuration in a dedicated module.
    implementation(libs.spring.boot.liquibase)
    implementation(libs.liquibase.core)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test) {
        // jqwik brings its own JUnit Platform engine; keep vintage out of the way.
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    // Spring Boot 4 ships the @WebMvcTest slice in a dedicated module.
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.zonky.embedded.postgres)
    testImplementation(enforcedPlatform(libs.zonky.postgres.binaries.bom))
    // Enforces the layering rule (see ADR 0012). It runs here because only this module sees
    // every layer on its classpath.
    testImplementation(libs.archunit.junit5)

    // System tests talk to the application over HTTP only; the JDK HttpClient is enough.
    // Spring is needed solely to boot the application when no external instance is given.
    // The configurations are created by the testSets block above, so they are addressed by name.
    "systemtestImplementation"(libs.zonky.embedded.postgres)
    "systemtestImplementation"(enforcedPlatform(libs.zonky.postgres.binaries.bom))
}

// ---------------------------------------------------------------------------
// Frontend (React/Vite in frontend/), built into the Boot jar – see ADR 0007.
// It belongs to this module because this is where the deployable is assembled.
// Use -PskipFrontend to keep the Java-only inner loop fast.
// ---------------------------------------------------------------------------
val frontendDir = rootProject.file("modules/frontend")
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

// ---------------------------------------------------------------------------
// Systemdokumentation (docs/system, gerendert nach HTML), ebenfalls im Boot-Jar – ADR 0024.
// Gerendert wird im Wurzelprojekt; hier wird nur eingepackt, weil hier das Deployable
// entsteht – dieselbe Rollenteilung wie beim Frontend.
// Use -PskipDocs to keep the Java-only inner loop fast.
// ---------------------------------------------------------------------------
val systemdokuVerzeichnis = rootProject.layout.buildDirectory.dir("docs/asciidoc")
val systemdokuEnabled = !providers.gradleProperty("skipDocs").isPresent

// The bundle is served by Spring Boot from the classpath, so the SPA and the API
// share one origin and one deployable artifact.
tasks.named<ProcessResources>("processResources") {
    dependsOn(frontendBuild)
    from("$frontendDir/dist") {
        into("static")
    }

    if (systemdokuEnabled) {
        // Cross-project dependency: der rendernde Task gehört dem Wurzelprojekt, weil die
        // Dokumentation das Repository beschreibt und keine Schicht (ADR 0022).
        dependsOn(":asciidoctor")
        // Das gerenderte Kapitelwerk samt der daneben kopierten ADRs. `system.html` heißt im
        // Jar `index.html`, damit die Doku unter `/recipes/docs/` liegt und nicht unter einem
        // Dateinamen; die relativen Verweise auf `adr/…` stimmen dadurch unverändert.
        //
        // Das Muster ist verankert (`^…$`). Ohne die Anker sucht Gradle den Ausdruck *irgendwo*
        // im Dateinamen – seit die ADRs als HTML danebenliegen (ADR 0026), traf er auch
        // `0022-asciidoc-systemdokumentation-in-docs-system.html` und benannte sie in
        // `…-in-docs-index.html` um. Aufgefallen ist das als 404 im `DocumentationSystemTest`.
        from(systemdokuVerzeichnis) {
            into("static/docs")
            rename("""^system\.html$""", "index.html")
        }
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

import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

plugins {
    `java-library`
    alias(libs.plugins.jooq)
}

description = "Inbound REST and outbound jOOQ adapters, plus the Liquibase changelog"

// Align the jOOQ runtime managed by Spring Boot with the version used for code generation.
extra["jooq.version"] = libs.versions.jooq.get()

dependencies {
    api(project(":modules:backend:application"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jooq)
    // The @Operation/@Schema annotations live on the REST adapter, so the contract is
    // described next to the code it belongs to (ADR 0005). `api` because :bootstrap builds
    // the OpenAPI document from these types.
    api(libs.springdoc.openapi.starter.webmvc.ui)

    // Das MCP-SDK: Werkzeuge, Ressourcen und Transport des MCP-Adapters sind SDK-Typen (ADR 0049).
    // `implementation`, nicht `api`: Der Server wird in diesem Modul zusammengesetzt, und keine
    // dieser Klassen verlässt es – :bootstrap darf sie nach der Zwiebelregel gar nicht kennen
    // (ADR 0019).
    implementation(libs.mcp.sdk)

    // Der Adapter gitdata (ADR 0053): JGit liest und schreibt den Branch `data` über die
    // Objektdatenbank – ohne `git` im PATH und ohne Arbeitsverzeichnis. Jackson 3 ist die Fassung,
    // die Spring Boot 4 mitbringt; hier steht sie ausdrücklich, weil dieses Modul sie selbst
    // benutzt und nicht mehr nur transitiv erbt. Beides `implementation`: Keiner dieser Typen
    // verlässt das Modul (ADR 0019).
    implementation(libs.jgit)
    implementation(libs.tools.jackson.databind)

    // Code generation: LiquibaseDatabase interprets the changelog in-memory, so no live DB is
    // required at build time. The changelog lives in this module because it describes the
    // schema this adapter talks to.
    jooqGenerator(libs.jooq.meta.extensions.liquibase)

    testImplementation(libs.jqwik)
    // Die MCP-Werkzeuge werden gegen einen Mock ihres eingehenden Ports geprüft (ADR 0049);
    // bisher brauchte dieses Modul kein Mocking.
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

jooq {
    version = libs.versions.jooq.get()
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
                            // Resolve the changelog (and its relative includes) from this
                            // module's resources root.
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

// Der jOOQ-Codegen erzeugt oberhalb von `…/persistence/jooq` einige tausend Zeilen Tabellen-
// und Record-Klassen. Gemessen wird die *geschriebene* Schicht, nicht der Generator: Ohne diese
// Ausnahme bestünde die Abdeckung dieses Moduls zu vier Fünfteln aus Code, den niemand geschrieben
// hat und den kein Test je aufrufen soll (ADR 0047).
tasks.named<JacocoReport>("jacocoTestReport") {
    classDirectories.setFrom(
        files(
            classDirectories.files.map { verzeichnis ->
                fileTree(verzeichnis) {
                    exclude("io/github/keymaster65/helloai/adapter/out/persistence/jooq/**")
                }
            },
        ),
    )
}

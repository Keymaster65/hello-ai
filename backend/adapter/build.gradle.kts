import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

plugins {
    `java-library`
    id("nu.studer.jooq") version "10.2.1"
}

description = "Inbound REST and outbound jOOQ adapters, plus the Liquibase changelog"

// Align the jOOQ runtime managed by Spring Boot with the version used for code generation.
extra["jooq.version"] = "3.21.6"

dependencies {
    api(project(":backend:application"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    // The @Operation/@Schema annotations live on the REST adapter, so the contract is
    // described next to the code it belongs to (ADR 0005). `api` because :bootstrap builds
    // the OpenAPI document from these types.
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")

    // Code generation: LiquibaseDatabase interprets the changelog in-memory, so no live DB is
    // required at build time. The changelog lives in this module because it describes the
    // schema this adapter talks to.
    jooqGenerator("org.jooq:jooq-meta-extensions-liquibase:3.21.6")

    testImplementation("net.jqwik:jqwik:1.9.3")
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

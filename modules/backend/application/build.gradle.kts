plugins {
    `java-library`
}

description = "Use cases, ports and business logic"

dependencies {
    // Ports expose domain types, so consumers need them too.
    api(project(":modules:backend:domain"))

    // No framework on the production classpath (ADR 0045): wiring and transaction boundaries
    // are supplied from :bootstrap, so a framework import here does not even compile.

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

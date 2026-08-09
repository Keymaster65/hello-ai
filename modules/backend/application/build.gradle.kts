plugins {
    `java-library`
}

description = "Use cases, ports and business logic"

dependencies {
    // Ports expose domain types, so consumers need them too.
    api(project(":modules:backend:domain"))

    // Only what the layer really uses: component model and declarative transactions.
    // No web, no persistence technology.
    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

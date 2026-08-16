plugins {
    `java-library`
}

description = "Domain model – the innermost layer"

// Deliberately without a single production dependency. The freedom from frameworks that
// ArchUnit checks (docs/prompt/architektur.adoc) is guaranteed structurally here: what is not on the compile
// classpath cannot be imported.
dependencies {
    testImplementation(libs.jqwik)
}

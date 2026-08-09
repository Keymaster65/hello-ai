import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Root project: no sources of its own. Each layer of the architecture is a module
// (see ADR 0013):
//
//     :bootstrap  →  :adapter  →  :application  →  :domain
//
// The dependency direction is enforced by the build itself: a module simply cannot see
// what it does not declare. `frontend/` stays an npm project that :bootstrap bundles.
//
// The Spring Boot plugin is only applied in :bootstrap (it is what produces the Boot jar);
// its BOM is imported everywhere so that every module resolves the same versions.
plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")

    group = "io.github.keymaster65"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    extensions.configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(SpringBootPlugin.BOM_COORDINATES)
        }
    }

    dependencies {
        "testImplementation"("org.assertj:assertj-core")
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    // The Spring Boot plugin adds this automatically, but it now only applies to :bootstrap.
    // Without it, parameter names are missing from the bytecode and Spring MVC cannot bind
    // `@PathVariable long id` – requests would fail with 400 instead of hitting the handler.
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

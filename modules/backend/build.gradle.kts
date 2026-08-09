import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Container module for the application. It carries no sources of its own; each layer is a
// submodule (see ADR 0013 for the split, ADR 0014 for this grouping):
//
//     :backend:bootstrap  →  :backend:adapter  →  :backend:application  →  :backend:domain
//
// The dependency direction is enforced by the build: a module cannot see what it does not
// declare. Everything the layers share lives here – close to where it applies, instead of
// in the root project.
//
// The Spring Boot plugin is only applied in :backend:bootstrap (it produces the Boot jar);
// its BOM is imported everywhere so all modules resolve the same versions.
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

    // The Spring Boot plugin adds this automatically, but it now only applies to
    // :backend:bootstrap. Without it, parameter names are missing from the bytecode and
    // Spring MVC cannot bind `@PathVariable long id` – requests would fail with 400
    // instead of reaching the handler (see ADR 0013).
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

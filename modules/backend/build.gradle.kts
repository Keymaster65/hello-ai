import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Container module for the application. It carries no sources of its own; each layer is a
// submodule (see docs/prompt/architektur.adoc for the split, docs/prompt/architektur.adoc for this grouping):
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

    // `rootProject.libs`, not plain `libs`: inside `subprojects` the receiver is the
    // submodule, and its catalog extension does not exist yet while this block runs
    // (see docs/prompt/build.adoc). The configuration names stay strings because `java` is applied
    // dynamically above, so there are no type-safe accessors in this script.
    dependencies {
        "testImplementation"(rootProject.libs.assertj.core)
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
    }

    // The Spring Boot plugin adds this automatically, but it now only applies to
    // :backend:bootstrap. Without it, parameter names are missing from the bytecode and
    // Spring MVC cannot bind `@PathVariable long id` – requests would fail with 400
    // instead of reaching the handler (see docs/prompt/architektur.adoc).
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // JaCoCo misst in jedem Modul, ausgewertet wird die Messung aber im Wurzelprojekt: Der Task
    // `coverageTable` liest die XML-Fassung jedes Moduls und schreibt daraus die Tabelle des
    // Kapitels „Abdeckung" (docs/prompt/tests.adoc). XML ist in Gradle voreingestellt *aus* – ohne diese
    // Zeilen gäbe es nur den HTML-Report, den kein Werkzeug lesen kann. Der Report hängt am
    // `test`-Task des jeweiligen Moduls, weil er ohne dessen Messdatei leer wäre.
    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.named("test"))
        reports {
            xml.required = true
            html.required = true
        }
    }

    // Gemessen wird bei jedem `test`-Lauf, in jedem Modul – nicht nur in `:bootstrap`, wie es
    // bis docs/prompt/tests.adoc der Fall war. Sonst zeigte die Tabelle drei Module im Stand des letzten
    // ausdrücklichen Aufrufs und eines im Stand des letzten Testlaufs.
    // `named("test")` statt `withType<Test>()`: In `:bootstrap` gibt es mit `systemtest` ein
    // zweites Test-Source-Set (docs/prompt/tests.adoc), und das soll ein `./gradlew test` nicht anstoßen.
    tasks.named("test") {
        finalizedBy(tasks.named("jacocoTestReport"))
    }
}

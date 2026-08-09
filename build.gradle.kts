// Root project: no sources, no shared configuration – it only aggregates.
//
// Alle Bausteine liegen unter `modules/` (ADR 0015):
//
//   :modules:backend   – die Anwendung, ein Untermodul je Schicht (ADR 0013, ADR 0014)
//   modules/frontend   – npm-Projekt, von :modules:backend:bootstrap ins Boot-Jar gepackt
//
// Die gemeinsame Konfiguration der Schichtmodule steht in `modules/backend/build.gradle.kts`,
// also dort, wo sie gilt. Hier stehen nur die Plugins, die auf dem Buildscript-Classpath
// verfügbar sein müssen; angewendet werden sie in den Modulen.
//
// Die gewohnten Kommandos funktionieren unverändert, weil Gradle einen Task-Namen an jedes
// Projekt weiterleitet, das ihn kennt: ./gradlew clean build, test, systemtest, e2eTest.
plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

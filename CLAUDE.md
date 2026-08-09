# Projekt: [Projektname]

## Rolle
Du bist ein **Senior Java-Entwickler** (Backend) und ein
**Senior TypeScript-Entwickler** (Frontend). Du schreibst sauberen, wartbaren
und testbaren Code nach Best Practices und modernen Standards der jeweiligen
Sprache. Welche Rolle gilt, ergibt sich aus der bearbeiteten Datei bzw. Aufgabe.

## Tech-Stack

### Backend
- Sprache: Java 25 (LTS)
- Build-Tool: Gradle mit **Kotlin-DSL** (`build.gradle.kts`) –
  **ausschließlich über den Wrapper** (`./gradlew`)
- Testing: jqwik, Mockito, AssertJ, jacoco
- Persistenz: jooq
- Datenbank: PostgreSQL
- Framework: Spring Boot 4

### Frontend
- Sprache: TypeScript
- Build-Tool / Dev-Server: Vite
- Testing: Vitest (Komponenten), Playwright (E2E im echten Browser)
- UI-Framework: React

## Grundregeln
1. **Verstehen vor Handeln**: Bestehenden Code analysieren.
2. **Kleine, nachvollziehbare Schritte.**
3. **Deutsch kommunizieren**, Code/JavaDoc auf Englisch.
4. **Keine ungefragten Refactorings** großer Bereiche.
5. Bei Unklarheiten: **nachfragen** statt raten.
6. Moderne Java-Features nutzen (Records, Streams, Optional, Pattern Matching, Text Blocks).

## Coding-Konventionen
- Google Java Style / einheitliche Formatierung
- `Optional` statt `null` an Rückgabe-Grenzen
- Keine Business-Logik in Controllern
- DTOs für API-Grenzen, Entities nicht direkt exponieren
- Immutability bevorzugen (Records, `final`)

## Skills
- **Architektur** → siehe `.claude/skills/architecture.md`
- **Tests** → siehe `.claude/skills/testing.md`
- **Develop / Session-Protokoll** → siehe `.claude/skills/develop.md`
  (pflegt `docs/log/claudeLog.md` fortlaufend – in **jeder** Session aktiv)

## Workflow
1. Aufgabe & Kontext verstehen
2. Plan vorstellen (bei größeren Änderungen)
3. Implementieren
4. Tests schreiben & mit `./gradlew test` ausführen
5. Systemtests mit `./gradlew systemtest` ausführen
6. E2E-Tests mit `./gradlew e2eTest` ausführen
7. Zusammenfassung geben

## Nützliche Befehle
**Immer den Gradle-Wrapper `./gradlew` verwenden** (Windows: `gradlew.bat`), nie ein
lokal installiertes `gradle` – siehe [ADR 0009](docs/adr/0009-gradle-wrapper-verbindlich.md).

- Build: `./gradlew clean build`
- Tests: `./gradlew test`
- Systemtests: `./gradlew systemtest`
  (gegen laufende Instanz: `./gradlew systemtest -Psystemtest.baseUrl=http://localhost:8080`)
- E2E-Tests: `./gradlew e2eTest` – benötigt eine erreichbare PostgreSQL
  (gegen laufende Instanz: `./gradlew e2eTest -Pe2e.baseUrl=http://localhost:8080`)
  Ergebnisse inkl. Videos jedes Laufs: `build/e2e/` (Report: `build/e2e/report/index.html`)

## Definition of Done
- [ ] Code kompiliert (`./gradlew clean build`)
- [ ] Tests grün (`./gradlew test`)
- [ ] **Systemtests grün (`./gradlew systemtest`)** – prüfen die laufende Anwendung über HTTP
- [ ] **E2E-Tests grün (`./gradlew e2eTest`)** – prüfen die Nutzer-Flows im echten Browser
- [ ] Keine kritischen Warnungen/Linter-Fehler
- [ ] Architektur- und Test-Prinzipien eingehalten
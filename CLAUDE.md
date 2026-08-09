# Projekt: [Projektname]

## Rolle
Du bist ein **Senior Java-Entwickler** (Backend) und ein
**Senior TypeScript-Entwickler** (Frontend). Du schreibst sauberen, wartbaren
und testbaren Code nach Best Practices und modernen Standards der jeweiligen
Sprache. Welche Rolle gilt, ergibt sich aus der bearbeiteten Datei bzw. Aufgabe.

## Tech-Stack

### Backend
- Sprache: Java 25 (LTS)
- Build-Tool: gradle
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
4. Tests schreiben & mit `gradle test` ausführen
5. Systemtests mit `gradle systemtest` ausführen
6. E2E-Tests mit `gradle e2eTest` ausführen
7. Zusammenfassung geben

## Nützliche Befehle
- Build: `gradle clean build`
- Tests: `gradle test`
- Systemtests: `gradle systemtest`
  (gegen laufende Instanz: `gradle systemtest -Psystemtest.baseUrl=http://localhost:8080`)
- E2E-Tests: `gradle e2eTest` – benötigt eine erreichbare PostgreSQL
  (gegen laufende Instanz: `gradle e2eTest -Pe2e.baseUrl=http://localhost:8080`)
  Ergebnisse inkl. Videos jedes Laufs: `build/e2e/` (Report: `build/e2e/report/index.html`)

## Definition of Done
- [ ] Code kompiliert (`gradle clean build --test`)
- [ ] Tests grün (`gradle test`)
- [ ] **Systemtests grün (`gradle systemtest`)** – prüfen die laufende Anwendung über HTTP
- [ ] **E2E-Tests grün (`gradle e2eTest`)** – prüfen die Nutzer-Flows im echten Browser
- [ ] Keine kritischen Warnungen/Linter-Fehler
- [ ] Architektur- und Test-Prinzipien eingehalten
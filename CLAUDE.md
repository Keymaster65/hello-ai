# Projekt: [Projektname]

## Rolle
Du bist ein Senior Java-Entwickler. Du schreibst sauberen, wartbaren
und testbaren Code nach Best Practices und modernen Java-Standards.

## Tech-Stack
- Sprache: Java 25 (LTS)
- Build-Tool: gradle
- Testing: jqwik, Mockito, AssertJ, jacoco
- Persistenz: jooq
- Datenbank: PostgreSQL
- Framework: Spring Boot 4

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

## Workflow
1. Aufgabe & Kontext verstehen
2. Plan vorstellen (bei größeren Änderungen)
3. Implementieren
4. Tests schreiben & mit `gradle test` ausführen
5. Zusammenfassung geben

## Nützliche Befehle
- Build: `gradle clean build`
- Tests: `gradle test`

## Definition of Done
- [ ] Code kompiliert (`gradle clean build --test`)
- [ ] Tests grün (`gradle test`)
- [ ] Keine kritischen Warnungen/Linter-Fehler
- [ ] Architektur- und Test-Prinzipien eingehalten
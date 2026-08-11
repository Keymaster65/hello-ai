# Claude Log

Chronologischer Verlauf der Session (Prompts des Nutzers + Zusammenfassung meiner Aktionen).
**Sortierung: absteigend — neuester Eintrag oben.** Neue Prompts werden immer vorne eingefügt.
Datum: 2026-07-19

---

## 80. Prompt: „Erstelle eine Asciidoc Systemdocumentation in "docs/system" und Pflege diese in Zukunft mit. Ergänze das als ADR."

_Delta: 37:50 · 176.921 out · 608.818 in (neu) · 23.137.633 gesamt_
_Stand (Session, 1 Prompts): 37:50 · 176.921 out · 23.137.633 gesamt_

**Aktionen:**
- Rückfrage vorab (drei Punkte). Antworten: **eigene schlanke Gliederung** statt arc42,
  Rendern über das **Asciidoctor-Gradle-Plugin**, Pflegepflicht in einem **eigenen Skill**.
- `docs/system/` angelegt: `system.adoc` (Master mit Attributen, Abgrenzung zu README/ADR,
  `include`-Liste, ADR-Index) plus sechs Kapitel – `ueberblick`, `schichten`,
  `datenmodell`, `api`, `betrieb`, `qualitaetssicherung`.
- Inhalte aus dem Code erhoben, nicht aus dem README abgeschrieben. Neu dokumentiert sind
  u. a. die vier `error`-Codes des `GlobalExceptionHandler`, die Validierungsregeln der
  DTOs, dass `created_at`/`updated_at` geschrieben aber nie gelesen werden, die
  Positions-Asymmetrie zwischen `Ingredient` und `PreparationStep`, die vierfach verankerte
  Festlegung des Context-Paths und ein Abschnitt „Offene Punkte" (keine Auth, kein HTTPS,
  kein Actuator, keine Paginierung).
- Asciidoctor-Plugin 4.0.5 in `libs.versions.toml`; im Wurzelprojekt angewendet, dazu
  `base` (für `clean`/`build`) und ein `repositories`-Block. `./gradlew asciidoctor` →
  `build/docs/asciidoc/system.html`, hängt an `build`.
- `baseDirFollowsSourceFile()` war nötig – ohne das sucht Asciidoctor die Kapitel im
  Projektverzeichnis statt neben dem Masterdokument (Fehlschlag reproduziert).
- Kaputte Includes **und** kaputte Querverweise sind Build-Fehler
  (`fatalWarnings(missingIncludes(), "possible invalid reference")`, dafür
  `logLevel = INFO`). Beide Regeln gegen echte Verstöße geprüft: fehlende Includes und der
  Anker `[[Offene Punkte]]` (Leerzeichen in der ID) brachen den Task, nach der Korrektur
  auf `[[offene-punkte]]` läuft er meldungsfrei.
- `docs/adr/0022-asciidoc-systemdokumentation-in-docs-system.md`: sechs Optionen (README
  ausbauen, arc42 voll/gekürzt, eigene Gliederung, Markdown, Generieren), Entscheidung mit
  den drei Regeln „verlinken statt kopieren / keine Versionsnummern / bei Widerspruch
  gewinnt der Code", Nachteile inkl. der Abweichung „Plugin im Wurzelprojekt".
- `.claude/skills/systemdoc.md`: Zuordnung „Auslöser → zu pflegendes Kapitel" (15 Zeilen),
  AsciiDoc-Konventionen, Prüfschritt. In `CLAUDE.md` unter „Skills" verlinkt, dazu
  `./gradlew asciidoctor` in „Nützliche Befehle"; README bekommt einen Verweiskasten.
- Kein Workflow-Schritt und kein DoD-Häkchen in `CLAUDE.md` – die Verankerung liegt laut
  Antwort ausschließlich im Skill. (Zwischenzeitlich ergänzt und wieder entfernt.)
- Beim Gegenlesen am Code zwei eigene Fehlaussagen korrigiert: `RecipeDetail` lädt sein
  Rezept **selbst** (nicht „Darstellung ohne Datenzugriff"), und der JaCoCo-Report hängt nur
  im Modul `:bootstrap` an `test`, nicht in jedem Backend-Modul.
- **DoD:** `./gradlew clean build` grün (4m15s, 31 Tasks, `:asciidoctor` inbegriffen),
  `systemtest` grün (15 Tests), `e2eTest` grün (9 Tests). Für die E2E-Stufe waren in dieser
  Umgebung zwei Vorbereitungen nötig: eine lokale PostgreSQL (`initdb`/`pg_ctl` unter
  `/tmp/pgdata`, danach wieder gestoppt) und `npx playwright install chromium` – der
  Browser fehlte, was zunächst 9 Fehlschläge erzeugte.

## 79. Prompt: „Ergänze Currying für Java-record Klassen mit mehr als 2 Members. Ergänze das als ADR."

_Delta: 15:49 · 86.319 out · 221.592 in (neu) · 5.987.902 gesamt_
_Stand (Session, 3 Prompts): 15:49 · 86.319 out · 5.987.902 gesamt_

**Aktionen:**
- Rückfrage vorab (drei Punkte). Antworten: **benannte Step-Interfaces** statt roher
  `Function`-Kette, Geltung **überall** (Domäne *und* Adapter-DTOs), Absicherung per
  **ArchUnit**.
- Betroffen sind sechs Records mit mehr als zwei Komponenten: `Recipe` (8),
  `RecipeResponse` (8), `RecipeRequest` (7), `ErrorResponse` (4), `Ingredient` (3),
  `IngredientDto` (3). Nicht betroffen: `PreparationStep`, `PreparationStepResponse`,
  `ErrorResponse.FieldError` (je 2), `PreparationStepDto` (1).
- Je Record eine statische `curried()`-Factory plus ein `@FunctionalInterface` pro
  Komponente, benannt wie die Komponente. Der kanonische Konstruktor bleibt öffentlich und
  einziger Ort der Validierung – die Kette ruft ihn im letzten Schritt auf.
- `RecordCurryingTest` (ArchUnit, `:bootstrap`) prüft nicht nur die Existenz von
  `curried()`, sondern die **Form der Kette**: Schritt *n* ist ein Interface mit genau einer
  Methode, diese heißt wie Komponente *n*, nimmt ein Argument von deren Typ, der letzte
  Schritt liefert den Record.
- Regel gegengeprüft: mit einem temporären Probe-Record schlug sie in beiden Fehlerfällen
  zu (fehlende Factory; vertauschte Schritte → „Schritt 2 heißt c(), erwartet war die
  Komponente b()"). Probe danach entfernt.
- Neue Property-Tests `CurriedFactoryTest` in `:domain` und `:adapter` (je 5 Tests, grün):
  Kette ≡ kanonischer Konstruktor, Validierung greift weiterhin, teilweise angewandte Kette
  ist wiederverwendbar, `ErrorResponse.of(…)` ≡ Kette ohne Field-Errors.
- [ADR 0021](../adr/0021-currying-fuer-records-mit-mehr-als-zwei-komponenten.md) angelegt;
  Verweise in `README.md` (Testübersicht) und `.claude/skills/architecture.md`
  (neuer Abschnitt „Records: Currying ab drei Komponenten").
- Bestehende Aufrufstellen bewusst **nicht** umgestellt (Grundregel 4: keine ungefragten
  Refactorings); `curried()` steht additiv neben dem Konstruktor.
- DoD: `clean build` grün (2:30), `test` grün, `systemtest` grün (58s),
  `e2eTest` grün (9 Tests, 16,6s).

---

## 78. Prompt: „Teile das "domain" Java Package im "domain" gradle Modul auf in "domain/model" und "domain/services"."

_Delta: 9:45 · 28.577 out · 104.834 in (neu) · 2.675.738 gesamt_
_Stand (Session, 3 Prompts): 9:45 · 28.577 out · 2.675.738 gesamt_

**Aktionen:**
- Befund vorab: Die Domäne enthielt ausschließlich Modelltypen, keinen Domain-Service.
  `RecipeServiceImpl` ist ein Application-Service (Spring, Ports) und bleibt, wo er ist.
- `Recipe`, `Ingredient`, `PreparationStep`, `Difficulty` per `git mv` nach
  `io.github.keymaster65.helloai.domain.model` verschoben; Package- und Import-Zeilen in
  12 Dateien über alle vier Schichten angepasst.
- `domain.services` angelegt, vorerst leer. Das `package-info.java` nennt die drei
  Bedingungen für eine Klasse dort (Domänenregel statt Anwendungsfall, mehr als ein
  Aggregat, frameworkfrei); `domain.model` ebenfalls dokumentiert.
- `OnionArchitectureTest`: Ringe jetzt `domainModels("..domain.model..")` **und**
  `domainServices("..domain.services..")` – die Richtung Modell ↔ Domain-Service ist damit
  ab dem ersten Service geprüft. `withOptionalLayers(true)` bleibt nötig (leerer Ring).
- `LayeredArchitectureTest`: neue Regel `domain_is_split_into_model_and_services` verhindert
  Klassen direkt in `..domain`. Probe wie in ADR 0012: eine Klasse dort lässt die Regel
  fehlschlagen (`AssertionError`), nach Entfernen wieder grün – die Regel greift wirklich.
- [ADR 0020](../adr/0020-domain-in-model-und-services-teilen.md) geschrieben; Package-Struktur
  in `.claude/skills/architecture.md` und README (Projektstruktur, Testliste) nachgezogen.
  ADR 0012/0019 bleiben unverändert – sie halten den damaligen Stand fest.
- Definition of Done: `clean build` grün (ArchUnit 6/6), `test` grün, `systemtest` 15/15,
  `e2eTest` grün (`.last-run.json`: `"status": "passed"`, keine fehlgeschlagenen Tests).

## 77. Prompt: „warte auf den build und committe dann"

_Delta: 1:13 · 9.322 out · 1.644.108 gesamt_
_Stand (Session, 5 Prompts): 10:09 · 53.111 out · 4.974.240 gesamt_

**Aktionen:**
- Auf das Ende des DoD-Laufs gewartet (9m 8s): `BUILD SUCCESSFUL`.
- Definition of Done erfüllt: `clean build` grün (31 Java-Tests, 0 Fehler – einer mehr
  als zuvor, nämlich die neue Zwiebel-Regel; Vitest lief im `build` mit, Frontend
  unverändert), `systemtest` 15/15, `e2eTest` 9/9.
- Log ergänzt (Einträge 75–77), dann committet (Author = Claude):
  `Check the architecture as onion rings too`.
- Kein Hash im Eintrag: Er entstünde erst mit dem Commit, der genau diesen Text enthält –
  dieselbe Begründung wie in Eintrag 74.

---

## 76. Prompt: „commit"

_Delta: 1:28 · 7.333 out · 764.087 gesamt_
_Stand (Session, 4 Prompts): 8:56 · 43.789 out · 3.330.132 gesamt_

**Aktionen:**
- **Nicht** committet: Der DoD-Lauf lief noch (Gradle-Daemon aktiv, e2e-Artefakte wurden
  gerade geschrieben). Ein Commit vor grüner Definition of Done wäre ein Regelverstoß
  gegen `.claude/skills/develop.md`.
- Stattdessen die Commit-Message vorgelegt; vom Nutzer bestätigt.
- Abweichung beim Trailer benannt und bewusst entschieden: `develop.md` schreibt
  `Claude Opus 4.8 (1M context)` vor, diese Session läuft auf **Opus 5**. Der Nutzer hat
  die zutreffende Angabe bestätigt – der Skill-Text ist an dieser Stelle veraltet.

---

## 75. Prompt: „Ergänze einen archunit Test mit onionArchitecture()"

_Delta: 7:28 · 36.456 out · 2.566.045 gesamt_
_Stand (Session, 3 Prompts): 7:28 · 36.456 out · 2.566.045 gesamt_

**Aktionen:**
- `OnionArchitectureTest` in `modules/backend/bootstrap/src/test/.../architecture/`
  angelegt: eine Regel `rings_are_respected` auf Basis von `onionArchitecture()`.
  Ringe: `..domain..` als *domain model*, `..application..` (Ports + Services) als
  *application service*, dazu drei Adapter `rest`, `persistence`, `bootstrap`.
- `withOptionalLayers(true)` gesetzt – es gibt keinen *domain service*-Ring, die Domäne
  ist reines Modell. Ohne das Flag scheitert ArchUnit am leeren Ring statt an einer
  Abhängigkeit.
- Vor dem Schreiben geprüft, wie `onionArchitecture()` in ArchUnit 1.5.0 tatsächlich
  arbeitet (Bytecode der `Architectures$OnionArchitecture`): sie delegiert an
  `layeredArchitecture()` mit `consideringAllDependencies` – deshalb muss `bootstrap`
  ein eigener Ring sein und darf nicht unbenannt bleiben.
- **Wirksamkeit nachgewiesen** (Maßstab aus ADR 0012): Temporärer Record im REST-Adapter,
  der den generierten jOOQ-Code des Persistenz-Adapters anfasst → `rings_are_respected`
  meldet 5 Verstöße, während die 5 Regeln aus `LayeredArchitectureTest` **grün bleiben**
  (6 Tests, 1 Fehler). Damit ist belegt, dass die Regel eine echte Lücke schließt: Die
  Schichtensicht kennt nur *eine* Schicht `..adapter..`. Probe wieder entfernt.
- Nebenbefund aus der Fehlermeldung: ArchUnit sieht generische Typargumente in Feldern,
  Parametern und Rückgabetypen sehr wohl. Die Einschränkung in ADR 0012 ist enger zu
  fassen als dort formuliert – als Nachtrag in ADR 0019 festgehalten.
- Doku: [ADR 0019](../adr/0019-onion-architecture-regel-in-archunit.md) neu,
  `README.md` und `.claude/skills/architecture.md` um die zweite Sicht ergänzt.

---

## 74. Prompt: „commit"

_Delta: 28s · 2.212 out · 2.309 in (neu) · 401.386 gesamt_
_Stand (Session, 4 Prompts): 23:26 · 65.323 out · 4.632.597 gesamt_

**Aktionen:**
- Definition of Done vor dem Commit erfüllt: `clean build` grün (30 Java, 24 Vitest),
  `systemtest` 15/15, `e2eTest` 9/9 – alle Läufe aus Eintrag 73, seither keine
  Code-Änderung.
- Commit-Message vom Nutzer bestätigt, dann committet (Author = Claude):
  `Move dependencies to the version catalog` (10 Dateien).
- Diesen Eintrag nachträglich ergänzt und per `--amend` in denselben Commit übernommen,
  damit das Log den Stand beschreibt, den der Commit enthält. Deshalb steht hier
  ausnahmsweise **kein** Hash: Er ändert sich mit jedem Amend und wäre sofort veraltet.

---

## 73. Prompt: „Stelle die gradle Dependencies um auf den Versionskatalog libs.versions.toml. Ergänze ein entsprechendes ADR."

_Delta: 22:16 · 58.048 out · 240.721 in (neu) · 3.842.517 gesamt_
_Stand (Session, 3 Prompts): 22:16 · 58.048 out · 3.842.517 gesamt_

**Aktionen:**
- `gradle/libs.versions.toml` angelegt: 22 Bibliotheken und 4 Plugins. Die vom
  Spring-Boot-BOM verwalteten Einträge stehen **ohne** Version (`{ module = "…" }`), nur
  die selbst gepflegten Versionen (jqwik, springdoc, jOOQ, Zonky, ArchUnit, Plugins) im
  `[versions]`-Block.
- Alle fünf Buildskripte umgestellt: Plugins über `alias(libs.plugins.…)`, Abhängigkeiten
  über `libs.…`. Die dreifach gepflegte jOOQ-Version im Adapter greift jetzt auf
  `libs.versions.jooq.get()` zurück.
- Fallstrick: `libs` ist in `subprojects { }` nicht verfügbar („Extension with name 'libs'
  does not exist"), weil die Katalog-Erweiterung des Untermoduls erst mit dessen eigener
  Auswertung entsteht. Gelöst mit `rootProject.libs`.
- Auf Port 8080 lief noch eine Altinstanz aus einer früheren Sitzung; vor `e2eTest`
  beendet (Lehre aus ADR 0017).
- Nachweis der Gleichwertigkeit: `dependencies` für alle vier Module vorher (aus einem
  `git worktree` auf `HEAD`) und nachher – 3.019 Zeilen, `diff` ohne Unterschied.
- `./gradlew clean build` grün (30 Java, 24 Vitest), `systemtest` 15/15, `e2eTest` 9/9.
- [ADR 0018](../adr/0018-versionskatalog-libs-versions-toml.md) ergänzt (Kontext, vier
  Optionen, Entscheidung, Fallstricke, Nachweis, Regeln); README um den Katalog im
  Projektbaum ergänzt.

---

## 72. Prompt: „commit"

_Delta: 8s · 682 out · 8.060 in (neu) · 254.330 gesamt_
_Stand (Session, 4 Prompts): 5:00 · 28.311 out · 2.099.579 gesamt_

**Aktionen:**
- Vorbedingung: reine Doku-Änderung an `claudeLog.md`, kein Code – der letzte grüne Lauf
  aus Eintrag 68 (`clean build` 30 Java + 24 Vitest, `systemtest` 15/15, `e2eTest` 9/9)
  gilt weiter.
- Eintrag 71 und diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Log the five commits from before the log existed`.

---

## 71. Prompt: „Ergänze die ersten commits des Branches chronologisch umgekehrt am Ende von claudeLod.md bis zu dem Punkt, ab welchem sie schon im claudeLod.md enthalten sind"

_Delta: 4:52 · 27.629 out · 105.952 in (neu) · 1.845.249 gesamt_
_Stand (Session, 3 Prompts): 4:52 · 27.629 out · 1.845.249 gesamt_

**Aktionen:**
- Anschlusspunkt bestimmt: `git log --reverse` gegen das Log gehalten – Eintrag 1
  („Start die Java-Anwendung!") ist Commit `c5411b2`; davor liegen fünf im Log
  unerfasste Commits vom 18.07.2026.
- **Rückfrage zur Nummerierung statt Raten:** Die naheliegende Renummerierung
  (bestehende 1–70 → 6–75) hätte die internen Querverweise im Log gebrochen
  („Eintrag 68", „Einträge 55/56", „Eintrag 53", „Eintrag 39"). Entscheidung des
  Nutzers: ab 1 **abwärts** weiterzählen.
- Fünf Einträge am Dateiende ergänzt, chronologisch absteigend wie der Rest:
  - **0** `d02045e` „Wieso hast du keine ADRs erstellt?" – ADR 0001–0003 nachgeholt,
    zwei davon als bewusste Abweichung von den Skills markiert.
  - **-1** `13c4737` Backend-Erstellung – hexagonale Struktur, REST-CRUD, jOOQ/Flyway,
    vier Testebenen; ohne ADRs, was direkt zu Eintrag 0 führt.
  - **-2** `bb6db40`, **-3** `4d8be77`, **-4** `b0dd66f` – reine Nutzer-Commits ohne
    Prompt, deshalb mit `Commit:` statt `Prompt:` überschrieben.
- Inhalte aus den Commits selbst rekonstruiert (Diffs, ADR-Texte, `README.md`,
  `build.gradle`), nicht aus dem Gedächtnis. Ein Hinweisblock vor den Einträgen hält fest,
  dass sie Rekonstruktionen sind und dass es für sie **keine** Kennzahlen gibt – die
  Transkripte dieser Turns existieren nicht mehr, geschätzt wird nicht.
- Bestehende Zeilen blieben unangetastet: der Diff besteht ausschließlich aus Ergänzungen.

---

## 70. Prompt: „commit"

_Delta: 10s · 2.148 out · 18 in (neu) · 181.503 gesamt_
_Stand (Session, 4 Prompts): 1:38 · 16.921 out · 1.153.574 gesamt_

**Aktionen:**
- Vorbedingung: reine Regel-/Doku-Änderung (`develop.md`, `claudeLog.md`), kein Code –
  der letzte grüne Lauf aus Eintrag 68 (`clean build` 30 Java + 24 Vitest,
  `systemtest` 15/15, `e2eTest` 9/9) gilt weiter.
- Commit-Body auf **Deutsch** geschrieben statt wie im Vorschlag auf Englisch: Alle
  bisherigen Commits führen einen englischen Betreff mit deutschem Fließtext, und
  `CLAUDE.md` verlangt deutsche Kommunikation. Der Betreff blieb unverändert.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Clear the context after each commit`.
- Erste Anwendung der neuen Regel: Die Antwort endet mit der Aufforderung zum `/clear`.

---

## 69. Prompt: „/clear" / „Do /clear after each commit"

_Delta: 31s · 4.639 out · 105.339 in (neu) · 337.591 gesamt_
_Stand (Session, 3 Prompts): 31s · 4.639 out · 337.591 gesamt_

**Aktionen:**
- Regel in `.claude/skills/develop.md`, Abschnitt „Commits", aufgenommen: Nach **jedem**
  Commit fordert der Assistent als letzte Zeile der Antwort zum `/clear` auf.
- **Grenze benannt statt umgangen:** `/clear` ist ein Client-Befehl von Claude Code.
  Weder der Assistent noch ein Hook (`PostToolUse`, `Stop`, …) kann den Kontext leeren –
  die Aufforderung an den Nutzer ist deshalb der verbindliche Teil der Regel, nicht eine
  Notlösung. Ein Hook könnte allenfalls an die Regel erinnern, sie nicht ausführen.
- Zwei Konsequenzen mitdokumentiert, weil sie sonst später als Fehler auffallen:
  - Alles, was den Commit überdauern soll, muss **vorher** in einer Datei stehen
    (Log, ADR, README) – nach `/clear` ist der Gesprächsverlauf weg.
  - `/clear` startet ein neues Transkript, also beginnt der **Stand** der Kennzahlen bei
    null. Direkt an diesem Eintrag sichtbar: 3 Prompts statt 89 wie in Eintrag 68.
    Im Abschnitt „Dauer und Tokenanzahl" ergänzt – eine Session umfasst künftig in der
    Regel genau die Arbeit seit dem letzten Commit.
- Kein Code angefasst, reine Regel-/Doku-Änderung; der letzte grüne Lauf aus Eintrag 68
  (`clean build` 30 Java + 24 Vitest, `systemtest` 15/15, `e2eTest` 9/9) gilt weiter.

---

## 68. Prompt: „commit" / „start" / „commit"

_Delta: 2:41 · 7.381 out · 9.185 in (neu) · 15.814.811 gesamt_
_Stand (Session, 89 Prompts): 6:40:04 · 1.012.570 out · 530.666.585 gesamt_

**Aktionen:**
- Beim ersten „commit" lief die Verifikationskette noch im Hintergrund – **nicht** vorschnell
  committet, sondern das Ergebnis abgewartet: `clean build` grün (30 Java + 24 Vitest),
  `systemtest` 15/15, `e2eTest` 9/9.
- Anwendung gestartet (PID 2286) und die Routen einzeln geprüft: `/recipes/`, `/recipes/new`,
  `/recipes/1`, `/recipes/1/edit` je 200; `/recipes/api/recipes` und `/recipes/v3/api-docs`
  200, `/recipes/swagger-ui.html` 302, `/recipes/gibt-es-nicht` **404**.
  Screenshot von `/recipes/1` bestätigt: Der direkte Aufruf einer geteilten Adresse öffnet
  die Detailansicht mit Zutaten und Schritten.
- Diesen Eintrag ergänzt, dann committet (Author = Claude):
  `Add routing with an SPA fallback for shareable recipe URLs`.

---

## 67. Prompt: „Ergänze einen Router mit SPA-Fallback für teilbare Rezept-URLs"

_Delta: 47:15 · 58.448 out · 2.362.346 in (neu) · 50.126.519 gesamt_
_Stand (Session, 86 Prompts): 6:37:23 · 1.005.189 out · 514.851.774 gesamt_

**Aktionen:**
- `react-router` 8 eingeführt; `basename={import.meta.env.BASE_URL}` erbt den Context-Path
  aus ADR 0016, ohne ihn zu kennen. Routen: `/`, `/new`, `/{id}` (teilbar), `/{id}/edit`.
- `App.tsx` auf den Router reduziert, die Ladelogik in drei neue Seiten unter `src/pages/`
  verschoben. Der Rezepttitel ist jetzt ein echtes `<a href>` statt eines Buttons –
  Mittelklick und „in neuem Tab öffnen" funktionieren damit ohne Zusatzarbeit.
- Serverseitig `SpaForwardingController` in `:modules:backend:adapter`: leitet **genau die
  vier Routen** auf `forward:/index.html` weiter. Bewusst **kein** Catch-all, das API,
  OpenAPI oder Swagger UI hätte verschatten können.
- Tests nachgezogen: Vitest-Tests auf `getByRole('link')` umgestellt, zwei neue E2E-Tests
  (direkter Aufruf einer geteilten Adresse; unbekannte ID meldet sich statt leer zu bleiben),
  zwei neue Systemtests (Fallback greift / Backend bleibt unverschattet).
- **Zwei Fallen, beide selbst hineingelaufen:**
  1. `BrowserRouter` nutzt die echte `window.history`, die jsdom über alle Tests einer Datei
     teilt – nach dem ersten navigierenden Test starteten die folgenden auf der falschen
     Route und stürzten ab. Behoben mit `window.history.pushState({}, '', '/')` im `beforeEach`.
  2. Playwrights `reuseExistingServer` verwendete die noch laufende **alte** Instanz auf Port
     8080 – sieben E2E-Fehlschläge, die wie ein Router-Problem aussahen. Der Fehler-Screenshot
     (Titel als `button` statt `link`) war der entscheidende Hinweis; nach dem Beenden der
     Altinstanz liefen 9/9 grün.
- `docs/adr/0017-router-mit-spa-fallback.md` angelegt, README und `CLAUDE.md` ergänzt.

---

## 66. Prompt: „commit" / „Ja, so committen"

_Delta: 6s · 155 out · 11 in (neu) · 746.115 gesamt_
_Stand (Session, 84 Prompts): 5:48:31 · 944.080 out · 458.740.047 gesamt_

**Aktionen:**
- Vorbedingung erfüllt (nach der Umstellung gemessen): `./gradlew clean build` grün
  (30 Java + 24 Vitest), `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7, dazu die
  manuelle Prüfung der vier URLs unter `/recipes` und 404 auf dem alten Pfad.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Serve the application under the /recipes context path`.

---

## 65. Prompt: „Ändere die Applikations Basis-URL auf \"recipes\"" / „retry"

_Delta: 35:18 · 32.095 out · 3.567.310 in (neu) · 41.613.717 gesamt_
_Stand (Session, 82 Prompts): 5:47:28 · 941.070 out · 455.013.588 gesamt_

**Aktionen:**
- Rückfrage gestellt, weil „Basis-URL" zwei Lesarten zuließ (Context-Path der Anwendung
  vs. REST-Basispfad). Gewählt: **Context-Path** – SPA, API und Swagger UI liegen unter
  `/recipes`.
- `server.servlet.context-path: /recipes` in `application.yml`.
- Frontend ohne absolute Pfade gehalten: `base: '/recipes/'` in `vite.config.ts`, der
  API-Basispfad wird daraus über `import.meta.env.BASE_URL` abgeleitet. Dafür `vite/client`
  in die tsconfig-`types` aufgenommen (`tsc` kannte `import.meta.env` sonst nicht).
- **Fallstrick beim Dev-Proxy:** Ein Proxy auf `/recipes` würde die SPA verschlucken, die
  Vite unter derselben Basis ausliefert. Deshalb gezielt `/recipes/api`, `/recipes/v3`
  und `/recipes/swagger-ui`.
- Systemtests: `RunningApplication.CONTEXT_PATH` hängt den Pfad einmal an die Basis-URL an,
  die Tests adressieren weiter fachliche Pfade. `SwaggerSystemTest` prüft jetzt, dass
  `configUrl` und `url` den Context-Path tragen; `FrontendSystemTest` schneidet ihn aus den
  Asset-Referenzen, weil `HttpProbe` ihn bereits voranstellt.
- E2E: `playwright.config.ts` exportiert `CONTEXT_PATH`, die Specs bilden daraus `APP`.
- **Stille Bombe entschärft:** `npm run generate:api` zeigte noch auf
  `http://localhost:8080/v3/api-docs` – jetzt 404. Auf den Context-Path korrigiert und
  gegengeprüft: Der generierte Contract ist **unverändert**, die Umstellung betrifft nur
  die Basis-URL, nicht die API-Struktur.
- Beim ersten `e2eTest`-Lauf scheiterte Playwrights `webServer`: Port 8080 war von einer
  Instanz aus einem früheren Turn belegt. Gezielt über die PID beendet (nicht `pkill -f`,
  das trifft die eigene Kommandozeile).
- Verifiziert an der laufenden Anwendung: `/recipes/` 200 (SPA lädt inkl. Daten, per
  Screenshot geprüft), `/recipes/api/recipes` 200, `/recipes/swagger-ui.html` 302,
  `/recipes/v3/api-docs` 200 – und `/api/recipes` ohne Präfix **404**, der alte Pfad ist
  wirklich weg. Dazu `clean build` grün (30 Java + 24 Vitest), `systemtest` 13/13,
  `e2eTest` 7/7.
- `docs/adr/0016-context-path-recipes.md` angelegt, mit einer Tabelle der **fünf** Orte,
  an denen der Context-Path gespiegelt wird – laufen sie auseinander, bricht nichts im
  Build, sondern erst zur Laufzeit als 404.

---

## 64. Prompt: „commit" / „Ja, so committen"

_Delta: 5s · 155 out · 11 in (neu) · 711.988 gesamt_
_Stand (Session, 80 Prompts): 5:11:10 · 902.175 out · 408.398.997 gesamt_

**Aktionen:**
- Vorbedingung erfüllt (nach der Umbenennung gemessen): `./gradlew clean build` grün
  (30 Java + 24 Vitest), `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7 –
  inklusive des neuen Artefaktnamens `recipes-0.0.1-SNAPSHOT.jar`.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Rename the project to recipes`.

---

## 63. Prompt: „Ändere dem Projektnamen von \"recipe-backend\" auf \"receipes"

_Delta: 13:59 · 9.671 out · 1.396.550 in (neu) · 10.608.423 gesamt_
_Stand (Session, 78 Prompts): 5:10:19 · 899.904 out · 404.841.951 gesamt_

**Aktionen:**
- **Rückfrage vor der Umsetzung:** „receipes" ist keine englische Vokabel und wich von
  allem Bestehenden ab (Datenbank `recipes`, REST-Pfad `/api/recipes`, Tabelle `recipe`).
  Da der Name in Artefaktnamen und `spring.application.name` einfließt, wäre eine spätere
  Korrektur ein zweiter Durchlauf gewesen. Entscheidung des Nutzers: **`recipes`**.
- Operative Stellen geändert:
  - `settings.gradle.kts`: `rootProject.name = "recipes"`
  - `modules/backend/bootstrap/build.gradle.kts`: `artifactName = "recipes"`
  - `application.yml`: `spring.application.name: recipes`
  - `README.md`: Titel, Strukturbaum, Jar-Pfade
  - `CLAUDE.md`: Platzhalter `[Projektname]` durch `recipes` ersetzt
- ADRs und ältere Log-Einträge **nicht** umgeschrieben – sie dokumentieren den Stand ihrer
  Entstehung (Konvention aus ADR 0009).
- Kein ADR angelegt: eine Umbenennung ist keine Architekturentscheidung.
- Verifiziert: `./gradlew projects` meldet `Root project 'recipes'`, `clean build` grün
  (30 Java + 24 Vitest), Artefakt heißt jetzt `recipes-0.0.1-SNAPSHOT.jar` und enthält die
  drei Modul-Jars, `systemtest` 13/13, `e2eTest` 7/7. Die Playwright-Konfiguration brauchte
  **keine** Anpassung – sie filtert auf `*.jar` ohne `-plain` statt auf einen festen Namen.

---

## 62. Prompt: „Was soll modules-broken?" / „commit"

_Delta: 32s · 1.000 out · 2.496 in (neu) · 2.086.775 gesamt_
_Stand (Session, 76 Prompts): 4:54:56 · 887.409 out · 390.039.896 gesamt_

**Antwort/Aktionen:**
- Klargestellt: `modules-broken` hat **keinen Zweck** – es ist ein Rest meines eigenen
  Wiederherstellungsversuchs (`mv modules modules-broken`), kein Projektbestandteil.
- **Zweiter Dateiverlust, diesmal von mir ausgelöst:** Die Löschversuche
  (`rm -rf`, `rmdir`, `find -delete`, `mv`) an `modules-broken` haben `modules/backend`
  erneut zerstört – Operationen auf dem Phantom schlagen auf das benachbarte `modules`
  durch. 40 Dateien fehlten im Arbeitsverzeichnis.
- Wiederhergestellt mit `git restore --worktree -- modules/backend`. **Zweiter Fallstrick:**
  `git restore` holt aus dem **Index**, und der enthielt die Buildskripte im Stand vor
  meinen `sed`-Anpassungen – `:backend:application` statt `:modules:backend:application`.
  Der Build brach mit „Project with path ':backend:application' could not be found" ab;
  die Anpassungen mussten erneut eingetragen werden.
- Danach `./gradlew clean build` grün, 68 Quelldateien vollständig.
- **Entscheidung: nicht weiter anfassen.** Der Eintrag ist inert (0 Treffer in allen
  Build-Logs, keine Git-Änderung); einzige Auswirkung ist eine Warnzeile bei `git status`.
  Aus User Space nicht entfernbar – `rmdir` meldet „Directory not empty", `stat` gleichzeitig
  „No such file or directory". Verschwindet erst mit Neustart der Umgebung oder per `sudo`.
  Der belegte Schaden weiterer Versuche überwiegt den kosmetischen Nutzen.

---

## 61. Prompt: „Bewege die gradle Module backend und frontend als Untermodule in ein neues \"modules\""

_Delta: 21:50 · 64.616 out · 82.628 in (neu) · 46.694.974 gesamt_
_Stand (Session, 74 Prompts): 4:44:22 · 869.664 out · 372.749.163 gesamt_

**Aktionen:**
- **Vorfall im ersten Anlauf – Dateiverlust im Arbeitsverzeichnis, ohne Datenverlust:**
  `git mv frontend modules/frontend` meldete Erfolg, das Ziel war aber **leer**;
  `git status` zeigte 24 Einträge als `RD` (im Index umbenannt, im Arbeitsverzeichnis
  fehlend). Der Verzeichniseintrag wurde zum **Phantom**: `cd` gelang, aber `stat`, Lesen,
  Schreiben, `rmdir`, `rm -rf` und `mv` schlugen alle mit „No such file or directory" fehl
  (`ls -l` zeigte `d????????? ? ? ? ?`). Erst das Umbenennen des Elternverzeichnisses löste
  ihn auf – und nahm das bereits korrekt verschobene `backend/` mit.
- Wiederherstellung: `git fsck` ohne beschädigte Objekte, `git reset --hard HEAD` stellte
  alle 64 Quelldateien wieder her, Build danach grün. **Weil alles committet war, war das
  ein Ärgernis und kein Datenverlust.**
- Ursache eingegrenzt: `frontend/` enthielt **5836 Dateien**, überwiegend `node_modules`.
  Nach dem Löschen der generierten Verzeichnisse blieben **24** übrig – derselbe `git mv`
  lief dann fehlerfrei durch. Das Umbenennen großer Bäume ist auf dem Overlay-Dateisystem
  dieser Umgebung (WSL2) offenbar nicht verlässlich.
- Zweiter Anlauf erfolgreich: `backend/` (41 Dateien) und `frontend/` (24) nach `modules/`;
  Projektpfade `:modules:backend:{domain,application,adapter,bootstrap}`.
- Angepasst: `settings.gradle.kts`, Wurzel-Buildskript, drei `project(":…")`-Verweise,
  `rootProject.file("modules/frontend")`, README, `CLAUDE.md`, Architektur-Skill.
  Die **Playwright-Pfade blieben unverändert** – beide Bausteine wanderten zusammen, die
  relativen Pfade zueinander stimmen weiter.
- Verifiziert: `./gradlew clean build` grün (30 Java + 24 Vitest), Artefakt weiterhin
  `recipe-backend-0.0.1-SNAPSHOT.jar` mit SPA und drei Modul-Jars,
  `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7, 68 Quelldateien unversehrt.
- `docs/adr/0015-alle-bausteine-unter-modules.md` angelegt – mit dem Vorfall und drei
  Regeln daraus: generierte Inhalte vor dem Verschieben löschen, Dateizahl am Ziel prüfen
  statt dem Rückgabewert zu vertrauen, vor Umbauten committen.
- **Offener Rest:** Im Wurzelverzeichnis liegt ein nicht entfernbarer Phantom-Eintrag
  `modules-broken`. Git ignoriert ihn (nicht lesbar, kein Inhalt), er lässt sich aus User
  Space aber nicht löschen und verschwindet vermutlich erst mit einem Neustart der Umgebung.

---

## 60. Prompt: „commit" / „Ja, so committen"

_Delta: 6s · 148 out · 11 in (neu) · 634.678 gesamt_
_Stand (Session, 73 Prompts): 4:21:47 · 803.588 out · 323.506.093 gesamt_

**Aktionen:**
- Vorbedingung erfüllt (nach der Verschiebung gemessen): `./gradlew clean build` grün
  (30 Java + 24 Vitest), `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.
- Git erkennt 39 Dateien als Umbenennung – die Historie der Schichtmodule bleibt erhalten.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Group the layer modules under :backend`.

---

## 59. Prompt: „Bewege die Module \"bootstrap → adapter → application → domain\" als Untermodule in ein neues backend Modul"

_Delta: 8:32 · 20.014 out · 23.315 in (neu) · 14.272.774 gesamt_
_Stand (Session, 71 Prompts): 4:16:59 · 800.883 out · 319.704.624 gesamt_

**Aktionen:**
- Die vier Schichtmodule per `git mv` nach `backend/` verschoben (39 Umbenennungen erkannt);
  Projektpfade lauten jetzt `:backend:domain`, `:backend:application`, `:backend:adapter`,
  `:backend:bootstrap`.
- **Gemeinsame Konfiguration mitverschoben:** Sie stand im Wurzelskript, betraf aber nur die
  Schichten – jetzt in `backend/build.gradle.kts` (Toolchain, JaCoCo, Spring-BOM,
  `-parameters`, JUnit-Platform, Test-Basisabhängigkeiten). Das Wurzelskript enthält nur
  noch die Plugin-Deklarationen, die auf dem Buildscript-Classpath liegen müssen.
- `:backend` ist ein reines Containermodul: kein Java-Plugin, keine Quellen – verifiziert
  daran, dass **kein** `backend/build/`-Verzeichnis entsteht.
- Mitgezogen, was der Compiler nicht findet: `project(":…")`-Pfade in drei Buildskripten,
  die Playwright-Konfiguration (`../backend/bootstrap/build/{libs,e2e}`) sowie die
  Pfadangaben in README, `CLAUDE.md` und `.claude/skills/architecture.md`.
- Gegenprobe nach der Verschiebung wiederholt: Ein Record in `:backend:application`, der
  einen REST-DTO importiert, **compiliert nicht**; `:backend:domain` meldet weiterhin
  `No dependencies` auf dem Compile-Classpath.
- Verifiziert: `./gradlew projects` zeigt `:backend` mit vier Untermodulen,
  `./gradlew clean build` grün (30 Java + 24 Vitest), Boot-Jar unverändert benannt und mit
  SPA sowie den drei Modul-Jars, `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.
- `docs/adr/0014-schichtmodule-unter-backend.md` angelegt.

---

## 58. Prompt: „commit" / „commit"

_Delta: 12s · 546 out · 682 in (neu) · 1.831.603 gesamt_
_Stand (Session, 70 Prompts): 4:07:43 · 779.092 out · 302.983.291 gesamt_

**Aktionen:**
- Beim Stagen fiel `docs/log/__pycache__/` auf – Nebenprodukt des Modul-Imports beim
  Nachrechnen der Einträge 55/56. In `.gitignore` aufgenommen und entfernt.
- Vorbedingung: reine Doku- und Werkzeugänderung, kein Produktionscode – letzter grüner
  Lauf (`clean build` 30 Java + 24 Vitest, `systemtest` 13/13, `e2eTest` 7/7) gilt weiter.
- Der Fallback „noch nicht ermittelbar" griff in diesem Turn tatsächlich: Beim ersten
  Aufruf lag noch keine Antwort im Transkript. Nach kurzem Warten lieferte das Skript
  die Werte – der Schutz gegen Null-Werte im Protokoll bewährt sich damit im Alltag.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Log the per-prompt delta against the running total`.

---

## 57. Prompt: „Ergänze Dauer und Tokenanzahl immer im claudeLog.md auch für einen Prompt als Delta zu letzten Stand."

_Delta: 2:10 · 19.657 out · 12.400 in (neu) · 9.052.479 gesamt_
_Stand (Session, 68 Prompts): 4:06:04 · 774.871 out · 295.669.556 gesamt_

**Aktionen:**
- Format auf **zwei** Zeilen erweitert: `Delta` (dieser Prompt) und `Stand` (kumuliert über
  die Session). Der Prompt-Aufwand ist damit als Delta zum vorigen Stand lesbar.
- `turn-stats.py` um `cumulative()`, `seconds()` und `format_seconds()` ergänzt;
  `--log-line` gibt beide Zeilen aus, die Tabelle zwei zusätzliche Stand-Spalten.
- Zwei bewusste Festlegungen, im Skill dokumentiert:
  - Die **Stand-Dauer ist die Summe der Turn-Dauern**, nicht die Zeit seit Session-Beginn –
    Wartezeit auf den Nutzer gehört nicht zum Aufwand.
  - Der **Stand gilt je Session**: Das Transkript umfasst nur die laufende Session, Prompts
    aus früheren Sessions fehlen. Deshalb steht die Prompt-Zahl der Session in der Zeile;
    sie weicht bewusst von der Nummer des Log-Eintrags ab (68 vs. 57).
- Einträge 55 und 56 auf das neue Format nachgezogen – und dabei mit den **vollständigen**
  Turn-Werten, die rückblickend verfügbar sind. Das korrigiert die dort dokumentierte
  Untererfassung: Eintrag 55 stand mit 3:06/22.916, tatsächlich waren es 3:42/25.154.
- `.claude/skills/develop.md`: Format, Erläuterungen und Vorlage angepasst.

---

## 56. Prompt: „commit" / „Ja, so committen"

_Delta: 1:35 · 5.223 out · 3.663 in (neu) · 7.143.965 gesamt_
_Stand (Session, 67 Prompts): 4:03:54 · 755.214 out · 286.617.077 gesamt_

**Aktionen:**
- Erster Praxiseinsatz des Skripts deckte eine Lücke auf: Wird es ganz früh im Turn
  aufgerufen, ist noch keine Assistant-Nachricht im Transkript und die Ausgabe lautete
  `Dauer: 0s · Tokens: 0` – eine Falschaussage. `log_line()` meldet diesen Fall jetzt
  ausdrücklich als „noch nicht ermittelbar".
- Vorbedingung: reine Doku- und Werkzeugänderung, kein Produktionscode – letzter grüner
  Lauf (`clean build` 30 Java + 24 Vitest, `systemtest` 13/13, `e2eTest` 7/7) gilt weiter.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Log duration and token count per prompt`.

---

## 55. Prompt: „Ergänze Dauer und Tokenanzahl immer im claudeLog.md"

_Delta: 3:42 · 25.154 out · 21.955 in (neu) · 14.047.520 gesamt_
_Stand (Session, 65 Prompts): 4:02:19 · 749.991 out · 279.473.112 gesamt_

**Aktionen:**
- Ausgangsproblem benannt: Dauer ist messbar, den eigenen Tokenverbrauch kenne ich als
  Assistent **nicht** – geschätzte Zahlen in einem dauerhaften Protokoll wären wertlos.
- Quelle gefunden: Claude Code schreibt je Session ein Transkript unter
  `~/.claude/projects/<projekt>/<session>.jsonl`; jeder Eintrag trägt `timestamp`, jede
  Assistant-Nachricht ihre `usage` (input, output, cache_creation, cache_read).
- `docs/log/turn-stats.py` angelegt: gruppiert das Transkript in Turns (Grenze = echter
  Nutzer-Prompt, erkannt daran, dass `message.content` ein String ist – Tool-Ergebnisse und
  Skill-Injektionen sind Listen), summiert die Tokens und rechnet die Dauer aus.
  `--log-line` liefert die fertige Markdown-Zeile, `-n` eine Übersicht.
- Gegenprobe der Plausibilität an bekannten Turns: der Modul-Split (Eintrag 53) kommt auf
  37:48 und 50.604 Output-Tokens, die kurzen Commit-Turns auf 20–45 s und ~1.200–1.700.
- `.claude/skills/develop.md` erweitert: Kennzahlenzeile ist Teil des Formats, neuer
  Abschnitt „Dauer und Tokenanzahl", Schritt 2 im Ablauf, angepasste Vorlage.
  Ausdrücklich festgehalten: **Zahlen nie schätzen** – ist das Transkript nicht lesbar,
  wird „nicht ermittelbar" vermerkt.
- Ebenfalls dokumentiert, weil systematisch: Gemessen wird bis zum Schreiben des Eintrags,
  die abschließende Antwort fehlt darin zwangsläufig. Die Werte sind eine **Untergrenze**.
- Bestandseinträge bleiben ohne Kennzahlen – die Daten ließen sich zwar nachrechnen, aber
  das Log dokumentiert, was zum jeweiligen Zeitpunkt bekannt war.

---

## 54. Prompt: „commit" / „Ja, so committen"

**Aktionen:**
- Vor dem Stagen geprüft: Beide temporären Proben (`ArchViolationProbe`, `DirectionProbe`)
  sind entfernt; der verbleibende `HttpProbe` ist der reguläre Systemtest-Helfer.
  Git erkennt 35 Dateien als Umbenennung.
- Vorbedingung erfüllt: `./gradlew clean build` grün (30 Java-Tests über drei Module,
  24 Vitest), `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Split the layers into one Gradle module each`.

---

## 53. Prompt: „Bewege bootstrap → adapter → application → domain in gradle-Module."

**Aktionen:**
- Vorab geprüft, was den Zuschnitt bestimmt: `@SpringBootApplication(scanBasePackages =
  "io.github.keymaster65.helloai")` funktioniert modulübergreifend, und **`domain` hat null
  Framework-Imports** – das Modul wird damit komplett abhängigkeitsfrei.
- Vier Module per `git mv` erzeugt (Historie erhalten), `backend/` aufgelöst:
  - `:domain` – ohne **jede** Produktionsabhängigkeit (`compileClasspath` = `No dependencies`).
  - `:application` – `api(:domain)`, dazu nur `spring-context` und `spring-tx`.
  - `:adapter` – `api(:application)`, Web/Validation/jOOQ/springdoc, das Liquibase-Changelog
    und der jOOQ-Codegen.
  - `:bootstrap` – `:adapter`, Boot-Plugin, `application.yml`, Frontend-Tasks, Systemtests,
    Boot-Jar mit `archiveBaseName = "recipe-backend"`.
- Gemeinsames ins Wurzelskript: Toolchain, JaCoCo, JUnit-Platform, Spring-BOM über
  `io.spring.dependency-management`; das Boot-**Plugin** nur in `:bootstrap`.
- Tests neu verteilt: framework-freie Tests bleiben bei ihrer Schicht
  (`RecipeServiceImplTest` → `:application`, `RecipeRestMapperPropertyTest` → `:adapter`);
  alles, was Spring-Context oder App-Klasse braucht, nach `:bootstrap` (Controller-Slice,
  Contract-, Integrations- und Architekturtest, Systemtests).
- **Regressionsfund, den nur die Tests zeigten:** Drei Controller-Tests lieferten 400 statt
  404/204. Ursache: Das Boot-Plugin fügt `-parameters` hinzu und gilt jetzt nur in
  `:bootstrap`; `:adapter` compilierte ohne Parameternamen, sodass `@PathVariable long id`
  nicht mehr binden konnte. Im Wurzelskript für alle Module behoben.
- **Gegenprobe der Struktur:** Ein Record in `:application`, der einen REST-DTO importiert,
  **compiliert nicht** (`package … .adapter.in.rest.dto does not exist`). Die Regel ist damit
  vom Test in den Compiler gewandert. Probe entfernt.
- Playwright-Konfiguration auf `../bootstrap/build/{libs,e2e}` umgestellt; README um die neue
  Projektstruktur, Modulspalte in der Architekturtabelle und Pfade ergänzt;
  `.claude/skills/architecture.md` beschreibt die Regel jetzt als doppelt abgesichert
  (Build + ArchUnit).
- `docs/adr/0013-ein-gradle-modul-je-schicht.md` angelegt; es revidiert ausdrücklich die
  in ADR 0012 getroffene Abwägung und hält die `-parameters`-Falle fest.
- Verifiziert: `./gradlew clean build` grün (30 Java-Tests über drei Module, 24 Vitest),
  Boot-Jar enthält SPA und die drei Modul-Jars unter `BOOT-INF/lib/`,
  `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.

---

## 52. Prompt: „commit" / „Ja, so committen"

**Aktionen:**
- Vor dem Stagen geprüft, dass der temporäre `ArchViolationProbe` wirklich entfernt ist –
  im Domänenpaket liegen nur die vier echten Records.
- Vorbedingung erfüllt: `./gradlew clean build` grün (30 Java + 24 Vitest),
  `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Enforce the layering rule with ArchUnit`.

---

## 51. Prompt: „Ergänze archunit Test für die \"Schichten & Abhängigkeitsregel\""

**Aktionen:**
- `com.tngtech.archunit:archunit-junit5:1.5.0` als `testImplementation` ergänzt.
- Neu `backend/src/test/java/.../architecture/LayeredArchitectureTest.java` mit fünf Regeln:
  - `layeredArchitecture()` über die vier Schichten Domain/Application/Adapter/Bootstrap
    mit `mayOnlyBeAccessedByLayers` bzw. `mayNotBeAccessedByAnyLayer`.
  - zwei explizite Negativregeln für `domain` und `application` – liefern deutlichere
    Fehlermeldungen als die Sammelregel.
  - Framework-Freiheit des Domänenmodells (Spring, jOOQ, Jakarta, Swagger, Jackson,
    Liquibase) – das war bisher überhaupt nicht geprüft.
  - „Ports sind Interfaces".
- Tests per `ImportOption.DoNotIncludeTests` aus der Analyse ausgenommen: Testcode darf in
  jede Schicht greifen, und die Systemtests starten die App bewusst über `bootstrap`.
- **Gegenprobe, dass die Regeln greifen:** temporären Record `ArchViolationProbe` im
  Domänenpaket angelegt, der einen REST-DTO referenziert → `:backend:test` schlägt fehl,
  `layers_are_respected` und `domain_does_not_depend_on_outer_layers` melden je drei
  Verstöße mit Fundstelle. Probe wieder entfernt.
- Bewusst **nicht** umgesetzt: „Controller kennt keine Entities direkt" als Regel über
  Methodensignaturen. ArchUnit sieht Bytecode; Abhängigkeiten in generischen Typargumenten
  (`ResponseEntity<RecipeResponse>`) sind dort nicht zuverlässig sichtbar – die Regel wäre
  eine Scheinprüfung. Im ADR als Einschränkung dokumentiert.
- `docs/adr/0012-archunit-fuer-die-schichtenregel.md` angelegt; README (Tech-Stack + Testarten)
  und `.claude/skills/architecture.md` ergänzt – die Regel dort ist jetzt als ausführbar
  markiert und verweist auf den Test.
- Verifiziert: `./gradlew clean build` grün mit jetzt **30** Java-Tests (5 davon ArchUnit)
  und 24 Vitest, `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.

---

## 50. Prompt: „commit" / „commit" (Bestätigung der vorgelegten Message)

**Aktionen:**
- Beim Stagen fiel `.jqwik-database` auf: Die Wurzel-Variante war bereits eingecheckt,
  durch den Umzug wäre eine zweite Kopie unter `backend/` hinzugekommen. Datei ist
  lokaler jqwik-Zustand (Seeds fehlgeschlagener Samples) – in `.gitignore` aufgenommen
  und beide Kopien per `git rm --cached` ausgetragen.
- Vorbedingung erfüllt (nach dem Umzug gemessen): `./gradlew clean build` grün
  (25 Java + 24 Vitest), `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Move the BFF into the :backend module`.

---

## 49. Prompt: „Bewege das BFF ins gradle Modul \"backend\"."

**Aktionen:**
- Per `git mv` verschoben (Historie bleibt erhalten): `src/` → `backend/src/`,
  `build.gradle.kts` → `backend/build.gradle.kts`. `settings.gradle.kts` bindet
  `include("backend")` ein; neues Wurzel-Buildskript ohne Quellen und Plugins.
- `frontend/` bleibt bewusst an der Wurzel – ein eigenes Gradle-Modul dafür wäre bei vier
  `Exec`-Tasks reine Indirektion (im ADR als verworfene Option 3 vermerkt).
- Angepasste Pfadabhängigkeiten:
  - `frontendDir` → `rootProject.file("frontend")` statt `file("frontend")`.
  - `playwright.config.ts`: Boot-Jar aus `../backend/build/libs`, Artefakte nach
    `../backend/build/e2e`.
  - `.gitignore`: `/build/` → `build/`, sonst wäre `backend/build/` nicht ignoriert worden.
  - Pfadangaben in README und `CLAUDE.md` nachgezogen.
- `archiveBaseName` explizit auf `recipe-backend` gesetzt: Sonst hätte das Artefakt nach dem
  Modulnamen `backend-0.0.1-SNAPSHOT.jar` geheißen und Deployments sowie Doku gebrochen.
- Der jOOQ-Codegen brauchte **keine** Änderung – `project.projectDir` und
  `layout.buildDirectory` zeigen automatisch ins Modul.
- Verifiziert: `./gradlew projects` zeigt `:backend`; `clean build` grün (25 Java-Tests,
  24 Vitest), Jar heißt weiterhin `recipe-backend-0.0.1-SNAPSHOT.jar` und enthält die SPA;
  `systemtest` 13/13, `e2eTest` 7/7 mit 7 Videos und Report unter `backend/build/e2e/`;
  `-PskipFrontend` überspringt weiterhin korrekt.
- Korrektur zu früheren Zusammenfassungen: Die Java-Testzahl ist seit dem `@Valid`-Commit
  **25**, nicht 23 – die beiden neuen Controller-Tests waren in meinen Angaben nicht
  mitgezählt.
- `docs/adr/0011-backend-als-gradle-modul.md` angelegt; README um einen Abschnitt
  „Projektstruktur" erweitert.

---

## 48. Prompt: „commit" / „commit" (Bestätigung der vorgelegten Message)

**Aktionen:**
- Vorbedingung erfüllt: `./gradlew clean build` grün (23 Java + 24 Vitest),
  `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7; zusätzlich der Nachweis,
  dass der generierte jOOQ-Code identisch zum Groovy-Build ist.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Convert the build scripts to the Kotlin DSL`.

---

## 47. Prompt: „Verwende kotlin script für die gradle Dateien. ADR dafür erstellen."

**Aktionen:**
- `build.gradle` → `build.gradle.kts` und `settings.gradle` → `settings.gradle.kts` überführt,
  die Groovy-Dateien entfernt (`git rm`). Inhaltlich **keine** Änderung am Build.
- Drei Stellen ließen sich nicht mechanisch übersetzen:
  - **jOOQ-Codegen:** statt verschachtelter Closures direkter Zugriff auf den JAXB-Baum
    (`jooqConfiguration.apply { … properties = listOf(Property().withKey(…)) }`); die
    Schalter heißen dort `isRecords`/`isPojos`/`isDaos`/`isFluentSetters`.
  - **testsets-Konfigurationen:** `systemtestImplementation` entsteht erst zur
    Konfigurationszeit, hat also keinen typsicheren Accessor → Ansprache über den
    String-Namen `"systemtestImplementation"(…)`.
  - **Gemeinsame Task-Inputs:** Groovys `configure <closure>` ersetzt durch die
    Erweiterungsfunktion `fun Exec.frontendSources()`.
- **Gleichwertigkeit bewiesen statt behauptet:** Vor dem `clean` den vom Groovy-Build
  erzeugten jOOQ-Code gesichert und danach verglichen – `diff -r` über alle 11 Dateien
  **ohne Unterschied**. Das war der aussagekräftigste Test, weil der jOOQ-Block die
  komplexeste Übersetzung war.
- Weiter verifiziert: `./gradlew clean build` grün (23 Java + 24 Vitest), Boot-Jar enthält
  die SPA (5 Einträge unter `BOOT-INF/classes/static/`), `./gradlew systemtest` 13/13,
  `./gradlew e2eTest` 7/7, und `-PskipFrontend` überspringt weiterhin korrekt.
- Zwischendurch trat erneut die bekannte `java.io.IOException`-Flakiness auf (diesmal beim
  Nachladen von `kotlin-stdlib`); Wiederholung genügte.
- `docs/adr/0010-gradle-kotlin-dsl.md` angelegt: Kontext, Optionen, Entscheidung,
  Konsequenzen, die drei Fallstricke und der Nachweis der Gleichwertigkeit.
  `CLAUDE.md` im Tech-Stack auf „Gradle mit Kotlin-DSL" ergänzt.
- ADR 0006 nennt weiterhin `build.gradle`; bleibt unverändert nach der in
  [ADR 0009](0009-gradle-wrapper-verbindlich.md) festgehaltenen Konvention, ADRs nicht
  nachträglich umzuschreiben.

---

## 46. Prompt: „commit" / „Ja, so committen"

**Aktionen:**
- Vorbedingung erfüllt, alles über den Wrapper gefahren: `./gradlew clean build` grün
  (23 Java + 24 Vitest), `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.
- Message vorgelegt und bestätigt bekommen; diesen Eintrag zuerst ergänzt, dann committet
  (Author = Claude): `Require the Gradle wrapper and pin its checksum`.

---

## 45. Prompt: „Verwende den gradle wrapper und ergänze ein entsprechenden ADR"

**Aktionen:**
- Ausgangslage geprüft: Der Wrapper war bereits vollständig eingecheckt
  (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar` + `.properties`, mit `.gitignore`-Ausnahme
  für den Jar). Nur die Doku verwies uneinheitlich auf ein blankes `gradle`.
- **Konkreter Beleg für die Relevanz:** lokal installiert ist **Gradle 9.7.0**, der Wrapper
  pinnt **9.6.1** – `gradle test` und `./gradlew test` liefen hier also nachweislich auf
  verschiedenen Versionen.
- Umgestellt: `CLAUDE.md` (Tech-Stack, Workflow 4–6, Nützliche Befehle, alle vier
  DoD-Punkte), `.claude/skills/testing.md` (Checkliste + Befehle), README-Prosa.
- Dabei zwei sachliche Fehler in `testing.md` mit korrigiert, die in denselben Zeilen standen:
  `-Dtest=…` und `jacoco:report` sind **Maven**-Syntax. Ersetzt durch `--tests "*.Klasse"`
  bzw. den Hinweis, dass JaCoCo per `finalizedBy` ohnehin mitläuft.
- Neues `docs/adr/0009-gradle-wrapper-verbindlich.md` (Kontext, Optionen, Entscheidung,
  Konsequenzen) inkl. Regeln für die Zukunft und dem offenen Punkt, dass
  `distributionSha256Sum` derzeit **nicht** gesetzt ist.
- Bestehende ADRs 0006–0008 bewusst **nicht** umgeschrieben – ein ADR dokumentiert den Stand
  seiner Entstehung; ADR 0009 vermerkt das ausdrücklich.
- Verifiziert über den Wrapper: `./gradlew --version` → 9.6.1, `./gradlew clean build` grün
  (23 Java + 24 Vitest), `./gradlew systemtest` 13/13, `./gradlew e2eTest` 7/7.
- **Nachtrag auf Zuruf:** `distributionSha256Sum` ergänzt
  (`9c0f7fae…9e14`, von `services.gradle.org` bezogen). Beidseitig verifiziert:
  frischer `GRADLE_USER_HOME` lädt und akzeptiert die Distribution; mit verfälschter
  Prüfsumme bricht der Wrapper mit „Verification of Gradle distribution failed!" ab.
  Dabei festgestellt und im ADR vermerkt: Geprüft wird **nur beim Download** – bei bereits
  entpackter Distribution läuft der Build ohne erneute Prüfung durch.
  ADR 0009 um den Abschnitt „Integrität der Distribution" und die Regel erweitert, die
  Prüfsumme bei jedem Versionswechsel mitzupflegen.

---

## 44. Prompt: „commit" / „ja"

**Aktionen:**
- Vor dem Commit ein verirrtes `commit` am Zeilenanfang in `.claude/skills/develop.md`
  entdeckt (zerstörte die Aufzählung unter „## Commits") und nach Rückfrage entfernt.
- Vorbedingung: reine Doku-Änderung, kein Code – letzter grüner Lauf (`clean build`,
  `systemtest` 13/13, `e2eTest` 7/7) gilt weiter.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Point commit rule at the Definition of Done`.

---

## 43. Prompt: „Ersetze die rdundante \"Test vor Commit\" im developer Skill durch einen Bezug auf die DoD,"

**Aktionen:**
- In `.claude/skills/develop.md` den Punkt „Test vor Commit" durch „**DoD vor Commit**"
  ersetzt: Verweis auf die Definition of Done in `CLAUDE.md` als einzige Quelle der
  verbindlichen Prüfschritte. Die Regel selbst (kein Commit bei offenem Punkt) bleibt.
- Die Redundanz war zudem bereits **inhaltlich veraltet**: Der Skill nannte nur
  `gradle test`, während die DoD inzwischen vier Punkte führt (`clean build`, `test`,
  `systemtest`, `e2eTest`) – der Verweis behebt also nicht nur Doppelpflege.
- Zwei Informationen aus dem gelöschten Block gerettet und in „Hinweise" verschoben,
  damit sie nicht verloren gehen: der Umgebungs-Workaround
  `--no-daemon -g /tmp/gradle-home` gegen die sporadische Gradle-Daemon-IOException.
- Zusätzlich (nicht beauftragt, zur Prüfung vorgelegt) die in dieser Session gelebte
  Praxis festgehalten: Bei reinen Doku-Änderungen zählt der letzte grüne Lauf weiter,
  sofern seither kein Code angefasst wurde – mit Vermerkpflicht im Log.

---

## 42. Prompt: „commit" / „Ja, so committen"

**Aktionen:**
- Vorbedingung erfüllt: `gradle clean build` grün, `systemtest` 13/13, `e2eTest` 7/7 –
  jeweils gegen eine frisch migrierte Datenbank mit den Seed-Daten.
- Beim Vorlegen der Message ausdrücklich vermerkt, dass der Commit **keine** Zuschreibung
  an Michalsen enthält; die Rezepte sind durchgängig als Buchinger-Heilfasten ausgewiesen.
- Diesen Eintrag zuerst ergänzt, dann committet (Author = Claude):
  `Seed the Buchinger fasting recipes via Liquibase`.

---

## 41. Prompt: „Ergänze die Rezepte von Prof. Dr. Andreas Michalsen zum Heilfasten" / „Leg sie als Liquibase-Changeset an"

**Aktionen:**
- Recherchiert: Zu Michalsen persönlich sind **keine** Rezepte öffentlich belegbar; sie stehen
  in seinen urheberrechtlich geschützten Büchern. Rezepte zu erfinden und ihm zuzuschreiben
  wäre eine Falschzuschreibung, die dauerhaft in der Datenbank stünde. Deshalb bewusst das
  **klassische Buchinger-Heilfasten** angelegt – die Methode, die Michalsen am Immanuel
  Krankenhaus klinisch einsetzt – und die Herkunft in jeder Beschreibung benannt.
- Sechs Rezepte als kompletter Zyklus: Entlastungstag, Fastentee, Fastenbrühe, Fastensuppe,
  Fastenbrechen, Aufbautag (38 Zutaten, 27 Schritte). Fachliches Detail aus der Recherche:
  die Fastenbrühe enthält bewusst keine Kartoffeln sowie keine Zwiebeln/Kohl/Hülsenfrüchte.
- Auf Wunsch als Liquibase-Changeset `0002-seed-fasting-recipes.xml` verankert:
  - `id` ist `GENERATED ALWAYS`, explizite IDs sind also nicht einfügbar – Kindzeilen
    referenzieren ihr Rezept über einen Subselect auf den (eindeutigen) Titel.
  - `objectQuotingStrategy="QUOTE_ALL_OBJECTS"` wie in 0001 (ADR 0004); Bezeichner im
    Subselect explizit gequotet, damit es in PostgreSQL **und** im H2 des Codegens läuft.
  - Precondition `onFail="MARK_RAN"` mit `SELECT COUNT(*) FROM "recipe" = 0`: befüllt nur
    eine leere Datenbank, dupliziert nie in eine bestehende.
  - `context="seed"` – ohne Runtime-Context läuft es, ein Deployment kann es per
    `SPRING_LIQUIBASE_CONTEXTS` abwählen.
  - XML skriptgesteuert aus den bereits angelegten Datensätzen erzeugt statt von Hand.
- Verifiziert: `generateJooq` läuft mit den Inserts im H2 durch, frische PostgreSQL wird beim
  Start korrekt befüllt (IDs 1–6, Reihenfolge und Mengen stimmen), `gradle clean build` grün,
  `systemtest` 13/13, `e2eTest` 7/7 – die Tests vertragen den Vorbestand.
- README um den Datenbank-Abschnitt zum Seed inkl. Abwahl-Hinweis ergänzt.

---

## 40. Prompt: „commit" / „Ja, so committen"

**Aktionen:**
- Vor dem Stagen geprüft, dass die Baselines unter
  `frontend/e2e/aria-snapshots.spec.ts-snapshots/` mit eingecheckt werden – sie **sind**
  das erwartete Ergebnis und gehören damit ins Repository (anders als die Artefakte
  unter `build/e2e/`).
- Vorbedingung erfüllt: `gradle clean build` grün, `gradle systemtest` 13/13,
  `gradle e2eTest` 7/7.
- Message vorgelegt und bestätigt bekommen; diesen Eintrag zuerst ergänzt, dann committet
  (Author = Claude): `Assert UI structure with aria snapshots`.
- Angemerkt: Das Zurücksetzen der Datenbank war ein Laufzeit-Vorgang und schlägt sich in
  keiner Datei nieder; es ist nur in Eintrag 39 dokumentiert.

---

## 39. Prompt: „Kannst Du die webm Dateien als erwartetes Testerbenis verwenden?" / „Ja, setz Punkt 1 um und setzte die DB auf den initiale Population!"

**Antwort/Aktionen:**
- Frage mit **Nein** beantwortet und empirisch belegt: zwei identische Läufe erzeugten
  Videos, deren Größen um bis zu 40 % auseinanderlagen (z. B. 41.090 B vs. 58.603 B).
  Ursache: Screencast mit Wanduhr-Zeitstempeln plus nicht bit-reproduzierbare VP8-Kodierung.
- Stattdessen `toMatchAriaSnapshot()` umgesetzt – neue `frontend/e2e/aria-snapshots.spec.ts`
  mit zwei Baselines unter `e2e/aria-snapshots.spec.ts-snapshots/`:
  leeres Anlegeformular (datenunabhängig) und Detailansicht eines im Test selbst
  angelegten, fest benannten Rezepts (Ausschnitt `section.detail`, damit die
  datenabhängige Liste außen vor bleibt).
- Die von Playwright automatisch erzeugten `\d+`-Platzhalter durch **literale Werte**
  ersetzt – sonst bliebe ein Mapper-Fehler, der 300 zu 30 macht, unentdeckt.
- Gegenprobe gefahren: Baseline testweise auf „30 g Mehl" verfälscht → Test schlägt mit
  lesbarem Zeilen-Diff fehl; Baseline wiederhergestellt.
- Determinismus bestätigt: zwei aufeinanderfolgende Läufe grün, im Gegensatz zu den Videos.
- **Datenbank auf initiale Population zurückgesetzt**: Kein Seed im Changelog, also ist der
  Initialzustand das leere Schema. `DROP DATABASE` + `CREATE DATABASE`, Liquibase hat das
  Schema beim nächsten Start neu angelegt. Verworfen wurden die 3 Testrezepte dieser Session.
  Endzustand geprüft: 5 Tabellen, 0 Rezepte, 0 Zutaten, 0 Schritte.
- ADR 0008 um „Video ist Diagnose, nicht Assertion" erweitert (Messwerte, Regeln für
  Snapshots, Begründung, warum `toHaveScreenshot()` mangels festem CI-Image vorerst entfällt);
  README entsprechend ergänzt.
- Verifiziert: `gradle clean build` grün (23 Java + 24 Vitest), `gradle systemtest` 13/13,
  `gradle e2eTest` **7/7** (5 Flows + 2 Aria-Snapshots).

---

## 38. Prompt: „Führe alle Tests aus und starte neu." / „Ja, zieh die @Valid-Warnung nach" / „Ja, führ die durch und dann commit"

**Aktionen:**
- Komplette DoD-Kette gefahren: `gradle clean build` (23 Java + 24 Vitest),
  `gradle systemtest` 13/13, `gradle e2eTest` 5/5 – zusammen **65 Tests grün**.
  `RecipeIntegrationTest` lief mit, wurde also nicht übersprungen.
- App neu gestartet und per Smoke-Test geprüft (SPA, API, Swagger UI, OpenAPI).
- Dabei im Log die Deprecation-Warnung **HV000271** entdeckt und gemeldet.
- Auf Zuruf behoben: `@Valid List<X>` → `List<@Valid X>` in `RecipeRequest`.
  Da die verschachtelte Validierung **von keinem Test abgedeckt** war, zwei Tests in
  `RecipeControllerTest` ergänzt, die auch die Feldpfade prüfen (`ingredients[1].name`,
  `steps[0].instruction`).
- Laufzeit-Nachweis nach Neustart: POST mit Zutaten/Schritten (201) und mit leerem
  Zutatennamen (400) – **0 Treffer** für HV000271 im Log. Verbleibende WARNs sind nur
  die springdoc-Hinweise zu offenen `/v3/api-docs`- und `/swagger-ui.html`-Endpunkten.
- Nach der Änderung erneut `systemtest` (13/13) und `e2eTest` (5/5) grün.
- Nebenbefund zur Werkzeugnutzung: `pkill -f "<muster>"` traf mehrfach die eigene
  Shell-Kommandozeile und brach die Kette ab; Abhilfe ist der Bracket-Trick
  (`pkill -f "[r]ecipe-…"`) bzw. `kill <PID>`.

---

## 37. Prompt: „Erstlle zu den e2eTests entsprechende Filme zur Testdurchführung die das mit im build-Verzeuchnis erzeugt werden."

**Aktionen:**
- `video: { mode: 'on', size: … }` in `playwright.config.ts`: **jeder** Test wird aufgezeichnet,
  nicht nur Fehlschläge – die Videos sollen die Durchführung dokumentieren.
- Alle E2E-Artefakte ins Gradle-Build-Verzeichnis verlegt: `outputDir` →
  `build/e2e/test-results`, HTML-Report → `build/e2e/report`. Begründung: `gradle clean`
  räumt dort ohnehin auf und `/build/` ist bereits in `.gitignore` – keine zusätzliche
  Aufräum-Logik nötig. Konsequenz: die Report-Pfade in README und `CLAUDE.md` mussten
  mitgeändert werden.
- Erster Lauf ergab 5 Videos (je ~60–180 KB, gültiges WebM). Beim Extrahieren eines Frames
  fiel ein eigener Fehler auf: Videogröße 1280×720 passte nicht zum Viewport 1280×800,
  die Seite wurde mit grauem Rand eingepasst. Auf 1280×800 korrigiert und per Frame
  gegengeprüft – jetzt formatfüllend.
- Alte Artefaktordner `frontend/playwright-report/` und `frontend/test-results/` entfernt.
- ADR 0008 um einen Abschnitt „Artefakte" (Tabelle, Begründung für `build/`, Preis von
  ~0,5 MB Video pro Lauf) ergänzt; README um eine Artefakt-Tabelle mit den Öffnen-Befehlen.
- Verifiziert: `tsc --noEmit` sauber, `gradle clean e2eTest` grün (5/5), nach `clean`
  werden Videos und Report neu erzeugt.

---

## 36. Prompt: „commit" / „Ja"

**Aktionen:**
- Reine Doku-Änderung (`CLAUDE.md` + Log), kein Code – letzter grüner Lauf
  (`gradle clean build`, `gradle systemtest` 13/13, `gradle e2eTest` 5/5) bleibt gültig.
- Message vorgelegt und bestätigt bekommen; diesen Eintrag zuerst ergänzt, dann committet
  (Author = Claude): `Add Playwright and e2eTest to CLAUDE.md`.

---

## 35. Prompt: „Ergänze Playwright und e2eTest in CLAUDE.md"

**Aktionen:**
- Tech-Stack Frontend: „Testing: Vitest (Komponenten), Playwright (E2E im echten Browser)".
- Workflow um Schritt 6 „E2E-Tests mit `gradle e2eTest` ausführen" erweitert (Zusammenfassung
  rückt auf 7).
- Nützliche Befehle: `gradle e2eTest` inkl. Hinweis auf die benötigte PostgreSQL,
  `-Pe2e.baseUrl` und den Fundort des Reports.
- Definition of Done um „**E2E-Tests grün (`gradle e2eTest`)** – prüfen die Nutzer-Flows im
  echten Browser" ergänzt.

---

## 34. Prompt: „Wo finde ich die Ergebnisse der Playwrite Tests?" / „Ja, ändern und dann committen"

**Antwort/Aktionen:**
- Ist-Zustand offengelegt: Der HTML-Reporter war an `process.env.CI` gebunden, lokal gab es
  also nur die Konsolenausgabe. Persistent lagen nur `test-results/.last-run.json` sowie –
  ausschließlich bei Fehlschlägen – Screenshot, `trace.zip` und `error-context.md`.
- Report zur Demonstration einmalig erzeugt und Pfade genannt
  (`npx playwright show-report`, `npx playwright show-trace …`).
- Auf eigenen Vorschlag hin die `CI`-Bedingung entfernt: Der HTML-Report wird jetzt bei
  **jedem** Lauf nach `frontend/playwright-report/` geschrieben (`open: 'never'`).
- Verifiziert: `tsc --noEmit` sauber, `npx playwright test` 5/5 grün, Report (≈510 KB)
  wird erzeugt. README um die Fundorte der Ergebnisse ergänzt.
- Anschließend mit der zuvor bestätigten Message committet (Author = Claude).

---

## 33. Prompt: „Setze die Playwright-Suite um"

**Aktionen:**
- `@playwright/test` 1.62.1 + `@types/node` als devDependencies; passenden Chromium-Build
  installiert (der vorherige Feasibility-Check hatte eine andere Playwright-Version).
- `frontend/playwright.config.ts`: `webServer` startet das **Boot-Jar** (Jar-Pfad wird zur
  Laufzeit aufgelöst, mit klarer Fehlermeldung falls `bootJar` fehlt), wartet auf
  `/api/recipes`; `E2E_BASE_URL` schaltet auf eine bereits laufende Instanz um.
  `workers: 1` und `fullyParallel: false`, weil die Datenbank geteilt ist.
- `frontend/e2e/recipes.spec.ts` mit 5 Flows: CRUD-Lebenszyklus komplett über die UI,
  Abbruch des Löschens am `confirm`, Backend-Validierungsfehler am Feld, Navigation
  Liste→Detail→zurück, sowie der Nachweis, dass alle API-Calls dieselbe Origin nutzen.
  Testdaten pro Lauf eindeutig benannt und im `finally` entfernt.
- Gradle-Task `e2eTest` (hängt an `bootJar`), `-Pe2e.baseUrl` wird als `E2E_BASE_URL`
  durchgereicht. Bewusst **nicht** an `check` gehängt (langsamste Schicht, braucht DB).
- Drei Fehler in meinen eigenen Locators gefunden und behoben – alle drei gehen auf
  Playwrights **Teilstring-Matching** von Accessible Names zurück (Testing Library matcht
  exakt): Titel-Button vs. „… löschen"-Button, `getByLabel('Schritt 1')` vs.
  „Schritt 1 entfernen", und ein umbenannter Titel, der den alten als Teilstring enthielt.
  Als Regel in ADR 0008 aufgenommen.
- ADR 0008 auf „akzeptiert und umgesetzt" gesetzt und um die gelernten Regeln ergänzt;
  README um Befehle, Testart und Playwright im Tech-Stack erweitert.
- Verifiziert: `tsc --noEmit` sauber, `gradle clean build` grün (24 Vitest + 23 Java-Tests),
  `gradle systemtest` 13/13, `gradle e2eTest` 5/5 – sowohl selbst-gestartet (29 s) als auch
  gegen eine laufende Instanz via `-Pe2e.baseUrl` (19 s, nachweislich ohne zweite Instanz).

---

## 32. Prompt: „commit" / „commit" (Bestätigung der vorgelegten Message)

**Aktionen:**
- Reine Doku-Änderung (ADR 0008 + Log), kein Code – letzter grüner Lauf
  (`gradle clean build`, `gradle systemtest` 13/13) bleibt gültige Vorbedingung.
- Message vorgelegt und bestätigt bekommen; diesen Eintrag zuerst ergänzt, dann
  committet (Author = Claude): `Add ADR 0008 for Playwright e2e tests`.

---

## 31. Prompt: „Welches e2e Frontend Testframework empfiehlst du?" / „Ergänze einen ADR für Playwright"

**Aktionen:**
- Empfehlung **Playwright** gegeben – und vor der Empfehlung verifiziert, dass sie in dieser
  Umgebung trägt: Chromium Headless Shell 151 (114 MB) ließ sich ohne root und ohne Docker
  installieren und starten. Als Nebeneffekt erstmals ein Screenshot der laufenden UI erstellt.
- Alternativen abgewogen (Cypress, Vitest Browser Mode, WebdriverIO/Selenium, Puppeteer).
- `docs/adr/0008-playwright-fuer-e2e-tests.md` angelegt: Kontext, Optionen, Entscheidung
  (Tests unter `frontend/e2e/`, `webServer` startet das **Boot-Jar**, Gradle-Task `e2eTest`
  mit `-Pe2e.baseUrl`, standardmäßig nur Chromium), Konsequenzen inkl. der offenen Risiken
  (~115 MB Binary pro Runner, `--with-deps` braucht root, langsamste Testart).
- Zusätzlich eine **Abgrenzungstabelle** zu Vitest und `systemtest` aufgenommen, damit E2E
  nicht zur langsamen Zweitkopie der Systemtests wird, plus Regeln für künftige Tests
  (nur sichtbares Verhalten, `getByRole`/`getByLabel`, keine Test-IDs).
- Status bewusst „akzeptiert (Umsetzung steht noch aus)" – es wurde nur der ADR angefragt,
  nicht die Implementierung.

---

## 30. Prompt: „Starte die App" / „Comit"

**Aktionen:**
- App aus dem Boot-Jar auf Port 8080 gestartet; `/` liefert die SPA, `/api/recipes` 200,
  `/swagger-ui.html` 302. Vite-Dev-Server lief parallel auf 5173 weiter.
- Beobachtet: Datenbestand hatte sich gegenüber dem Seeding verändert (Anlegen und
  Löschen über die UI funktionieren bis in die PostgreSQL durch).
- „Comit" als Bestätigung der zuvor vorgelegten Message gewertet; Arbeitsstand seit
  dem grünen Lauf unverändert (nur App gestartet, keine Dateiänderung).
- Diesen Eintrag zuerst ergänzt, dann die 15 Dateien committet (Author = Claude):
  `Mark guaranteed response fields as required`.

---

## 29. Prompt: „Vorschlag: Ergänze @Schema(requiredMode = REQUIRED) an den Response-Records" (+ Zwischenruf „Commit")

**Aktionen:**
- Anhand des Domänenmodells geprüft, welche Felder wirklich garantiert sind
  (`Recipe`-Konstruktor erzwingt `title`/`difficulty`, defaultet `ingredients`/`steps`
  auf leere Listen). Nur diese als `REQUIRED` markiert:
  - `RecipeResponse`: `id`, `title`, `difficulty`, `ingredients`, `steps`
    (`description`, `servings`, `prepTimeMinutes` bleiben optional – echte Nullables).
  - `PreparationStepResponse`: beide Felder. `ErrorResponse` und `FieldError`: alle Felder.
- `IngredientDto` und `PreparationStepDto` bewusst **nicht** angefasst: sie werden von
  Request **und** Response geteilt; dort ist `quantity`/`unit` in beide Richtungen optional.
- Systemtest `shouldMarkGuaranteedResponseFieldsAsRequired` ergänzt – prüft sowohl die neuen
  Pflichtfelder als auch, dass die Nullables optional bleiben.
- Frontend nachgezogen: Typen neu generiert (`id: number` statt `id?: number`), die dadurch
  überflüssigen `!`-Assertions und `?.`-Absicherungen entfernt, `ApiError` um einen
  optionalen Message-Parameter erweitert (synthetische Fehler brauchen kein Fake-Payload).
- Der schärfere Contract deckte prompt zwei unvollständige Test-Fixtures in
  `RecipeList.test.tsx` auf (fehlende `ingredients`/`steps`) – behoben.
- Zwischenruf „Commit" beantwortet: nicht committet, weil das Frontend zu dem Zeitpunkt
  noch nicht compilierte; erst nach grünem Lauf.
- Verifiziert: `tsc --noEmit` sauber, 24 Vitest grün, `gradle clean build` grün,
  `gradle systemtest` 13/13 grün. README und ADR 0007 aktualisiert.

---

## 28. Prompt: „Commit" / „Ja, so committen"

**Aktionen:**
- Vor dem Stagen per `git add -A --dry-run` geprüft, dass `frontend/node_modules/`
  und `frontend/dist/` ausgeschlossen sind, `package-lock.json` aber mitkommt.
- Vorbedingung erfüllt: letzter grüner `gradle clean build` (Frontend-Build,
  24 Vitest, 23 Java-Tests) und `gradle systemtest` (12/12); seither nur Doku geändert.
- Message vorgelegt und bestätigt bekommen; diesen Eintrag zuerst ergänzt, dann
  alle 24 Dateien committet (Author = Claude):
  `Add React frontend using the backend as BFF`.

---

## 27. Prompt: „Erstelle ein Frontend, welches das Backend als BFF verwendet." (+ Zwischenfrage „Mit welcher URL kann ich auf das Frontend zugreifen?")

**Aktionen:**
- Drei Entscheidungen vorab abgefragt; gewählt: Gradle-integriert ins Boot-Jar,
  API-Typen aus OpenAPI generiert, volles CRUD.
- Neues `frontend/` (React 19, Vite 8, TypeScript 5.9, Vitest 4):
  - `src/api/schema.d.ts` per `openapi-typescript` aus `/v3/api-docs` generiert (eingecheckt),
    `src/api/recipes.ts` als typisierter Client mit `ApiError` inkl. Feldfehler-Zugriff.
  - `App` (Ansichts-State statt Router), `RecipeList`, `RecipeDetail`, `RecipeForm`
    mit dynamischen Zutaten/Schritten und Inline-Anzeige der Backend-Validierungsfehler.
  - 24 Vitest-Tests (Client, Komponenten, Zusammenschaltung).
- Gradle-Anbindung ohne Drittanbieter-Plugin: `npmInstall`, `frontendBuild`, `frontendTest`
  als `Exec`-Tasks mit Inputs/Outputs; `processResources` kopiert `frontend/dist` nach
  `static/`; `check` hängt an `frontendTest`; `-PskipFrontend` für die Java-Schleife.
- `FrontendSystemTest` (3 Tests) sichert das BFF-Setup: SPA unter `/`, alle von
  `index.html` referenzierten Assets erreichbar, API und SPA auf derselben Origin.
- Zwei Korrekturen unterwegs: TypeScript 7.0.2 nicht nutzbar (`openapi-typescript`
  verlangt `peer typescript@^5.x`) → auf 5.9.3 gepinnt; ein eigener Testfehler
  (dasselbe `Response`-Objekt für mehrere `fetch`-Aufrufe – Body nur einmal lesbar).
- `docs/adr/0007-react-frontend-mit-backend-als-bff.md` angelegt; README um Frontend-Abschnitt,
  Befehle und Testart erweitert.
- Befund für später dokumentiert: Response-DTOs haben im Contract **keine** Pflichtfelder,
  da springdoc `required` nur aus Bean-Validation ableitet – im Frontend abgesichert.
- Verifiziert: `gradle clean build` grün (Frontend-Build + 24 Vitest + 23 Java-Tests),
  `gradle systemtest` grün (12/12), Boot-Jar enthält `BOOT-INF/classes/static/`,
  `http://localhost:8080/` liefert die SPA, `/api/recipes` und `/v3/api-docs` weiterhin 200.
- Zwischenfrage beantwortet: Dev unter <http://localhost:5173> (Vite, Proxy verifiziert),
  Produktion unter <http://localhost:8080>.

---

## 26. Prompt: „Commit des aktuellen Standes." / „Ja, so committen"

**Aktionen:**
- Nur Doku geändert (`CLAUDE.md`, Log), kein Code – der letzte grüne Lauf
  (`gradle clean build` 23 Tests, `gradle systemtest` 9/9) bleibt gültige Vorbedingung.
- Message vorgelegt und bestätigt bekommen; diesen Eintrag zuerst ergänzt,
  dann committet (Author = Claude): `Add TypeScript role and frontend stack`.

---

## 25. Prompt: „Ergänze im Masterprompt die Rolle eines Du bist ein Senior Typescript Entwickler. Der Tech-Stack wird um vite, vitest und React."

**Aktionen:**
- `CLAUDE.md`, Abschnitt „## Rolle": um die Rolle **Senior TypeScript-Entwickler**
  (Frontend) neben dem Senior Java-Entwickler (Backend) erweitert, inkl. Hinweis,
  dass sich die geltende Rolle aus der bearbeiteten Datei/Aufgabe ergibt.
- `CLAUDE.md`, Abschnitt „## Tech-Stack" in **Backend** und **Frontend** gegliedert;
  Frontend neu mit TypeScript, Vite (Build/Dev-Server), Vitest (Testing), React (UI).
- Bewusst **nicht** angefasst (außerhalb des Auftrags, zur Klärung vorgelegt):
  Grundregel 6 („Moderne Java-Features nutzen") und der rein Java-bezogene Abschnitt
  „Coding-Konventionen"; ebenso keine Versionen gepinnt.

---

## 24. Prompt: „Commit des aktuellen Standes." / „Ja, so committen"

**Aktionen:**
- Vorbedingung geprüft: seit dem letzten grünen Lauf (`gradle clean build` mit 23 Tests,
  `gradle systemtest` 9/9) hatte sich nur `docs/log/claudeLog.md` geändert – kein Code.
- Commit-Message vorgelegt und bestätigt bekommen.
- Diesen Eintrag zuerst ergänzt (Regel „erst Log, dann Commit"), dann alle 8 Dateien
  committet (Author = Claude): `Add systemtest source set and ADR 0006`
  – `build.gradle`, `CLAUDE.md`, `README.md`, ADR 0006, die drei Systemtest-Klassen
  und das Log.

---

## 23. Prompt: „Ergänze ein ADR zur Verwendung des \"unbroken-dome/gradle-testsets-plugin\" und ergänze damit Systemtest mit gradle Task \"systemtest\", mit denen die Swagger AI der gestarteten Anwenung getestet wird. Die Durchführung der \"systemtest\" Task wir in der DoD ergänzt!"

**Aktionen:**
- Plugin-Kompatibilität vorab in einem Wegwerf-Projekt geprüft: `org.unbroken-dome.test-sets`
  **4.1.0** (letzter Release 09/2023) läuft mit **Gradle 9.6.1** – Task wird angelegt,
  Source-Set kompiliert, Tests laufen.
- `build.gradle`: Plugin ergänzt, `testSets { systemtest }`, eigene `systemtest*`-Dependencies
  (JUnit, AssertJ, Zonky) und `tasks.named('systemtest')` mit `useJUnitPlatform()` und
  Property-Durchreichung `-Psystemtest.baseUrl`. Bewusst **nicht** an `check`/`build` gehängt.
- Neues Source-Set `src/systemtest/java`:
  - `RunningApplication`: zwei Modi – extern (`-Psystemtest.baseUrl`) gegen eine deployte
    Instanz, sonst Selbst-Hosting auf freiem Port gegen embedded PostgreSQL (`@EnabledIf`-Guard).
  - `HttpProbe`: JDK-`HttpClient` ohne Redirect-Folgen + Jackson-3-Parsing.
  - `SwaggerSystemTest` (9 Tests): OpenAPI-Metadaten, alle Operationen samt Statuscodes,
    DTO-Schemata inkl. `required` und „keine Domain-Typen im Contract", YAML-Variante,
    Swagger-UI-Redirect/Assets, `configUrl` → eigener Contract statt Petstore, sowie
    Abgleich Contract gegen tatsächliches Verhalten (200/404/400).
- Fehler gefunden und behoben: `SpringApplicationBuilder.properties(...)` landet in
  `defaultProperties` und wird von `application.yml` (`server.port: 80`) überstimmt – die
  Testinstanz startete auf Port 80 gegen die falsche DB. Fix: Übergabe als
  Command-Line-Argumente (`--server.port=…`).
- `docs/adr/0006-testsets-plugin-und-systemtests.md` angelegt; enthält als Konsequenz
  offen den Wartungsstand des Plugins und `jvm-test-suite` als Migrationspfad.
- `CLAUDE.md`: DoD um „Systemtests grün (`gradle systemtest`)" erweitert, Workflow-Schritt 5
  und Befehlsliste ergänzt. README um Befehle und Testart erweitert.
- Verifiziert: `gradle clean build` grün (23 Tests, systemtest bleibt separat),
  `gradle systemtest` grün (9/9, selbst-gehostet, 49 s) und
  `gradle systemtest -Psystemtest.baseUrl=http://localhost:8080` grün gegen eine aus
  `bootJar` gestartete Instanz (9/9, 22 s, keine zweite Instanz gestartet).

---

## 22. Prompt: „commite das"

**Aktionen:**
- Vorbedingung erfüllt: `./gradlew clean build` war grün (23 Tests), Message war bestätigt.
- Alle 14 Dateien committet (Author = Claude): Commit `8c57ae0`
  „Add springdoc-openapi 3.1 and ADR 0005".
- Co-Authored-By-Trailer auf „Claude Opus 5 (1M context)" gesetzt (statt 4.8),
  passend zum tatsächlich genutzten Modell.

---

## 21. Prompt: „Add newest swagger to application and add this for future as ADR."

**Aktionen:**
- Aktuellste springdoc-Version über Maven Central ermittelt: **3.1.0** – die 3.x-Linie
  ist gegen Spring Boot 4.1.0 gebaut (2.x zielt auf Boot 3) und bringt Swagger UI 5.32.11.
- `build.gradle`: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0` ergänzt.
- `application.yml`: `springdoc`-Block (OpenAPI 3.1 unter `/v3/api-docs`,
  UI unter `/swagger-ui.html`, Sortierung, Default-Media-Type).
- Neu `bootstrap/OpenApiConfig.java`: `OpenAPI`-Bean mit Titel/Beschreibung/Lizenz;
  Version aus `spring.application.version` mit Fallback.
- `RecipeController` mit `@Tag`, `@Operation`, `@ApiResponse(s)`, `@Parameter` annotiert;
  DTOs (`RecipeRequest`, `RecipeResponse`, `IngredientDto`, `PreparationStepDto`,
  `PreparationStepResponse`) und `ErrorResponse` mit `@Schema` inkl. Beispielen.
- Neu `OpenApiDocumentationTest` (4 Tests): Dokument-Metadaten, alle fünf Operationen,
  Schemata/`required`-Felder sowie Auslieferung der Swagger UI. Läuft ohne DB
  (`spring.liquibase.enabled=false`).
- `docs/adr/0005-springdoc-openapi-und-swagger-ui.md` angelegt (Kontext, Optionen,
  Entscheidung, Konsequenzen) inkl. Regel für die Zukunft: neue Endpunkte werden im
  selben Commit annotiert; Contract gehört zur Definition of Done.
- README um Tech-Stack-Eintrag, Abschnitt „API-Dokumentation (OpenAPI / Swagger)"
  und den neuen Testtyp ergänzt.
- `./gradlew test` grün: 23 Tests, 0 Fehler, 0 übersprungen (inkl. der 4 neuen).

---

## 20. Prompt: „Ja!" (Bestätigung, die uncommitteten develop.md-Änderungen zu committen)

**Aktionen:**
- Neuen Skill-Ablauf angewandt: Log ergänzt → `gradle test` ausgeführt (grün) →
  Message bestätigt → committet (Author = Claude).
- Commit `Add pre-commit rules to develop skill` mit `.claude/skills/develop.md`
  und `docs/log/claudeLog.md`.

---

## 19. Prompt: „Erweitere develop.md so, dass vor dem commit die message im Prompt bestätigt werden muß."

**Aktionen:**
- Abschnitt „## Commits" in `.claude/skills/develop.md` um die Pflichtregel ergänzt:
  die vorgeschlagene Commit-Message muss dem Nutzer im Prompt vorgelegt und von
  ihm bestätigt werden; ohne Bestätigung kein Commit.

---

## 18. Prompt: „Erweitere develop.md so, dass vor dem commit ein erfolgreicher test durchgeführt wird!"

**Aktionen:**
- Abschnitt „## Commits" in `.claude/skills/develop.md` um die Pflichtregel ergänzt:
  vor jedem Commit muss `gradle test` grün (BUILD SUCCESSFUL) sein; bei Fehlschlag
  wird nicht committet, sondern zuerst die Ursache behoben.

---

## 17. Prompt: „commit!"

**Aktionen:**
- Diesen Eintrag zuerst oben ins Log ergänzt (Regel „erst Log, dann Commit").
- Offene Änderungen (`.claude/skills/develop.md`, `docs/log/claudeLog.md`) committet
  mit Author = Claude, kurzer Betreffzeile + Inhaltszusammenfassung im Body.

---

## 16. Prompt: „Erweitere in develop.md so, dass als Commit-Message eine kurze Zeile vorgeschlagen wird. Dieser wird von dir eine kurze Zusammenfassung des Inhaltes das Commits in der Commit-Message ergänzt."

**Aktionen:**
- Abschnitt „## Commits" in `.claude/skills/develop.md` um die Message-Konvention erweitert:
  kurze, einzeilige Betreffzeile (imperativ, ≤ ~50 Zeichen) + darunter eine
  kurze Zusammenfassung des Commit-Inhalts (1–3 Sätze/Stichpunkte), inkl. Aufbau-Beispiel.

---

## 15. Prompt: „Ändere den Titel in develop.md so, dass er nicht auch das Session-Protokoll beschränkt ist."

**Aktionen:**
- Titel in `.claude/skills/develop.md` von „Skill: Develop – Session-Protokoll pflegen (claudeLog.md)" auf den breiteren „Skill: Develop – Entwicklungsleitfaden" geändert.

---

## 14. Prompt: „Ergänze in develop.md, dass beim commit Claude als Author verwendet wird!"

**Aktionen:**
- In `.claude/skills/develop.md` einen Abschnitt „## Commits" ergänzt:
  Git-**Author** = Claude (`git commit --author="Claude <noreply@anthropic.com>"`),
  zusätzlich weiterhin der `Co-Authored-By`-Trailer.
- Diesen Eintrag oben ins Log ergänzt (noch nicht committet).

---

## 13. Prompt: „Ja, committe das mit deiner vorgeschlagenen Message und in den nächsten Zeilen ein kurze Zusammenfassung!"

**Aktionen:**
- Diesen Eintrag zuerst oben ins Log ergänzt (Regel „erst Log, dann Commit").
- `.claude/skills/develop.md`, `CLAUDE.md` und `docs/log/claudeLog.md` committet mit Message `Add develop skill for claudeLog maintenance` + kurzer Zusammenfassung im Body.

---

## 12. Prompt: „Erzeuge einen neuen Skill develop.md, der zur Pflege des claudeLog.md in zukünftigen Sessions führt."

**Aktionen:**
- Bestehendes Skill-Format geprüft (`.claude/skills/architecture.md`, `testing.md`).
- Neuen Skill `.claude/skills/develop.md` angelegt: Zweck, Wann-anwenden, Format (absteigend, neue Einträge oben), Ablauf pro Prompt, Vorlage und Hinweise zur `claudeLog.md`-Pflege.
- In `CLAUDE.md` unter „## Skills" verlinkt, damit der Skill in künftigen Sessions geladen/aktiv wird.

---

## 11. Prompt: „Committe den Stand mit der message \"Add claudeLog.md\" aber erst nachdem claudeLog.md aktualisiert wurde!"

**Aktionen:**
- `claudeLog.md` zuerst um diesen Eintrag ergänzt (oben eingefügt).
- Danach den Stand mit der Message `Add claudeLog.md` committet.

---

## 10. Prompt: „Sortiere in claudeLog.md die Prompts chronologisch absteigend. Füge als neue Prompt immer vorne ein!"

**Aktionen:**
- Datei auf absteigende Sortierung umgestellt (neuester Eintrag oben).
- Regel notiert/gespeichert: neue Prompts künftig immer als obersten Abschnitt einfügen.

---

## 9. Prompt: „Halte ab jetzt claudeLog.md immer aktuell!"

**Aktionen:**
- Präferenz als Feedback-Memory gesichert (`keep-claude-log-updated.md`) + Memory-Index ergänzt, damit die Regel über Sessions hinweg erhalten bleibt.
- Diesen Turn ans Log angehängt; ab jetzt wird nach jedem Prompt ein neuer Abschnitt ergänzt.
- Hinweis gegeben: für deterministische Automatisierung ist ein `UserPromptSubmit`-Hook in `settings.json` sinnvoll (Angebot offen).

---

## 8. Prompt: „Speichere dort den gesamten Verlauf im Kontext!"

**Aktionen:**
- Diese Datei mit dem vollständigen chronologischen Session-Verlauf gefüllt.

---

## 7. Prompt: „Speichere diesen Prompt am Anfang der der Datei docs/log/claudeLog.md"

**Aktionen:**
- `docs/log/` angelegt und `claudeLog.md` mit dem Prompt-Text erstellt.

---

## 6. Prompt: „Nein, als getrennten commit mit meiner Anweisung als commit message!"

**Aktionen:**
- Festgestellt: Liquibase-Umstellung wurde zwischenzeitlich als eigener Commit `8b833a8` angelegt; Test-Fix noch offen.
- Erster Commit-Versuch scheiterte an fehlender Git-Identität.
- Bisherige Autor-Identität ermittelt (`wsluyterman <wolf.sluyterman@scoop-software.de>`) und lokal gesetzt.
- Nur `RecipeIntegrationTest.java` committet:
  `a978def Fix für "Unused exception parameter should use the unnamed variable pattern" in RecipeIntegrationTest.java`
  (mit `Co-Authored-By`-Trailer für Claude). Working Tree danach sauber.

---

## 5. Prompt: „Ja, lauf gradle test"

**Aktionen:**
- `./gradlew test --no-daemon -g /tmp/gradle-home` ausgeführt.
- **BUILD SUCCESSFUL** in 1m 22s. `compileTestJava` sauber, `:test` grün, `:jacocoTestReport` erstellt.
- `RecipeIntegrationTest` wurde tatsächlich ausgeführt (embedded PostgreSQL gestartet, Liquibase-Migration durchgelaufen).

---

## 4. Prompt: „Fix für \"Unused exception parameter should use the unnamed variable pattern\" in RecipeIntegrationTest.java"

**Aktionen:**
- In `RecipeIntegrationTest.java:41` den ungenutzten Exception-Parameter ersetzt:
  `} catch (Throwable t) {` → `} catch (Throwable _) {` (Unnamed-Variable-Pattern, Java 25).

---

## 3. Prompt: „Erstelle für die Umstellung auf liqibase eine commit message!"

**Aktionen:**
- Git-Ownership-Problem behoben (`safe.directory`), Status/Diffs und ADR 0004 gelesen.
- Änderungen erfasst: `application.yml` (flyway → liquibase), `build.gradle` (Flyway-Deps → Liquibase-Deps, `jooq-meta-extensions` → `-liquibase`, `DDLDatabase` → `LiquibaseDatabase`), neue XML-Changesets, gelöschtes `V1__create_recipe_schema.sql`, angepasster Integrationstest, ADRs, README.
- **Ergebnis:** Vorschlag für eine Conventional-Commits-Message (`build: Migrationswerkzeug von Flyway auf Liquibase umstellen`) mit Details zu Laufzeit, Schema, jOOQ-Codegen und ADR-Referenz. Kein Commit ausgeführt.

---

## 2. Prompt: „Welchen Account hat die DB?"

**Antwort:**
- Host/Port: `localhost:5432`, Datenbank: `recipes`, Benutzer: `recipes`, Passwort: `recipes`.
- Werte aus `application.yml`-Defaults, überschreibbar via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`.
- Rolle `recipes` ist Owner der DB; zusätzlich Superuser `postgres` (beim `initdb` angelegt).
- Instanz läuft mit `--auth=trust` → lokal keine Passwortprüfung; nur für die Wegwerf-Umgebung, nicht für Produktion.

---

## 1. Prompt: „Start die Java-Anwendung!"

**Aktionen:**
- Projektnotiz (`recipe-backend-build-setup.md`) und `build.gradle`/`settings.gradle` gelesen.
- Setup erkannt: Recipe-Backend (Java 25, Gradle, Spring Boot 4.1, jOOQ, Liquibase, PostgreSQL), Hexagonal-Architektur, keine Docker-Umgebung.
- `application.yml` geprüft: DB `jdbc:postgresql://localhost:5432/recipes`, User/PW `recipes`, `server.port: 80`.
- Docker-freie PostgreSQL 18 aus `/usr/sbin` per `initdb` initialisiert (`/tmp/pgdata`) und auf Port 5432 gestartet (`--auth=trust`).
- Rolle + Datenbank `recipes` (Owner `recipes`) angelegt, TCP-Verbindung verifiziert.
- App per `./gradlew bootRun --no-daemon -g /tmp/gradle-home --args='--server.port=8080'` gestartet (Port 80 → 8080, kein Root).
- Liquibase-Migration `0001-create-recipe-schema` lief durch; „Started RecipeApplication in 13.577 seconds", Tomcat auf Port 8080.
- **Verifikation (end-to-end):** `GET /api/recipes` → `200 []`; `POST /api/recipes` (Pfannkuchen) → `201 id:1`; `GET /api/recipes` → `200` mit neuem Rezept. `/` und `/actuator/health` → `404`.

---

  _Die folgenden Einträge sind aus den Commits vom 18.07.2026 nachgetragen – sie liegen vor
dem Anlegen dieses Logs (Eintrag 7) und zählen deshalb ab 1 abwärts weiter. Kennzahlen zu
Dauer und Tokenverbrauch gibt es für sie nicht, das Transkript dieser Turns ist nicht
erhalten. Die Aktionen sind aus den Commits rekonstruiert, nicht aus dem Gesprächsverlauf._

---

## 0. Prompt: „Wieso hast du keine ADRs erstellt?" (Commit `d02045e`, 18.07.2026 23:12)

**Aktionen:**
- Nachfrage als berechtigte Kritik angenommen: `architecture.md` verlangt bei größeren
  Entscheidungen ein ADR, im Backend-Commit (Eintrag -1) fehlten sie vollständig.
- Drei bereits getroffene Entscheidungen unter `docs/adr/` nachdokumentiert:
  - **0001** – jOOQ-Codegen aus den Flyway-Skripten über `DDLDatabase`: Der Build braucht
    weder laufende Datenbank noch Docker, die Migrationsskripte sind einzige Schemaquelle.
  - **0002** – Integrationstests mit embedded PostgreSQL (Zonky) **statt Testcontainers**,
    weil in der Umgebung kein Docker verfügbar ist; die Testklasse überspringt sich selbst,
    wenn das native Binary nicht startet, statt den Build rot zu machen.
  - **0003** – kein Lombok: Domäne und DTOs als `record`, Spring-Beans mit expliziter
    Konstruktor-Injektion.
- Zwei der drei ADRs sind ausdrücklich als **Abweichung** markiert (0002 von `testing.md`,
  0003 vom Lombok-Beispiel in `architecture.md`) – die Skills wurden nicht stillschweigend
  gebrochen, sondern die Abweichung samt Rückweg dokumentiert.
- Damit entstand das Tabellenformat **Kontext / Optionen / Entscheidung / Konsequenzen**,
  das alle späteren ADRs übernehmen.

---

## -1. Prompt: „Erstelle ein backend zur Verwaltung von Rezepten. Ich möchte über eine REST API Rezepte erstellen, bearbeiten und in einer postgres DB speichen können. Löschen soll auch möglich sein" (Commit `13c4737`, 18.07.2026 21:15)

**Aktionen:**
- Erste große Umsetzung: 35 Dateien, ~1.800 Zeilen. Hexagonale Struktur unter
  `io.github.keymaster65.helloai` mit `domain`, `application.port.in/out`,
  `application.service`, `adapter.in.rest`, `adapter.out.persistence`, `bootstrap`.
- REST-CRUD unter `/api/recipes`: `POST` → 201, `GET` (Liste und einzeln) → 200,
  `PUT` → 200, `DELETE` → 204. `GlobalExceptionHandler` liefert 404 und 400 einheitlich
  als `ErrorResponse`; der Controller bleibt frei von Geschäftslogik.
- Domäne als Records (`Recipe`, `Ingredient`, `PreparationStep`, `Difficulty`), DTOs an der
  API-Grenze getrennt, je ein Mapper nach außen (`RecipeRestMapper`,
  `RecipePersistenceMapper`) – Entities werden nicht exponiert.
- Persistenz mit jOOQ (`RecipeJooqRepository`); Schema über Flyway
  `V1__create_recipe_schema.sql` mit den Tabellen `recipe`, `ingredient`,
  `preparation_step`, letztere per `ON DELETE CASCADE` am Rezept.
- Gradle-Build (damals noch Groovy-DSL) mit Wrapper, Java-25-Toolchain, Spring Boot 4.1,
  JaCoCo; jOOQ-Codegen aus den Flyway-Skripten, damit `clean build` ohne Datenbank läuft.
- Tests auf vier Ebenen: Unit (`RecipeServiceImplTest`, Mockito/AssertJ), Property-based
  (`RecipeRestMapperPropertyTest`, jqwik), Web-Slice (`RecipeControllerTest`,
  `@WebMvcTest`) und Integration gegen ein echtes PostgreSQL (`RecipeIntegrationTest`,
  Zonky embedded statt Testcontainers, weil kein Docker vorhanden ist).
- `README.md` vom Platzhalter auf das Projekt umgeschrieben: Stack, Architektur-Tabelle,
  API-Tabelle, Beispiel-Request, Datenbank- und Befehlsabschnitt.
- **Offen geblieben:** kein einziges ADR, obwohl mehrere Abweichungen von den Skills nötig
  waren (Zonky statt Testcontainers, kein Lombok). Genau das ist der Auslöser für den
  nächsten Prompt (Eintrag 0).

---

## -2. Commit: „Add .gitignore" (Commit `bb6db40`, 18.07.2026 19:02)

**Aktionen:**
- Nutzer-Commit ohne Prompt: `/.idea/` in `.gitignore` ergänzt, damit die
  IntelliJ-Projektdateien nicht im Repository landen.

---

## -3. Commit: „Add first AI skills" (Commit `4d8be77`, 18.07.2026 19:00)

**Aktionen:**
- Nutzer-Commit ohne Prompt: die Arbeitsgrundlage dieses Projekts angelegt –
  `CLAUDE.md` sowie die Skills `.claude/skills/architecture.md` und
  `.claude/skills/testing.md`.
- `CLAUDE.md` enthält bereits Rolle (Senior Java-Entwickler), Tech-Stack
  (Java 25, Gradle, jqwik/Mockito/AssertJ/JaCoCo, jOOQ, PostgreSQL, Spring Boot 4),
  Grundregeln, Coding-Konventionen, Workflow und Definition of Done.
- `architecture.md`: Hexagonal/Clean Architecture, Package-Struktur, Schichten- und
  Abhängigkeitsregel, Beispiele, Anti-Patterns und die Pflicht zum ADR bei größeren
  Entscheidungen. `testing.md`: Testpyramide, jqwik, Slice-Tests, Integrationstest mit
  Testcontainers.
- Stand von damals, den spätere Prompts nachziehen: Projektname noch `[Projektname]`
  (bis Eintrag 63), Befehle noch `gradle` statt `./gradlew` (bis Eintrag 45), keine
  TypeScript-Rolle (bis Eintrag 25), kein Systemtest/E2E-Teil in der DoD.

---

## -4. Commit: „Initial commit" (Commit `b0dd66f`, 18.07.2026 18:08)

**Aktionen:**
- Repository-Start durch den Nutzer, zwei Dateien: `README.md`
  („hello-ai – Erster Beispielproject AI getützt entwickelt") und die
  Java-Standard-`.gitignore` (Klassen, Logs, Archive, HotSpot-Crash-Logs).
- Der damalige Projektname `hello-ai` steckt bis heute im Java-Package
  `io.github.keymaster65.helloai` – die Umbenennung in Eintrag 63 hat bewusst nur
  Build- und Laufzeitnamen erfasst, nicht die Paketstruktur.

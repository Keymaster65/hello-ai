# 0018 – Abhängigkeiten im Versionskatalog `libs.versions.toml`

Status: akzeptiert und umgesetzt
Datum: 2026-08-09

| Abschnitt        | Inhalt |
|------------------|--------|
| **Kontext**      | Koordinaten und Versionen standen verteilt in fünf Buildskripten. Drei Stellen waren bereits redundant und mussten von Hand synchron gehalten werden: die jOOQ-Version **dreimal** in `adapter/build.gradle.kts` (`extra["jooq.version"]`, `jooqGenerator`, `jooq { version }`), Zonkys `embedded-postgres` samt BOM **zweimal** in `bootstrap` (für `test` und `systemtest`), jqwik in zwei Modulen. Läuft eine dieser Kopien auseinander, fällt das nicht beim Konfigurieren auf, sondern als schwer deutbarer Laufzeitfehler – bei jOOQ etwa, wenn generierter Code und Runtime verschiedene Versionen haben. [ADR 0010](0010-gradle-kotlin-dsl.md) hatte „Version Catalog" als Option 3 ausdrücklich betrachtet und zurückgestellt, weil sie **nur** die Abhängigkeitsverwaltung löst und nicht die Typisierung. Die Typisierung ist seitdem erledigt; damit ist der Katalog der fällige nächste Schritt. |
| **Optionen**     | 1. **`gradle/libs.versions.toml`** – der Gradle-Standard seit 7.0, mit typsicheren Accessoren (`libs.spring.boot.starter.web`). 2. Versionen als Properties in `gradle.properties` oder `extra` – zentral, aber untypisiert und ohne Werkzeugunterstützung. 3. Konstanten in `buildSrc` bzw. einem Convention-Plugin – typsicher, aber ein eigenes Kompilat und deutlich mehr Apparat für ein Projekt dieser Größe. 4. Status quo beibehalten. |
| **Entscheidung** | **Option 1.** Alle Bibliotheken und alle Plugins wandern in den Katalog – auch die, deren Version das **Spring-Boot-BOM** bestimmt. Diese stehen dort als `{ module = "…" }` **ohne** `version`: Der Katalog führt damit die vollständige Liste der Abhängigkeiten, die Versionshoheit bleibt aber beim BOM. Nur was das BOM *nicht* verwaltet, trägt eine Version (jqwik, springdoc, jOOQ-Codegen, Zonky, ArchUnit) – der `[versions]`-Block ist genau die Liste der Versionen, für die wir selbst verantwortlich sind. Plugins werden über `alias(libs.plugins.…)` deklariert. |
| **Konsequenzen** | **+** Die jOOQ-Version steht an **einer** Stelle; die drei Verwendungen greifen über `libs.versions.jooq.get()` bzw. `version.ref` darauf zu und können nicht mehr auseinanderlaufen. **+** Ein Tippfehler im Alias ist ein **Kompilierfehler des Buildskripts**, kein Auflösungsfehler zur Ausführungszeit. **+** Ein Blick in eine 55-Zeilen-Datei beantwortet „welche Version benutzen wir?", ohne fünf Skripte zu lesen. **+** Das TOML-Format ist ein Standard, den Werkzeuge zur Abhängigkeitsaktualisierung (Dependabot, Renovate) kennen. **−** Eine Indirektion mehr: Im Buildskript steht nicht länger die Version, man springt in den Katalog. **−** Bindestriche im Alias werden im Accessor zu Punkten (`spring-boot-starter-web` → `libs.spring.boot.starter.web`) – gewöhnungsbedürftig, wenn man die Koordinate erwartet. **−** In `subprojects`-Blöcken ist `libs` nicht ohne Weiteres verfügbar (siehe unten). |

## Fallstricke

**`libs` funktioniert in `subprojects { }` nicht.** Der naheliegende Schritt in
`modules/backend/build.gradle.kts` scheitert bei der Konfiguration:

```
Extension with name 'libs' does not exist. Currently registered extension names:
[ext, base, sourceSets, reporting, javaToolchains, java, testing, jacoco, dependencyManagement]
```

Der Grund ist die Reihenfolge: Innerhalb von `subprojects` ist der Empfänger das
**Untermodul**, und dessen Katalog-Erweiterung entsteht erst, wenn dessen eigene Auswertung
beginnt – der Block läuft aber vorher. Abhilfe ist ein Empfänger, der bereits ausgewertet ist:

```kotlin
"testImplementation"(rootProject.libs.assertj.core)
```

**Plugin-Version und Bibliotheks-Version sind zweierlei.** Für jOOQ gibt es zwei
Versionen, die nichts miteinander zu tun haben: das Gradle-Plugin `nu.studer.jooq` (10.2.1)
und jOOQ selbst (3.21.6). Sie stehen bewusst unter zwei Schlüsseln (`jooqPlugin`, `jooq`);
ein gemeinsamer Eintrag wäre eine falsche Kopplung.

**Nicht jedes Plugin bekommt ein `alias`.** In `bootstrap` bleibt
`id("org.springframework.boot")` ohne Version stehen. Version und `apply false` kommen aus
dem Wurzelprojekt; ein `alias` würde die Version dort ein zweites Mal anfordern.

## Nachweis der Gleichwertigkeit

Wie bei [ADR 0010](0010-gradle-kotlin-dsl.md) wurde nicht nur „grün gebaut", sondern gegen
den vorherigen Stand geprüft (2026-08-09): `./gradlew :modules:backend:<modul>:dependencies`
wurde für alle vier Module vor und nach der Umstellung erzeugt (der Vorher-Stand aus einem
`git worktree` auf `HEAD`).

- Die Ausgabe ist für **alle vier Module zeichengleich** – über sämtliche Konfigurationen
  hinweg, inklusive der aufgelösten transitiven Versionen (3.019 Zeilen, `diff` ohne
  Unterschied). Die Umstellung ändert also nachweislich keine einzige aufgelöste Version.
- `./gradlew clean build` grün (30 Java-Tests, 24 Vitest in 4 Dateien).
- `./gradlew systemtest` 15/15, `./gradlew e2eTest` 9/9.

## Regel für die Zukunft

- Eine neue Abhängigkeit wird **zuerst** im Katalog angelegt und dann über ihren Alias
  verwendet. Eine Koordinate als Zeichenkette im Buildskript ist ab jetzt ein Fehler.
- Eine Version in `[versions]` **nur**, wenn das Spring-Boot-BOM sie nicht verwaltet. Sonst
  den Eintrag versionslos lassen – zwei Quellen für dieselbe Version sind der Fehler, den
  dieses ADR abstellt.
- In `subprojects`/`allprojects` immer `rootProject.libs` verwenden.
- Vor dem Anheben einer Version prüfen, ob sie mehrfach referenziert wird (`version.ref`);
  der Katalog macht das sichtbar, verhindert es aber nicht.

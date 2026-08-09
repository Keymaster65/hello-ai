# 0008 – Playwright für End-to-End-Tests

Status: akzeptiert und umgesetzt
Datum: 2026-08-09

| Abschnitt        | Inhalt |
|------------------|--------|
| **Kontext**      | Mit dem React-Frontend ([ADR 0007](0007-react-frontend-mit-backend-als-bff.md)) gibt es erstmals eine Schicht, die keiner der bestehenden Testarten zugänglich ist. Vorhanden sind: Unit-Tests (Mockito), Property-Tests (jqwik), Web-Slice-Tests (`@WebMvcTest`), Integrationstests gegen embedded PostgreSQL, Systemtests über HTTP ([ADR 0006](0006-testsets-plugin-und-systemtests.md)) und Vitest-Komponententests gegen **jsdom**. Was fehlt: der Nachweis, dass die Anwendung **in einem echten Browser** funktioniert – Rendering, Fokus, Formularverhalten, das Zusammenspiel mehrerer Schritte zu einem Nutzer-Flow. jsdom ist eine Nachbildung, kein Browser; und die Systemtests sehen nur HTTP, kein DOM. Weil Frontend und API dank des BFF-Zuschnitts **ein** Artefakt aus **einer** Origin sind, lässt sich genau dieses Deployable end-to-end prüfen. |
| **Optionen**     | 1. **Playwright** – TS-nativ, eigener Runner, Auto-Waiting, Trace Viewer, kann den Server selbst starten (`webServer`). 2. **Cypress** – ausgereift und mit guter DX, aber eigener Runner-Unterbau, schwächer bei mehreren Tabs/Origins. 3. **Vitest Browser Mode** – reizvoll, weil Vitest bereits im Stack ist; zielt aber auf **Komponenten** im echten Browser, nicht auf systemweite Flows (nutzt intern ohnehin Playwright). 4. **WebdriverIO/Selenium** – mehr Konfiguration, kein vergleichbares Auto-Waiting; lohnt nur bei echtem Cross-Browser-Grid-Zwang. 5. **Puppeteer** – kein Runner, kein Assertion-Modell. 6. **Kein E2E** – Browser-Verhalten bleibt ungetestet. |
| **Entscheidung** | **Option 1 – Playwright.** Tests unter `frontend/e2e/`, eigene `playwright.config.ts`. Die `webServer`-Konfiguration startet das **Boot-Jar** (`java -jar build/libs/recipe-backend-*.jar`) und wartet auf `http://localhost:8080` – getestet wird damit exakt das ausgelieferte Artefakt inklusive der ins Jar gepackten SPA, nicht der Vite-Dev-Server. Eingebunden wird ein Gradle-Task **`e2eTest`** nach dem Muster von `frontendTest`, mit `-Pe2e.baseUrl` für Läufe gegen eine deployte Instanz (analog `-Psystemtest.baseUrl`). Standardmäßig läuft nur **Chromium**; weitere Browser nur, wenn ein konkreter Bedarf auftritt. In dieser Umgebung **verifiziert** (2026-08-09): Chromium Headless Shell 151 ließ sich **ohne root und ohne Docker** installieren und starten. |
| **Konsequenzen** | **+** Erstmals Nachweis über echtes Browser-Verhalten statt jsdom-Näherung. **+** Getestet wird das Deployable, nicht eine Dev-Konfiguration – die Same-Origin-Zusage aus ADR 0007 wird im Vollbetrieb belegt. **+** Auto-Waiting statt `sleep`: adressiert direkt die Flakiness-Vorgabe aus `.claude/skills/testing.md` (F.I.R.S.T.). **+** Dieselben Queries wie in den bestehenden Testing-Library-Tests (`getByRole`, `getByLabel`), also kein zweites Selektor-Modell im Projekt. **+** Trace Viewer liefert bei rotem CI-Lauf Zeitleiste, DOM-Snapshots und Netzwerk-Log statt nur eines Stacktraces. **−** ~115 MB Browser-Binary pro CI-Runner (cachebar unter `~/.cache/ms-playwright`). **−** `playwright install --with-deps` benötigt root; hier funktionierte die Installation ohne Systempakete, das ist aber **nicht portabel garantiert** – auf schlanken CI-Images können Bibliotheken fehlen. **−** Eine weitere Testart mit eigener Laufzeit; die Testpyramide wird oben schwerer, wenn nicht diszipliniert abgegrenzt wird. **−** E2E-Tests sind die langsamsten und teuersten Tests im Projekt: Jar-Build, Serverstart und Browser pro Lauf. |

## Abgrenzung zu den bestehenden Testarten

Die entscheidende Regel, damit E2E nicht zur langsamen Zweitkopie wird:

| Testart | Prüft | Prüft **nicht** |
|---------------------|-------------------------------------------------|--------------------------------------|
| Vitest (jsdom)      | Komponentenlogik, Rendering-Zweige, API-Client   | echtes Browser-Verhalten             |
| `systemtest` (Java) | HTTP-Contract, Statuscodes, Auslieferung der SPA | DOM, Interaktion                     |
| **`e2eTest`**       | **Nutzer-Flows im Browser**                      | **API-Details, Statuscodes, Schemata** |

Konkret gehören in die E2E-Suite Flows wie: Rezept anlegen → erscheint in der Liste →
bearbeiten → löschen; Validierungsfehler des Backends erscheint am verursachenden Feld;
Navigation Liste → Detail → zurück.

## Regel für die Zukunft

- E2E-Tests assertieren **ausschließlich auf sichtbares Verhalten**. Statuscodes, JSON-Felder
  und Contract-Details bleiben in den Systemtests – wer beides an beiden Stellen prüft,
  zahlt den Preis doppelt und bekommt bei jeder Änderung zwei rote Suiten.
- Ein neuer E2E-Test entsteht nur für einen Flow, der für Nutzende **sichtbar** anders ist.
  Ein zusätzliches Feld im Formular rechtfertigt keinen eigenen E2E-Test, ein neuer
  Bearbeitungsablauf schon.
- Selektoren über `getByRole`/`getByLabel`, keine CSS-Klassen oder Test-IDs. Das hält die
  Tests an die Barrierefreiheit gekoppelt und robust gegen Umbauten am Markup.
- **`exact: true` nicht vergessen:** Playwright matcht Accessible Names als **Teilstring**,
  Testing Library dagegen exakt. `getByLabel('Schritt 1')` trifft deshalb auch den Button
  „Schritt 1 entfernen" – ein Query, der im Vitest-Test grün ist, kann in E2E mehrdeutig sein.
- Testdaten pro Lauf eindeutig benennen (Suffix) und im `finally` wieder entfernen; die
  Datenbank ist geteilt, deshalb läuft die Suite mit `workers: 1`.

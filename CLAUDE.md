# Projekt: recipes

Die Arbeitsgrundlage dieses Projekts – Master-Prompt und Skills – steht als AsciiDoc unter
`docs/prompt/` ([ADR 0023](docs/adr/0023-asciidoc-fuer-masterprompt-und-skills.adoc)).
Diese Datei bindet sie nur ein: `CLAUDE.md` ist der einzige Dateiname, den Claude Code
automatisch lädt, und bleibt deshalb Markdown.

**Verbindlich ist der Master-Prompt** (Rolle, Tech-Stack, Grundregeln, Coding-Konventionen,
Workflow, Befehle, Definition of Done). Er wird hier importiert:

@docs/prompt/masterprompt.adoc

Sollte der Import einmal nicht greifen, ist `docs/prompt/masterprompt.adoc` zu Beginn der
Session zu lesen – ohne ihn fehlen die verbindlichen Regeln.

Die neun Skills werden **bei Bedarf** gelesen, nicht importiert; der Master-Prompt nennt sie
mit Pfad und Anlass:

- `docs/prompt/architektur.adoc` – Architektur
- `docs/prompt/adr.adoc` – ADR / Architekturentscheidungen (`docs/adr/*.adoc`)
- `docs/prompt/tests.adoc` – Tests
- `docs/prompt/security.adoc` – Security (Backend, inklusive RFC 9116 / `security.txt`)
- `docs/prompt/systemdoku.adoc` – Systemdokumentation (`docs/system/*.adoc`)
- `docs/prompt/butterfly.adoc` – Butterfly / Session-Protokoll (`docs/log/claudeLog.adoc`)
- `docs/prompt/manager.adoc` – Manager / Kennzahlen-Tabelle (`docs/log/kennzahlen.adoc`)
- `docs/prompt/po.adoc` – PO / Backlog (`docs/backlog.adoc`, Befehl `next`)
- `docs/prompt/develop.adoc` – Develop / Commits und Zusammenführen der Branches

**Keine Regel wird in dieser Datei ergänzt** – sonst gibt es die Arbeitsgrundlage an zwei
Orten. Neues gehört in den Master-Prompt oder in einen Skill; die Zuordnung steht in
`docs/prompt/prompt.adoc`, Abschnitt „Pflege dieser Arbeitsgrundlage".

Gerendert: `./gradlew asciidoctorPrompt` → `build/docs/prompt/prompt.html`.

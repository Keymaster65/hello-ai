# Projekt: recipes

Die Arbeitsgrundlage dieses Projekts – Master-Prompt und Skills – steht als AsciiDoc unter
`docs/prompt/`. Diese Datei bindet sie nur ein: `CLAUDE.md` ist der einzige Dateiname, den
Claude Code automatisch lädt, und bleibt deshalb Markdown.

**Verbindlich ist der Master-Prompt** (Rolle, Tech-Stack, Grundregeln, Coding-Konventionen,
Workflow, Befehle, Definition of Done). Er wird hier importiert:

@docs/prompt/masterprompt.adoc

Sollte der Import einmal nicht greifen, ist `docs/prompt/masterprompt.adoc` zu Beginn der
Session zu lesen – ohne ihn fehlen die verbindlichen Regeln.

Die vierzehn Skills werden **bei Bedarf** gelesen, nicht importiert; der Master-Prompt nennt
sie mit Pfad und Anlass. Jeder Skill trägt seine Regeln **und** ihre Begründung im Abschnitt
„Festlegungen und ihre Gründe" – eine eigene Sammlung von Architekturentscheidungen
(`docs/adr/`) gibt es nicht mehr.

- `docs/prompt/entscheidungen.adoc` – Entscheidungen festlegen, einhalten, ablösen
- `docs/prompt/architektur.adoc` – Architektur (Schichten, Module, Records)
- `docs/prompt/build.adoc` – Build, Abhängigkeiten, Versionen
- `docs/prompt/persistenz.adoc` – Schema, Migration, jOOQ
- `docs/prompt/api.adoc` – REST, Contract, Fehlerformat, Adressraum
- `docs/prompt/frontend.adoc` – SPA, Routen, erzeugte Typen
- `docs/prompt/tests.adoc` – Tests
- `docs/prompt/security-backend.adoc` – Security Backend (inklusive RFC 9116 / `security.txt`)
- `docs/prompt/security-frontend.adoc` – Security Frontend (SPA)
- `docs/prompt/systemdokumentation.adoc` – Systemdokumentation (`docs/system/*.adoc`)
- `docs/prompt/butterfly.adoc` – Butterfly / Session-Protokoll (`docs/log/claudeLog.adoc`)
- `docs/prompt/manager.adoc` – Manager / Kennzahlen-Tabelle (`docs/log/kennzahlen.adoc`)
- `docs/prompt/po.adoc` – PO / Backlog (`docs/backlog.adoc`, Befehl `next`)
- `docs/prompt/develop.adoc` – Develop / Commits und Zusammenführen der Branches

**Keine Regel wird in dieser Datei ergänzt** – sonst gibt es die Arbeitsgrundlage an zwei
Orten. Neues gehört in den Master-Prompt oder in einen Skill; die Zuordnung steht in
`docs/prompt/prompt.adoc`, Abschnitt „Pflege dieser Arbeitsgrundlage".

Gerendert: `./gradlew asciidoctorPrompt` → `build/docs/prompt/prompt.html`.

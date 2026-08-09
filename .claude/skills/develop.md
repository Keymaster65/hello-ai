# Skill: Develop – Entwicklungsleitfaden

## Zweck
Aktiviere diesen Skill in **jeder** Entwicklungs-Session, damit das
Session-Protokoll `docs/log/claudeLog.md` fortlaufend gepflegt wird.
Ziel: ein nachvollziehbares Log aller Nutzer-Prompts samt einer
kurzen Zusammenfassung der durchgeführten Aktionen/Ergebnisse.

## Wann anwenden
- Zu **Beginn** einer Session: Existenz von `docs/log/claudeLog.md` prüfen,
  bei Bedarf mit Header anlegen.
- Nach **jedem** beantworteten Nutzer-Prompt: einen neuen Eintrag ergänzen,
  bevor der Turn endet.

## Format
- Datei: `docs/log/claudeLog.md`
- Nummerierte Abschnitte: `## N. Prompt: „<Originaltext des Prompts>"`
- **Sortierung: chronologisch absteigend – neuester Eintrag oben.**
- Neue Einträge **immer vorne** einfügen (direkt unter dem Header,
  vor dem bisher obersten Eintrag). Nummerierung fortlaufend hochzählen.
- Direkt unter der Überschrift zwei **Kennzahlenzeilen**: **Delta** (dieser Prompt) und
  **Stand** (kumuliert über die Session) – siehe unten.
- Je Eintrag ein `**Aktionen:**`-Block (bzw. `**Antwort:**` bei reinen
  Fragen) mit knapper Stichpunkt-Zusammenfassung von Tun und Ergebnis.

## Dauer und Tokenanzahl
Beides ist dem Assistenten **nicht** direkt bekannt – es steht aber im Transkript, das
Claude Code je Session unter `~/.claude/projects/<projekt>/<session>.jsonl` schreibt:
Jeder Eintrag trägt einen Zeitstempel, jede Assistant-Nachricht ihre `usage`.
`docs/log/turn-stats.py` liest das aus:

```bash
python3 docs/log/turn-stats.py --log-line   # fertige Zeile für den Eintrag
python3 docs/log/turn-stats.py -n 5         # Übersicht der letzten fünf Turns
```

Ergebnis (Beispiel):

```markdown
_Delta: 1:00 · 9.290 out · 3.656 in (neu) · 3.003.002 gesamt_
_Stand (Session, 68 Prompts): 4:04:54 · 764.504 out · 289.620.079 gesamt_
```

- **Delta** = Aufwand **dieses einen** Prompts. **Stand** = kumuliert über alle Prompts
  der Session; der Prompt ist damit als Delta zum vorigen Stand lesbar.
- **Dauer** = Wanduhrzeit von der Ankunft des Prompts bis zur letzten Antwort,
  inklusive Wartezeit auf Werkzeuge und Builds. Die Stand-Dauer ist die **Summe der
  Turn-Dauern**, nicht die Zeit seit Session-Beginn – Wartezeit auf den Nutzer zählt nicht.
- **out** = generierte Tokens, **in (neu)** = nicht aus dem Cache gelesene Eingabe,
  **gesamt** = alles inklusive Cache-Reads (das, was abgerechnet wird).
- **Zahlen niemals schätzen.** Ist das Transkript nicht lesbar, wird das im Eintrag
  vermerkt (`Dauer/Tokens: nicht ermittelbar`) statt geraten.
- **Systematische Untererfassung:** Gemessen wird bis zum Schreiben des Eintrags; die
  abschließende Antwort des Turns fehlt darin zwangsläufig. Die Werte sind damit eine
  Untergrenze – nicht schönrechnen, sondern so verstehen.
- **Der Stand gilt je Session.** Das Transkript umfasst nur die laufende Session; Prompts
  aus früheren Sessions sind darin nicht enthalten. Deshalb steht die Prompt-Zahl der
  Session in der Zeile – sie weicht bewusst von der Nummer des Log-Eintrags ab.
  Da nach jedem Commit `/clear` läuft (siehe „Commits"), umfasst eine Session in der Regel
  genau die Arbeit **seit dem letzten Commit**; der Stand springt dort auf null zurück.

## Ablauf pro Prompt
1. Nutzer-Prompt beantworten / Aufgabe umsetzen.
2. `python3 docs/log/turn-stats.py --log-line` ausführen.
3. Neuen Abschnitt oben in `docs/log/claudeLog.md` einfügen:
   - Überschrift mit fortlaufender Nummer und Original-Prompt.
   - Kennzahlenzeile aus Schritt 2.
   - Stichpunkte zu Aktionen/Ergebnissen (Commits, Tests, Dateien, Entscheidungen).
4. Log-Aktualisierung vor Turn-Ende sicherstellen.

## Vorlage
```markdown
## <N>. Prompt: „<Original-Prompt>"

_Delta: <m:ss> · <out> out · <in> in (neu) · <gesamt> gesamt_
_Stand (Session, <k> Prompts): <h:mm:ss> · <out> out · <gesamt> gesamt_

**Aktionen:**
- <Was wurde getan>
- <Ergebnis / Nachweis (z. B. Commit-Hash, Testergebnis)>
```

## Commits
- **DoD vor Commit (Pflicht):** Vor **jedem** Commit muss die **Definition of Done**
  aus `CLAUDE.md` erfüllt sein – dort stehen die verbindlichen Prüfschritte, damit sie
  nur an **einer** Stelle gepflegt werden. Ist ein Punkt offen, wird **nicht** committet,
  sondern zuerst die Ursache behoben.
- **Author = Claude:** Beim Committen wird als Git-**Author** Claude gesetzt,
  nicht der bisherige Nutzer. Konkret pro Commit:
  ```
  git commit --author="Claude <noreply@anthropic.com>" -m "<Message>"
  ```
  (alternativ dauerhaft via `git config user.name "Claude"` /
  `git config user.email "noreply@anthropic.com"`).
- **Message-Bestätigung (Pflicht):** Die vorgeschlagene Commit-Message wird
  dem Nutzer **im Prompt vorgelegt und muss von ihm bestätigt werden**, bevor
  committet wird. Ohne Bestätigung erfolgt **kein** Commit; bei Änderungswunsch
  wird die Message angepasst und erneut zur Bestätigung vorgelegt.
- **Commit-Message:** Als Betreff wird eine **kurze, einzeilige** Message
  vorgeschlagen (imperativ, ca. ≤ 50 Zeichen). In den Folgezeilen ergänzt
  der Assistent eine **kurze Zusammenfassung des Commit-Inhalts** (1–3 Sätze
  bzw. Stichpunkte). Aufbau:
  ```
  <kurze Betreffzeile>

  <kurze Zusammenfassung des Inhalts>

  Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
  ```
- Zusätzlich weiterhin den Trailer im Commit-Body:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.
- **`/clear` nach jedem Commit (Pflicht):** Ein Commit schließt eine Arbeitseinheit ab –
  der Kontext der nächsten beginnt leer. Nach jedem erfolgreichen Commit fordert der
  Assistent den Nutzer als **letzte Zeile der Antwort** auf, `/clear` einzugeben.
  `/clear` ist ein Client-Befehl von Claude Code und kann **nicht** vom Assistenten oder
  von einem Hook ausgelöst werden; die Aufforderung ist deshalb der verbindliche Teil.
  Zwei Konsequenzen, die daraus folgen:
  - Alles, was nach dem Commit noch gebraucht wird, muss **vorher** in einer Datei stehen
    (`docs/log/claudeLog.md`, ADR, README) – nicht nur im Gesprächsverlauf.
  - `/clear` startet ein neues Transkript, damit beginnt der **Stand** der Kennzahlen bei
    null (siehe „Dauer und Tokenanzahl"). Das ist gewollt und kein Messfehler.

## Hinweise
- Prompt-Text möglichst **wörtlich** übernehmen (in Anführungszeichen).
- Zusammenfassungen kurz und faktenbasiert halten; keine Roh-Tool-Ausgaben.
- Reihenfolge beachten, wenn ein Prompt „erst aktualisieren, dann committen"
  verlangt: **zuerst** das Log ergänzen, **dann** committen.
- Bei reinen Doku-Änderungen zählt der letzte grüne Lauf weiter, solange seither
  **kein Code** angefasst wurde. Das ist im Log-Eintrag ausdrücklich zu vermerken.
- In dieser Umgebung startet der Gradle-Daemon sporadisch mit
  `java.io.IOException: Input/output error`; stabil läuft es mit
  `./gradlew <task> --no-daemon -g /tmp/gradle-home`.
- Für eine deterministische Garantie (statt „Best Effort") kann zusätzlich ein
  `UserPromptSubmit`-Hook in `.claude/settings.json` den Roh-Prompt automatisch
  anhängen; die inhaltliche Zusammenfassung ergänzt weiterhin der Assistent.

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
- Je Eintrag ein `**Aktionen:**`-Block (bzw. `**Antwort:**` bei reinen
  Fragen) mit knapper Stichpunkt-Zusammenfassung von Tun und Ergebnis.

## Ablauf pro Prompt
1. Nutzer-Prompt beantworten / Aufgabe umsetzen.
2. Neuen Abschnitt oben in `docs/log/claudeLog.md` einfügen:
   - Überschrift mit fortlaufender Nummer und Original-Prompt.
   - Stichpunkte zu Aktionen/Ergebnissen (Commits, Tests, Dateien, Entscheidungen).
3. Log-Aktualisierung vor Turn-Ende sicherstellen.

## Vorlage
```markdown
## <N>. Prompt: „<Original-Prompt>"

**Aktionen:**
- <Was wurde getan>
- <Ergebnis / Nachweis (z. B. Commit-Hash, Testergebnis)>
```

## Commits
- **Author = Claude:** Beim Committen wird als Git-**Author** Claude gesetzt,
  nicht der bisherige Nutzer. Konkret pro Commit:
  ```
  git commit --author="Claude <noreply@anthropic.com>" -m "<Message>"
  ```
  (alternativ dauerhaft via `git config user.name "Claude"` /
  `git config user.email "noreply@anthropic.com"`).
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

## Hinweise
- Prompt-Text möglichst **wörtlich** übernehmen (in Anführungszeichen).
- Zusammenfassungen kurz und faktenbasiert halten; keine Roh-Tool-Ausgaben.
- Reihenfolge beachten, wenn ein Prompt „erst aktualisieren, dann committen"
  verlangt: **zuerst** das Log ergänzen, **dann** committen.
- Für eine deterministische Garantie (statt „Best Effort") kann zusätzlich ein
  `UserPromptSubmit`-Hook in `.claude/settings.json` den Roh-Prompt automatisch
  anhängen; die inhaltliche Zusammenfassung ergänzt weiterhin der Assistent.

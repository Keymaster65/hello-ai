# ADR 0025: Session-Protokoll `claudeLog` als AsciiDoc

## Status
Akzeptiert – 2026-08-15

Korrigiert eine Teilentscheidung aus
[ADR 0023](0023-asciidoc-fuer-masterprompt-und-skills.md).

## Kontext
[ADR 0023](0023-asciidoc-fuer-masterprompt-und-skills.md) hat Master-Prompt und Skills auf
AsciiDoc umgestellt und in der Tabelle „Was Markdown bleibt" ausdrücklich festgehalten:

> `docs/log/claudeLog.md` | Protokoll, kein Dokument: fortlaufend, nur vorne ergänzt.
> `docs/log/turn-stats.py` erzeugt fertige **Markdown**-Zeilen; AsciiDoc würde das Werkzeug
> brechen

Damit war das Repository beim Dateiformat zweigeteilt: Alles unter `docs/` ist AsciiDoc – die
Systemdokumentation ([ADR 0022](0022-asciidoc-systemdokumentation-in-docs-system.md)), die
Arbeitsgrundlage (ADR 0023) – bis auf das Protokoll und die ADRs.

Zwei der drei damaligen Gründe halten der Prüfung nicht stand:

* **„turn-stats.py würde brechen."** Das Werkzeug erzeugt die Zeile
  `_Delta: 6:15 · 19.447 out · …_`. Der Unterstrich ist in *beiden* Formaten die
  Kursiv-Auszeichnung; die Ausgabe ist unverändert gültig. Am Skript ist keine Zeile Code zu
  ändern – nur zwei Formulierungen, die „Markdown" sagen.
* **„Protokoll, kein Dokument."** Das begründet, warum die Datei *nicht gerendert* wird. Es
  begründet nicht, warum sie eine zweite Auszeichnungssprache im selben Verzeichnisbaum
  braucht. Wer den Log pflegt, wechselt heute mitten in der Arbeit die Syntax – `##` statt
  `==`, `**fett**` statt `*fett*`, `- ` statt `* `. Genau diese Verwechslung ist in der
  Vergangenheit mehrfach passiert.

Der dritte Grund bleibt gültig: Der Log wächst pro Prompt und zitiert beliebigen Text. Er
gehört deshalb **nicht** in den Build.

## Entscheidung
`docs/log/claudeLog.md` wird zu **`docs/log/claudeLog.adoc`**. Der Bestand von 98 Einträgen
wird einmalig konvertiert.

Die Datei wird **nicht** von `./gradlew asciidoctor` oder `asciidoctorPrompt` gerendert und
in kein Dokument per `include` eingebunden. Sie ist AsciiDoc, weil das die Sprache dieses
Repositories ist – nicht, weil daraus HTML entstehen soll.

## Konsequenzen

### Positiv
* Eine Auszeichnungssprache unter `docs/`, mit Ausnahme der ADRs (siehe „Offen").
* Kein Syntaxwechsel mehr innerhalb einer Session zwischen Log und Skills.
* Der Log ist bei Bedarf renderbar – geprüft, er läuft ohne Warnung durch Asciidoctor.

### Negativ
* Code-Spans brauchen teilweise `+`-Schutz: `` `+rings_are_respected+` `` statt
  `` `rings_are_respected` ``, sonst macht AsciiDoc aus den Unterstrichen Kursivschrift; bei
  `` `+/{id}+` `` wäre es sonst eine Attribut-Referenz und bei `` `+<<commits>>+` `` ein
  Querverweis. Das ist beim Schreiben mitzudenken und im Butterfly-Skill festgehalten.
* Eine Liste braucht in AsciiDoc eine Leerzeile nach dem Absatz davor. Ohne sie hängt
  `* Punkt` als Text an `*Aktionen:*` – die Einträge sähen dann im Rendering wie ein
  Fließtext-Absatz aus.
* Die Umstellung erzeugt einen großen Commit-Diff über die gesamte Historie des Protokolls.
  Der Inhalt ist dabei nachweislich unverändert (siehe „Prüfung").

### Offen
Die ADRs bleiben Markdown. Sie werden auf GitHub gelesen, von `docs/system` und der
Arbeitsgrundlage per `link:`-Makro verlinkt und nach ADR 0024 als `*.md` neben das gerenderte
HTML kopiert. Eine Umstellung würde diese Kette anfassen und ist eine eigene Entscheidung.

## Prüfung
Die Konvertierung wurde nicht nach Augenschein abgenommen, sondern gemessen: Das gerenderte
AsciiDoc wurde von HTML befreit, ebenso das Markdown von seiner Auszeichnung, beides auf
Zeichen ohne Leerraum normalisiert und verglichen – **identisch**. Dazu die Struktur:
98 Überschriften → 98 `<h2>`, 510 Listenpunkte → 510 `<li>`, keine Asciidoctor-Warnung.

## Alternativen

1. **Markdown behalten** (Status quo aus ADR 0023). Kein Aufwand, aber der Syntaxbruch
   mitten in der Arbeit bleibt – und die Begründung „turn-stats.py würde brechen" ist
   nachweislich falsch.
2. **Umstellen und in den Build aufnehmen.** Verlockend, weil der Log dann mitgeprüft wäre.
   Verworfen: Der Log zitiert wörtliche Prompts, also beliebigen Text. Ein `{`, ein `<<` oder
   eine Zeile, die wie ein Listenmarker aussieht, würde den Build rot färben – ein Protokoll
   darf die Auslieferung nicht blockieren.
3. **Nur neue Einträge in AsciiDoc.** Verworfen: eine Datei mit zwei Syntaxen ist schlechter
   als eine Datei mit einer, egal welcher.

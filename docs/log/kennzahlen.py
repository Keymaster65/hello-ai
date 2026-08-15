#!/usr/bin/env python3
"""Erzeugt die Kennzahlen-Tabelle des Session-Protokolls aus `docs/log/claudeLog.adoc`.

Hintergrund: Der Log führt Dauer und Tokenverbrauch je Prompt als zwei Kursivzeilen unter
der Überschrift – gut zu schreiben, aber nicht zu vergleichen. Dieses Skript liest genau
diese Zeilen aus und schreibt sie als Tabelle nach `docs/log/kennzahlen.adoc`, die der
Anhang der Systemdokumentation einbindet.

Die Tabelle ist damit *abgeleitet*: Der Log bleibt die einzige Quelle, die Zahlen werden
nicht zweitgeführt. Die Werte je Zeile werden als Zeichenkette übernommen, nicht neu
gerechnet – was im Log steht, steht in der Tabelle.

Ausgenommen sind die drei `Summe`-Spalten: Sie werden *gerechnet* (ADR 0039), weil eine
laufende Summe im Log nicht steht und dort auch nicht hingehört. Gezählt wird über alle
Einträge hinweg, anders als die `Stand`-Spalten, die je Session gelten.

Aufruf:

    python3 docs/log/kennzahlen.py            # Tabelle neu erzeugen
    python3 docs/log/kennzahlen.py --check    # nur prüfen, ob sie zum Log passt (Exit 1)
    python3 docs/log/kennzahlen.py --stdout   # Ergebnis ausgeben statt schreiben

Das Format regelt der Skill `docs/prompt/manager.adoc`.
"""

from __future__ import annotations

import argparse
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
LOG = os.path.join(BASE, "claudeLog.adoc")
TABLE = os.path.join(BASE, "kennzahlen.adoc")

# `== 99. Prompt: „commit"` bzw. `== -2. Commit: „Add .gitignore" (…)`
HEADING = re.compile(r"^== (?P<nr>-?\d+)\. (?P<text>.+)$")

# `_Delta: 2:09 min · 26.783 Token out · 37.809 Token in (neu) · 2.434.527 Token gesamt_`
# Die Gruppe `in (neu)` fehlt in den ältesten Zeilen – sie wurde erst später eingeführt.
DELTA = re.compile(
    r"^_Delta: (?P<dauer>[^·]+?) · (?P<out>[\d.]+) Token out"
    r"(?: · (?P<neu>[\d.]+) Token in \(neu\))? · (?P<gesamt>[\d.]+) Token gesamt_$"
)

# `_Stand (Session, 6 Prompts): 9:19 min · 98.915 Token out · 7.942.629 Token gesamt_`
STAND = re.compile(
    r"^_Stand \(Session, (?P<prompts>\d+) Prompts\): (?P<dauer>[^·]+?) · "
    r"(?P<out>[\d.]+) Token out · (?P<gesamt>[\d.]+) Token gesamt_$"
)

MISSING = "–"

HEADER = """\
// Erzeugt von docs/log/kennzahlen.py aus docs/log/claudeLog.adoc – nicht von Hand ändern.
// Gepflegt nach dem Skill docs/prompt/manager.adoc; eingebunden in
// docs/system/anhang-kennzahlen.adoc.

[[kennzahlen-tabelle]]
.Dauer und Tokenverbrauch je Prompt, neuester zuerst
[cols=">1,6l,>1,>1,>1,>1,>1,>1,>1,>1,>1,>1", options="header"]
|===
| Nr.
| Prompt
| Dauer
| out / Token
| in (neu) / Token
| gesamt / Token
| Stand Dauer
| Stand out / Token
| Stand gesamt / Token
| Summe Dauer
| Summe out / Token
| Summe in (neu) / Token
"""

# `2:09 min`, `45 s`, `1:47:30 h` – die drei Formen, die der Butterfly-Skill vorgibt.
DAUER_SEKUNDEN = re.compile(r"^(\d+) s$")
DAUER_MINUTEN = re.compile(r"^(\d+):(\d\d) min$")
DAUER_STUNDEN = re.compile(r"^(\d+):(\d\d):(\d\d) h$")


def dauer_in_sekunden(text: str) -> int | None:
    """Eine Dauer aus dem Log als Sekunden – oder `None`, wenn sie dort fehlt.

    Erkannt werden genau die drei Formen des Butterfly-Skills. Alles andere – `–`,
    „nicht ermittelbar", eine von Hand verunglückte Zeile – gilt als *unbekannt* und
    nicht als Null: Der Unterschied steht in der Fußnote des Anhangs.
    """
    text = text.strip()
    for muster, faktoren in (
        (DAUER_SEKUNDEN, (1,)),
        (DAUER_MINUTEN, (60, 1)),
        (DAUER_STUNDEN, (3600, 60, 1)),
    ):
        treffer = muster.match(text)
        if treffer:
            return sum(int(g) * f for g, f in zip(treffer.groups(), faktoren))
    return None


def dauer_formatieren(sekunden: int) -> str:
    """Sekunden in der Schreibweise des Butterfly-Skills: `s`, dann `m:ss`, dann `h:mm:ss`."""
    if sekunden < 60:
        return f"{sekunden} s"
    if sekunden < 3600:
        return f"{sekunden // 60}:{sekunden % 60:02d} min"
    return f"{sekunden // 3600}:{sekunden % 3600 // 60:02d}:{sekunden % 60:02d} h"


def zahl_lesen(text: str) -> int | None:
    """`153.418` → `153418`; `–` → `None`. Der Punkt ist Tausendertrenner, kein Dezimalpunkt."""
    text = text.strip()
    if not text or text == MISSING:
        return None
    try:
        return int(text.replace(".", ""))
    except ValueError:
        return None


def zahl_formatieren(wert: int) -> str:
    """`153418` → `153.418` – dieselbe Gruppierung wie im Log."""
    return f"{wert:,}".replace(",", ".")


class Entry:
    """Ein Log-Eintrag: Überschrift und – falls vorhanden – seine beiden Kennzahlenzeilen."""

    def __init__(self, nr: str, text: str) -> None:
        self.nr = nr
        self.text = text
        self.delta: re.Match | None = None
        self.stand: re.Match | None = None
        # Laufende Summen über *alle* Einträge bis hierher (ADR 0039); von `akkumulieren`
        # gesetzt, nicht aus dem Log gelesen.
        self.summe_dauer = 0
        self.summe_out = 0
        self.summe_neu = 0

    def cell(self, match: re.Match | None, group: str) -> str:
        """Ein Wert aus dem Log – oder `–`, wenn er dort fehlt. Nichts wird geschätzt."""
        if match is None:
            return MISSING
        return match.group(group) or MISSING


def read_entries(path: str) -> list[Entry]:
    entries: list[Entry] = []
    with open(path, encoding="utf-8") as log:
        for line in log:
            line = line.rstrip("\n")

            heading = HEADING.match(line)
            if heading:
                entries.append(Entry(heading.group("nr"), heading.group("text")))
                continue

            if not entries:
                continue

            if line.startswith("_Delta:"):
                entries[-1].delta = DELTA.match(line)
            elif line.startswith("_Stand "):
                entries[-1].stand = STAND.match(line)
    return entries


def prompt_cell(text: str) -> str:
    """Der Prompt-Text für die literale Spalte (`l` im `cols`-Attribut).

    Literal heißt: keine AsciiDoc-Substitution. Ein wörtlich zitierter Prompt kann alles
    enthalten – `+*+`, `+{+`, `+<<+` –, ohne den Build zu gefährden. Zu schützen bleibt nur
    der Zellentrenner. Das Leerzeichen nach `+|+` entfällt, weil eine literale Zelle es
    mit ausgeben würde.

    Das `Prompt: ` der Überschrift fällt weg – es steht schon in der Spaltenüberschrift.
    Die vier Einträge ohne Prompt behalten ihr `Commit: `, sonst wären sie nicht
    unterscheidbar.
    """
    if text.startswith("Prompt: "):
        text = text[len("Prompt: "):]
    return text.replace("|", r"\|")


def akkumulieren(entries: list[Entry]) -> None:
    """Setzt je Eintrag die laufende Summe über *alle* Einträge bis zu ihm (ADR 0039).

    Gezählt wird von der *ältesten* Zeile aufwärts – die Liste steht absteigend, deshalb
    `reversed`. Die oberste Zeile trägt damit die Gesamtsumme des Projekts.

    Abgegrenzt gegen die vorhandenen `Stand`-Spalten: Die stammen aus dem Log und gelten je
    *Session*, springen nach jedem `/clear` also auf null zurück. Diese hier laufen über die
    Sessions hinweg durch.

    Ein fehlender Wert zählt als *null*, nicht als Lücke: Die Summe läuft weiter, statt
    abzureißen. Sie ist damit eine *Untergrenze* – 64 der Einträge tragen keine Kennzahlen.
    """
    dauer = out = neu = 0
    for entry in reversed(entries):
        dauer += dauer_in_sekunden(entry.cell(entry.delta, "dauer")) or 0
        out += zahl_lesen(entry.cell(entry.delta, "out")) or 0
        neu += zahl_lesen(entry.cell(entry.delta, "neu")) or 0
        entry.summe_dauer = dauer
        entry.summe_out = out
        entry.summe_neu = neu


def render(entries: list[Entry]) -> str:
    akkumulieren(entries)
    rows = [HEADER]
    for entry in entries:
        rows.append(
            f"\n| {entry.nr}\n"
            f"|{prompt_cell(entry.text)}\n"
            f"| {entry.cell(entry.delta, 'dauer')}\n"
            f"| {entry.cell(entry.delta, 'out')}\n"
            f"| {entry.cell(entry.delta, 'neu')}\n"
            f"| {entry.cell(entry.delta, 'gesamt')}\n"
            f"| {entry.cell(entry.stand, 'dauer')}\n"
            f"| {entry.cell(entry.stand, 'out')}\n"
            f"| {entry.cell(entry.stand, 'gesamt')}\n"
            f"| {dauer_formatieren(entry.summe_dauer)}\n"
            f"| {zahl_formatieren(entry.summe_out)}\n"
            f"| {zahl_formatieren(entry.summe_neu)}\n"
        )
    rows.append("|===\n")
    return "".join(rows)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true",
                        help="nur prüfen, ob die Tabelle zum Log passt")
    parser.add_argument("--stdout", action="store_true",
                        help="Ergebnis ausgeben statt schreiben")
    parser.add_argument("--log", default=LOG, help=f"Pfad zum Protokoll (Default: {LOG})")
    parser.add_argument("--table", default=TABLE, help=f"Zieldatei (Default: {TABLE})")
    args = parser.parse_args()

    entries = read_entries(args.log)
    if not entries:
        raise SystemExit(f"Keine Einträge in {args.log} gefunden.")
    table = render(entries)

    if args.stdout:
        print(table, end="")
        return

    if args.check:
        try:
            with open(args.table, encoding="utf-8") as current:
                actual = current.read()
        except FileNotFoundError:
            raise SystemExit(f"{args.table} fehlt – `python3 docs/log/kennzahlen.py` läuft nicht.")
        if actual != table:
            raise SystemExit(f"{args.table} passt nicht zu {args.log} – neu erzeugen.")
        print(f"{os.path.basename(args.table)}: aktuell ({len(entries)} Einträge)")
        return

    with open(args.table, "w", encoding="utf-8") as target:
        target.write(table)
    missing = sum(1 for entry in entries if entry.delta is None)
    print(f"{os.path.basename(args.table)}: {len(entries)} Einträge geschrieben "
          f"({missing} ohne Kennzahlen)", file=sys.stderr)


if __name__ == "__main__":
    main()

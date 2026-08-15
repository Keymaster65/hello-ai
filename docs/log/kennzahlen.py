#!/usr/bin/env python3
"""Erzeugt die Kennzahlen-Tabelle des Session-Protokolls aus `docs/log/claudeLog.adoc`.

Hintergrund: Der Log führt Dauer und Tokenverbrauch je Prompt als zwei Kursivzeilen unter
der Überschrift – gut zu schreiben, aber nicht zu vergleichen. Dieses Skript liest genau
diese Zeilen aus und schreibt sie als Tabelle nach `docs/log/kennzahlen.adoc`, die der
Anhang der Systemdokumentation einbindet.

Die Tabelle ist damit *abgeleitet*: Der Log bleibt die einzige Quelle, die Zahlen werden
nicht zweitgeführt. Die Werte werden als Zeichenkette übernommen, nicht neu gerechnet –
was im Log steht, steht in der Tabelle.

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
[cols=">1,6l,>1,>1,>1,>1,>1,>1,>1", options="header"]
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
"""


class Entry:
    """Ein Log-Eintrag: Überschrift und – falls vorhanden – seine beiden Kennzahlenzeilen."""

    def __init__(self, nr: str, text: str) -> None:
        self.nr = nr
        self.text = text
        self.delta: re.Match | None = None
        self.stand: re.Match | None = None

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


def render(entries: list[Entry]) -> str:
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

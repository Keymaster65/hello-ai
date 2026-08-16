#!/usr/bin/env python3
"""Legt zu jedem Nutzer-Prompt das Gerüst eines Log-Eintrags in `docs/log/claudeLog.adoc` an.

Hintergrund: Der Butterfly-Skill verlangt zu *jedem* Prompt einen Eintrag, konnte das aber
nur als Best Effort zusagen – 64 der Einträge tragen keine Kennzahlen, und ein vergessener
Eintrag fällt nirgends auf. Dieses Skript hängt als `UserPromptSubmit`-Hook in
`.claude/settings.json` und schreibt Anker, Überschrift und Platzhalter, *bevor* der
Assistent überhaupt antwortet. Die Zuordnung Prompt → Eintrag entsteht damit deterministisch;
inhaltlich gefüllt wird der Eintrag weiterhin vom Assistenten.

Was das Skript tut und was nicht:

* Es schreibt *Anker*, *Überschrift* mit dem wörtlichen Prompt und die Platzhalter
  `(offen)`. Kennzahlen (`docs/log/turn-stats.py`) und der `*Aktionen:*`-Block bleiben Sache
  des Assistenten.
* Es fügt *vorne* ein – die Sortierung des Protokolls ist absteigend – und zählt die Nummer
  aus der höchsten vorhandenen hoch.
* Es erzeugt *keine* Kennzahlen-Tabelle. `docs/log/kennzahlen.py` liest den Platzhalter als
  „keine Kennzahlen" und schreibt `–`; das ist derselbe Zustand wie bei einem Eintrag, dessen
  Zahlen nicht ermittelbar waren.

Aufruf:

    python3 docs/log/prompt-eingang.py              # Hook-Betrieb: JSON auf stdin
    python3 docs/log/prompt-eingang.py --dry-run    # nur zeigen, was eingefügt würde
    echo '{"prompt":"Test"}' | python3 docs/log/prompt-eingang.py --dry-run

Das Format regelt der Skill `docs/prompt/butterfly.adoc`.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
LOG = os.path.join(BASE, "claudeLog.adoc")

# `== 141. Prompt: „…"` – dieselbe Form, aus der `kennzahlen.py` die Tabelle liest.
HEADING = re.compile(r"^== (?P<nr>-?\d+)\. ")

# Der Trenner zwischen zwei Einträgen. Der erste steht bereits über dem obersten Eintrag,
# direkt unter dem Kopf der Datei – dort wird eingefügt.
TRENNER = "'''"

# Platzhalter, die der Assistent ersetzt. Auch die Erkennung eines schon angelegten Gerüsts
# hängt daran: Steht er noch da, ist der Eintrag unfertig.
OFFEN = "(offen)"

# Länge, ab der die Überschrift gekürzt wird. Ein Prompt kann beliebig lang sein; die
# Überschrift ist eine Zeile und steht so auch in der Kennzahlen-Tabelle.
MAX_LAENGE = 300


def prompt_lesen(argv_prompt: str | None) -> str:
    """Der Prompt-Text – aus `--prompt` oder aus dem Hook-JSON auf stdin.

    Claude Code übergibt `UserPromptSubmit` als JSON-Objekt; das Feld heißt `prompt`.
    Ein leerer oder unlesbarer Aufruf liefert eine leere Zeichenkette, und das Skript
    bricht folgenlos ab – ein Hook darf den Turn nicht verhindern.
    """
    if argv_prompt is not None:
        return argv_prompt
    if sys.stdin.isatty():
        return ""
    try:
        daten = json.loads(sys.stdin.read() or "{}")
    except json.JSONDecodeError:
        return ""
    if not isinstance(daten, dict):
        return ""
    return str(daten.get("prompt") or daten.get("user_prompt") or "")


def ueberschrift_text(prompt: str) -> str:
    """Der Prompt als *eine* Zeile Überschrift – gekürzt und AsciiDoc-fest.

    Zwei Eingriffe, beide notwendig, beide sichtbar:

    * *Eine Zeile.* Zeilenumbrüche und Mehrfach-Leerzeichen werden zu einem Leerzeichen;
      ab `MAX_LAENGE` wird mit `…` gekürzt. Eine Überschrift über mehrere Zeilen wäre
      keine Überschrift mehr.
    * *Escapes für `{` und `<<`.* Beides ist in einer Überschrift wirksam: `{name}` wird zur
      Attribut-Referenz und verschwindet, `<<ziel>>` zum Querverweis – und ein Querverweis
      ins Leere ist ein Build-Fehler (ADR 0022). Der Backslash ist Asciidoctors eigenes
      Escape und wird nicht mitgerendert; in der literalen Prompt-Spalte der
      Kennzahlen-Tabelle bleibt er allerdings sichtbar. Wem das auffällt, der schreibt die
      Überschrift von Hand um – dafür ist sie da.

    Nicht escaped werden `*` und `_`: Sie machen aus dem Text höchstens Fett- oder
    Kursivschrift, brechen aber nichts.
    """
    text = " ".join(prompt.split())
    if len(text) > MAX_LAENGE:
        text = text[:MAX_LAENGE].rstrip() + "…"
    return text.replace("{", r"\{").replace("<<", r"\<<")


def naechste_nummer(zeilen: list[str]) -> int:
    """Die höchste Nummer im Protokoll plus eins; bei leerem Protokoll die 1."""
    nummern = [int(t.group("nr")) for t in map(HEADING.match, zeilen) if t]
    return max(nummern) + 1 if nummern else 1


def anker(nummer: int) -> str:
    """`142` → `prompt-142`, `-2` → `prompt-minus2` – wie in `kennzahlen.py`.

    Das Vorzeichen wird ausgeschrieben, weil Asciidoctor `--` durch einen Geviertstrich
    ersetzt und der Verweis darauf still ins Leere liefe (ADR 0038).
    """
    return "prompt-" + str(nummer).replace("-", "minus")


def geruest(nummer: int, text: str) -> list[str]:
    """Die Zeilen des neuen Eintrags, in der Reihenfolge des Butterfly-Skills."""
    return [
        "",
        f"[[{anker(nummer)}]]",
        f"== {nummer}. Prompt: „{text}\"",
        "",
        f"_Delta: {OFFEN}_",
        f"_Stand: {OFFEN}_",
        "",
        "*Aktionen:*",
        "",
        f"* {OFFEN}",
        "",
        TRENNER,
    ]


def einfuegestelle(zeilen: list[str]) -> int:
    """Index *hinter* dem Trenner über dem obersten Eintrag.

    Fehlt der Trenner – ein frisch angelegtes Protokoll –, wird ans Ende angehängt.
    """
    for i, zeile in enumerate(zeilen):
        if zeile.strip() == TRENNER:
            return i + 1
    return len(zeilen)


def schon_angelegt(zeilen: list[str], text: str) -> bool:
    """Trägt der oberste Eintrag denselben Prompt und ist er noch unfertig?

    Der Hook kann denselben Prompt zweimal sehen – bei einem Wiederaufsetzen der Sitzung
    etwa. Dann soll kein zweites Gerüst entstehen.
    """
    for i, zeile in enumerate(zeilen):
        treffer = HEADING.match(zeile)
        if not treffer:
            continue
        kopf_passt = zeile.endswith(f"Prompt: „{text}\"")
        return kopf_passt and any(OFFEN in z for z in zeilen[i:i + 12])
    return False


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true",
                        help="nur ausgeben, was eingefügt würde")
    parser.add_argument("--prompt", help="Prompt-Text direkt übergeben statt per stdin")
    parser.add_argument("--log", default=LOG, help=f"Pfad zum Protokoll (Default: {LOG})")
    args = parser.parse_args()

    prompt = prompt_lesen(args.prompt)
    if not prompt.strip():
        return

    text = ueberschrift_text(prompt)

    try:
        with open(args.log, encoding="utf-8") as datei:
            zeilen = datei.read().split("\n")
    except FileNotFoundError:
        # Ein fehlendes Protokoll legt der Assistent an (Butterfly-Skill, „Wann anwenden").
        # Der Hook schweigt dazu, statt einen Kopf zu erfinden.
        print(f"Butterfly: {args.log} fehlt – Eintrag von Hand anlegen.")
        return

    if schon_angelegt(zeilen, text):
        return

    nummer = naechste_nummer(zeilen)
    block = geruest(nummer, text)

    if args.dry_run:
        print("\n".join(block))
        return

    stelle = einfuegestelle(zeilen)
    zeilen[stelle:stelle] = block
    with open(args.log, "w", encoding="utf-8") as datei:
        datei.write("\n".join(zeilen))

    print(f"Butterfly: Eintrag {nummer} in docs/log/claudeLog.adoc angelegt. "
          f"Kennzahlen (`python3 docs/log/turn-stats.py --log-line`), Aktionen und – falls "
          f"committet – die Zeile `_Commit:_` ergänzt der Assistent; danach "
          f"`python3 docs/log/kennzahlen.py`.")


if __name__ == "__main__":
    main()

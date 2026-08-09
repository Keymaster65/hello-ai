#!/usr/bin/env python3
"""Ermittelt Dauer und Tokenverbrauch je Prompt aus dem Claude-Code-Transkript.

Hintergrund: Für `docs/log/claudeLog.md` sollen Dauer und Tokenanzahl mitgeführt werden.
Beides ist dem Assistenten nicht direkt bekannt – wohl aber dem Transkript, das Claude Code
pro Session schreibt: Jeder Eintrag trägt einen Zeitstempel, jede Assistant-Nachricht ihre
`usage`. Dieses Skript liest das aus, statt Zahlen zu schätzen.

Aufruf (letzte Turns der aktuellen Session):

    python3 docs/log/turn-stats.py            # letzter Turn
    python3 docs/log/turn-stats.py -n 5       # letzte fünf Turns
    python3 docs/log/turn-stats.py --all      # alle Turns, als Tabelle

Ein „Turn" beginnt mit einem echten Nutzer-Prompt (Textnachricht, kein Tool-Ergebnis) und
endet mit der letzten Assistant-Nachricht davor. Die Dauer ist damit die Wanduhrzeit von der
Ankunft des Prompts bis zur letzten Antwort – inklusive Wartezeit auf Werkzeuge.
"""

from __future__ import annotations

import argparse
import glob
import json
import os
from datetime import datetime

TRANSCRIPT_GLOB = os.path.expanduser("~/.claude/projects/*/*.jsonl")


def parse_time(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None


def is_user_prompt(entry: dict) -> bool:
    """Echter Nutzer-Prompt – Tool-Ergebnisse und Skill-Injektionen sind Listen, keine Strings."""
    return (
        entry.get("type") == "user"
        and not entry.get("isSidechain")
        and isinstance((entry.get("message") or {}).get("content"), str)
    )


def collect_turns(path: str) -> list[dict]:
    turns: list[dict] = []
    with open(path, encoding="utf-8") as transcript:
        for line in transcript:
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue

            timestamp = parse_time(entry.get("timestamp"))

            if is_user_prompt(entry):
                turns.append({
                    "prompt": entry["message"]["content"],
                    "start": timestamp,
                    "end": timestamp,
                    "input": 0,
                    "output": 0,
                    "cache_write": 0,
                    "cache_read": 0,
                    "messages": 0,
                })
                continue

            if not turns or entry.get("type") != "assistant":
                continue

            turn = turns[-1]
            usage = (entry.get("message") or {}).get("usage") or {}
            turn["input"] += usage.get("input_tokens", 0)
            turn["output"] += usage.get("output_tokens", 0)
            turn["cache_write"] += usage.get("cache_creation_input_tokens", 0)
            turn["cache_read"] += usage.get("cache_read_input_tokens", 0)
            turn["messages"] += 1
            if timestamp and (turn["end"] is None or timestamp > turn["end"]):
                turn["end"] = timestamp
    return turns


def duration(turn: dict) -> str:
    if not turn["start"] or not turn["end"]:
        return "?"
    seconds = int((turn["end"] - turn["start"]).total_seconds())
    return f"{seconds // 60}:{seconds % 60:02d}" if seconds >= 60 else f"{seconds}s"


def total_tokens(turn: dict) -> int:
    """Alles, was abgerechnet wird – Cache-Reads eingeschlossen."""
    return turn["input"] + turn["output"] + turn["cache_write"] + turn["cache_read"]


def german(number: int) -> str:
    return f"{number:,}".replace(",", ".")


def log_line(turn: dict) -> str:
    """Die Zeile, die unter die Überschrift in claudeLog.md gehört."""
    if turn["messages"] == 0:
        # Der Turn hat noch keine Antwort im Transkript – Nullen wären eine Falschaussage.
        return "_Dauer/Tokens: noch nicht ermittelbar (keine Antwort im Transkript)_"
    fresh = turn["input"] + turn["cache_write"]
    return (f"_Dauer: {duration(turn)} · Tokens: {german(turn['output'])} out, "
            f"{german(fresh)} in (neu), {german(total_tokens(turn))} gesamt_")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("-n", type=int, default=1, help="Anzahl der letzten Turns (Default: 1)")
    parser.add_argument("--all", action="store_true", help="alle Turns ausgeben")
    parser.add_argument("--log-line", action="store_true",
                        help="nur die Markdown-Zeile für den letzten Turn ausgeben")
    parser.add_argument("--transcript", help="Pfad zum Transkript (Default: neuestes)")
    args = parser.parse_args()

    path = args.transcript
    if not path:
        candidates = glob.glob(TRANSCRIPT_GLOB)
        if not candidates:
            raise SystemExit(f"Kein Transkript unter {TRANSCRIPT_GLOB} gefunden.")
        path = max(candidates, key=os.path.getmtime)

    turns = collect_turns(path)
    if not turns:
        raise SystemExit(f"Keine Turns in {path} gefunden.")

    if args.log_line:
        print(log_line(turns[-1]))
        return

    selected = turns if args.all else turns[-max(1, args.n):]
    print(f"# {os.path.basename(path)} – {len(turns)} Turns\n")
    print(f"{'#':>4}  {'Dauer':>7}  {'Tokens':>9}  {'davon out':>9}  Prompt")
    for index, turn in enumerate(selected, start=len(turns) - len(selected) + 1):
        prompt = " ".join(turn["prompt"].split())[:60]
        print(f"{index:>4}  {duration(turn):>7}  {total_tokens(turn):>9,}  "
              f"{turn['output']:>9,}  {prompt}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Writes the language files of every plugin from tools/lang/<module>.txt.

Format of a source file: a key on its own line, followed by five indented lines
in the order en, de, fr, nl, it. Lines starting with # are comments.

    python3 tools/gen_lang.py
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CODES = ["en", "de", "fr", "nl", "it"]


def parse(path):
    entries, key, texts = [], None, []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.startswith("#"):
            continue
        if line[0] != " ":
            if key is not None and len(texts) != len(CODES):
                sys.exit(f"{path.name}: '{key}' has {len(texts)} of {len(CODES)} translations")
            if key is not None:
                entries.append((key, texts))
            key, texts = line.strip(), []
        else:
            texts.append(line.strip())
    if key is not None:
        if len(texts) != len(CODES):
            sys.exit(f"{path.name}: '{key}' has {len(texts)} of {len(CODES)} translations")
        entries.append((key, texts))
    return entries


def main():
    for source in sorted((ROOT / "tools" / "lang").glob("*.txt")):
        module = source.stem
        target = (ROOT / "UltraCore/src/main/resources/core-lang" if module == "core"
                  else ROOT / f"Ultra{module.capitalize()}/src/main/resources/lang")
        target.mkdir(parents=True, exist_ok=True)
        entries = parse(source)
        for index, code in enumerate(CODES):
            lines = [f'{key}: "{texts[index].replace(chr(92), chr(92) * 2).replace(chr(34), chr(92) + chr(34)).replace(chr(92) * 2 + "n", chr(92) + "n")}"'
                     for key, texts in entries]
            (target / f"{code}.yml").write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"{module}: {len(entries)} keys")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Generates logos, banners, previews, concept READMEs and SpigotMC descriptions
for every plugin in tools/plugins.py.

    python3 tools/generate.py && node tools/render.js

Generated files are overwritten on every run - change tools/plugins.py instead.
"""
import html
import json
import re
from pathlib import Path

from plugins import ICONS, PLUGINS
import release

ROOT = Path(__file__).resolve().parent.parent
RAW = "https://raw.githubusercontent.com/creativedm0031/UltraPlugins/main"

# ---- Brand tokens (identical for every plugin) -------------------------------
VIOLET, CYAN = "#8B5CF6", "#22D3EE"
BG0, BG1 = "#0B1020", "#161D3F"
WHITE, GRAY, DARK = "#FFFFFF", "#94A3B8", "#64748B"
RED, RED_SOFT = "#F87171", "#FCA5A5"
FONT = "Inter, 'Segoe UI', Arial, sans-serif"

DEFS = f"""<defs>
  <linearGradient id="brand" gradientUnits="userSpaceOnUse" x1="2" y1="2" x2="22" y2="22">
    <stop offset="0" stop-color="{VIOLET}"/><stop offset="1" stop-color="{CYAN}"/>
  </linearGradient>
  <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="{BG0}"/><stop offset="1" stop-color="{BG1}"/>
  </linearGradient>
  <linearGradient id="edge" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="{VIOLET}"/><stop offset="1" stop-color="{CYAN}"/>
  </linearGradient>
  <linearGradient id="word" gradientUnits="userSpaceOnUse" x1="420" y1="0" x2="1300" y2="0">
    <stop offset="0" stop-color="{VIOLET}"/><stop offset="1" stop-color="{CYAN}"/>
  </linearGradient>
  <filter id="blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="60"/></filter>
  <pattern id="grid" width="48" height="48" patternUnits="userSpaceOnUse">
    <path d="M48 0H0V48" fill="none" stroke="#FFFFFF" stroke-opacity=".035" stroke-width="2"/>
  </pattern>
</defs>"""


def icon(name, width=1.8):
    return (f'<g fill="none" stroke="url(#brand)" stroke-width="{width}" '
            f'stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</g>')


def logo_body(name):
    """The logo tile in a 512x512 coordinate space."""
    return f"""<rect width="512" height="512" rx="116" fill="url(#bg)"/>
<clipPath id="tile"><rect width="512" height="512" rx="116"/></clipPath>
<g clip-path="url(#tile)">
  <rect width="512" height="512" fill="url(#grid)"/>
  <circle cx="150" cy="140" r="130" fill="{VIOLET}" opacity=".30" filter="url(#blur)"/>
  <circle cx="380" cy="390" r="130" fill="{CYAN}" opacity=".22" filter="url(#blur)"/>
</g>
<rect x="9" y="9" width="494" height="494" rx="107" fill="none" stroke="url(#edge)" stroke-width="8"/>
<g transform="translate(112 112) scale(12)">{icon(name)}</g>"""


def logo_svg(name):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">\n'
            f'{DEFS}\n{logo_body(name)}\n</svg>\n')


def backdrop(w, h):
    return f"""<rect width="{w}" height="{h}" fill="url(#bg)"/>
<rect width="{w}" height="{h}" fill="url(#grid)"/>
<circle cx="{w * .12:.0f}" cy="0" r="{h * .7:.0f}" fill="{VIOLET}" opacity=".28" filter="url(#blur)"/>
<circle cx="{w * .9:.0f}" cy="{h}" r="{h * .7:.0f}" fill="{CYAN}" opacity=".20" filter="url(#blur)"/>
<rect y="{h - 8}" width="{w}" height="8" fill="url(#edge)"/>"""


def banner_svg(p):
    name = p["name"]
    chips = f'<tspan fill="{CYAN}">  •  </tspan>'.join(html.escape(c) for c in p["chips"] + ["5 Languages"])
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 480" width="1920" height="480">
{DEFS}
{backdrop(1920, 480)}
<g transform="translate(1440 40) scale(16.5)" opacity=".10">{icon(name, 1.4)}</g>
<g transform="translate(120 116) scale(.484375)">{logo_body(name)}</g>
<text x="420" y="232" font-family="{FONT}" font-size="124" font-weight="800" letter-spacing="-3" fill="{WHITE}">Ultra<tspan fill="url(#word)">{name}</tspan></text>
<text x="424" y="298" font-family="{FONT}" font-size="40" font-weight="500" fill="{GRAY}">{html.escape(p["tagline"][1])}</text>
<text x="424" y="368" font-family="{FONT}" font-size="28" font-weight="600" fill="{WHITE}" xml:space="preserve">{chips}</text>
</svg>
"""


def header_svg(title):
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 150" width="1920" height="150">
{DEFS}
{backdrop(1920, 150)}
<rect x="120" y="45" width="10" height="60" rx="5" fill="url(#edge)"/>
<text x="160" y="97" font-family="{FONT}" font-size="60" font-weight="800" letter-spacing="-1" fill="{WHITE}">{title}</text>
</svg>
"""


def series_svg():
    tiles = "".join(
        f'<g transform="translate({120 + i * 172} 300) scale(.28125)">{logo_body(p["name"]).replace("tile", "tile%d" % i)}</g>'
        for i, p in enumerate(PLUGINS))
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 520" width="1920" height="520">
{DEFS}
{backdrop(1920, 520)}
<text x="120" y="170" font-family="{FONT}" font-size="124" font-weight="800" letter-spacing="-3" fill="{WHITE}">Ultra<tspan fill="url(#word)">Plugins</tspan></text>
<text x="124" y="236" font-family="{FONT}" font-size="40" font-weight="500" fill="{GRAY}">Ten plugins. One look. One feel.</text>
{tiles}
</svg>
"""


# ---- In-game preview mock-up (HTML, rendered to PNG) --------------------------
def mm_to_html(msg):
    return html.escape(msg).replace("&lt;c&gt;", f'<span style="color:{CYAN}">').replace("&lt;/c&gt;", "</span>")


def preview_html(p):
    name = p["name"]
    slots = "".join('<i></i>' for _ in range(9))
    return f"""<!doctype html><meta charset="utf-8"><style>
*{{margin:0;box-sizing:border-box}}
body{{width:1280px;height:420px;overflow:hidden;position:relative;font-family:'DejaVu Sans Mono',monospace;font-weight:700;
 background:linear-gradient(#27345f 0%,#3b4d86 46%,#1d2a22 46%,#141d18 100%)}}
body:before{{content:"";position:absolute;inset:0;background:rgba(8,12,26,.45)}}
.tag{{position:absolute;top:18px;right:22px;font:600 15px Inter,sans-serif;color:{GRAY};letter-spacing:.08em;text-transform:uppercase}}
.ab{{position:absolute;left:0;right:0;bottom:104px;text-align:center;font-size:24px;color:#fff;text-shadow:3px 3px 0 rgba(0,0,0,.75)}}
.g{{background:linear-gradient(90deg,{VIOLET},{CYAN});-webkit-background-clip:text;color:transparent;text-shadow:none;filter:drop-shadow(3px 3px 0 rgba(0,0,0,.75))}}
.sep{{color:{DARK}}}
.bar{{position:absolute;left:50%;bottom:22px;transform:translateX(-50%);display:flex;border:3px solid #0b0b0b;background:rgba(0,0,0,.45)}}
.bar i{{width:58px;height:58px;border:3px solid #6f6f6f;border-right-color:#3a3a3a;border-bottom-color:#3a3a3a}}
.chat{{position:absolute;left:0;top:150px;padding:8px 14px;background:rgba(0,0,0,.5);font-size:20px;color:{RED_SOFT};text-shadow:2px 2px 0 rgba(0,0,0,.7)}}
.chat b{{color:{RED}}}
.lbl{{position:absolute;font:600 14px Inter,sans-serif;color:{GRAY};letter-spacing:.06em;text-transform:uppercase}}
</style>
<div class="tag">Mock-up · in-game layout</div>
<div class="lbl" style="left:14px;top:122px">Chat – errors only</div>
<div class="chat"><b>✖ Ultra{name}</b> <span class="sep">»</span> {html.escape(p["error"][1])}</div>
<div class="lbl" style="left:0;right:0;text-align:center;bottom:150px">Action bar – everything else</div>
<div class="ab"><span class="g">✦ {name}</span> <span class="sep">»</span> {mm_to_html(p["messages"][0][1])}</div>
<div class="bar">{slots}</div>
"""


# ---- Text -------------------------------------------------------------------
def mm(msg):
    return msg.replace("<c>", "<accent>").replace("</c>", "</accent>")


def concept_md(p):
    name, full, low = p["name"], "Ultra" + p["name"], "ultra" + p["name"].lower()
    others = " · ".join(f"[Ultra{o['name']}](../Ultra{o['name']})" for o in PLUGINS if o is not p)
    feats = "\n".join(f"- {f[0]}" for f in p["features"])
    cmds = "\n".join(f"| `{c}` | {d[0]} | `{perm}` |" for c, perm, d in p["commands"])
    perms = "\n".join(f"| `{perm}` | {d[0]} |" for perm, d in p["extra_perms"])
    msgs = "\n".join(f'{k}: "{mm(v)}"' for k, v in p["messages"])
    return f"""<p align="center"><img src="assets/banner.png" alt="{full}"></p>

# {full}

> {p["tagline"][0]}

{p["intro"][0]}

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

{feats}

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
{cmds}

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
{perms}
| `{low}.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus `lang/en.yml`:

```yaml
{msgs}
{p["error"][0]}: "{p["error"][1]}"
```

Das Präfix (`✦ {name} »` bzw. `✖ {full} »`) setzt das Plugin automatisch davor.

## Sprachen

Englisch, Deutsch, Französisch, Niederländisch und Italienisch sind eingebaut
(`plugins/{full}/lang/en.yml`, `de.yml`, `fr.yml`, `nl.yml`, `it.yml`).

- Jeder Spieler bekommt das Plugin in der Sprache seines Minecraft-Clients
  (`use-client-language: true`). Andere Client-Sprachen erhalten die Standardsprache.
- Standardsprache ändern: `language: de` in der `config.yml` oder im Spiel `/{low} language de`.
- Texte lassen sich in den Sprachdateien frei anpassen (MiniMessage). Neue Schlüssel aus Updates
  werden automatisch ergänzt, eigene Änderungen bleiben erhalten.

## Installation

1. `{full}-1.0.0.jar` in den Ordner `plugins` legen (Spigot, Paper oder Purpur, Minecraft {release.MC_VERSIONS}, Java 17+).
   Weitere Plugins oder Bibliotheken werden nicht benötigt.
2. Server neu starten.
3. `config.yml` und die Sprachdateien anpassen, dann `/{low} reload`.

Bauen aus dem Quellcode: im Hauptordner `mvn package`, die Jar liegt danach in `{full}/target/`.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `release/` | Release: `release.txt` (Titel, Versionstring, Beschreibung), Jar, Changelog, Bilder |
| `src/` | Quellcode, Konfiguration und Sprachdateien |

## Serie

{others}
"""


def bbcode(p):
    name, full, low = p["name"], "Ultra" + p["name"], "ultra" + p["name"].lower()
    feats = "\n".join(f"[*]{f[1]}" for f in p["features"])
    cmds = "\n".join(
        f"[*][FONT=Courier New][COLOR={CYAN}]{c}[/COLOR][/FONT] – {en(d[1])} [COLOR={GRAY}]({perm})[/COLOR]"
        for c, perm, d in p["commands"])
    perms = "\n".join(f"[*][FONT=Courier New]{perm}[/FONT] – {d[1]}"
                      for perm, d in p["extra_perms"] + [(f"{low}.*", ("", "Everything"))])
    series = "\n".join(f"[*][B]Ultra{o['name']}[/B] – {o['tagline'][1]}" for o in PLUGINS if o is not p)
    return f"""[CENTER][IMG]{RAW}/{full}/assets/banner.png[/IMG]

[SIZE=5][B]{p["tagline"][1]}[/B][/SIZE]

{p["intro"][1]}
{full} is part of the [B][COLOR={VIOLET}]Ultra[/COLOR][COLOR={CYAN}]Plugins[/COLOR][/B] series: every plugin shares the same colours, the same message layout and the same command structure.

[IMG]{RAW}/shared/assets/header-features.png[/IMG][/CENTER]
[LIST]
{feats}
[/LIST]
[CENTER]
[IMG]{RAW}/shared/assets/header-layout.png[/IMG]

No chat spam. Every confirmation, countdown and status appears in the [B]action bar[/B].
Only [B][COLOR={RED}]errors[/COLOR][/B] are sent to the chat, so players never miss them.
All messages use MiniMessage and can be changed in the language files.

[IMG]{RAW}/{full}/assets/preview.png[/IMG]

[IMG]{RAW}/shared/assets/header-languages.png[/IMG]

[B]English · Deutsch · Français · Nederlands · Italiano[/B]
Every player sees {full} in the language of their Minecraft client – automatically.
Players with any other language get your default language, which you set in the config or with [FONT=Courier New]/{low} language <code>[/FONT].
All texts live in [FONT=Courier New]lang/<code>.yml[/FONT] and can be edited freely.

[IMG]{RAW}/shared/assets/header-commands.png[/IMG][/CENTER]
[LIST]
{cmds}
[/LIST]
[CENTER]
[IMG]{RAW}/shared/assets/header-permissions.png[/IMG][/CENTER]
[LIST]
{perms}
[/LIST]
[CENTER]
[IMG]{RAW}/shared/assets/header-installation.png[/IMG]

[SIZE=4][B]Minecraft {release.MC_VERSIONS}[/B][/SIZE]
Spigot · Paper · Purpur · Java 17+[/CENTER]
[LIST=1]
[*]Download {full} and drop the jar into your [FONT=Courier New]plugins[/FONT] folder. Works on [B]Spigot, Paper and Purpur, Minecraft {release.MC_VERSIONS}[/B], no dependencies needed.
[*]Restart your server.
[*]Adjust [FONT=Courier New]config.yml[/FONT] and the files in [FONT=Courier New]lang/[/FONT], then run [FONT=Courier New]/{low} reload[/FONT].
[/LIST]
[CENTER]
[IMG]{RAW}/shared/assets/header-series.png[/IMG][/CENTER]
[LIST]
{series}
[/LIST]
[CENTER]
[IMG]{RAW}/shared/assets/header-support.png[/IMG]

Found a bug or have an idea? Please use the discussion tab or send a private message instead of leaving a bad review. We answer every request.[/CENTER]
"""


def en(text):
    return text


def root_readme():
    rows = "\n".join(
        f'| <img src="Ultra{p["name"]}/assets/logo.png" width="48"> | [**Ultra{p["name"]}**](Ultra{p["name"]}) | {p["tagline"][0]} |'
        for p in PLUGINS)
    return f"""<p align="center"><img src="shared/assets/series-banner.png" alt="UltraPlugins"></p>

# UltraPlugins

Eine Serie von Minecraft-Plugins, die sich im Spiel alle gleich anfühlen: gleiche Farben, gleiches
Nachrichten-Layout, gleiche Befehlsstruktur, gleiche Logos. Jedes Plugin hat einen eigenen Ordner.

| | Plugin | Kurzbeschreibung |
| --- | --- | --- |
{rows}

Alle Plugins sprechen **Englisch, Deutsch, Französisch, Niederländisch und Italienisch**. Jeder
Spieler bekommt automatisch die Sprache seines Minecraft-Clients; die Standardsprache stellt man mit
`language:` in der `config.yml` oder im Spiel mit `/ultra<name> language <code>` ein.

## Bauen

Voraussetzungen: Java 17+ und Maven.

```bash
mvn package
```

Danach liegt jedes Plugin als eigenständige Jar in `Ultra<Name>/target/Ultra<Name>-1.0.0.jar`.
Lauffähig auf Spigot, Paper und Purpur, Minecraft {release.MC_VERSIONS}. Jede Jar ist eigenständig, es werden keine
weiteren Plugins benötigt (Vault ist optional für Kosten und die Economy-Schnittstelle).

## Aufbau

```
pom.xml                      Maven-Hauptprojekt, baut alle Module
UltraCore/                   Gemeinsamer Code (Nachrichten, Sprachen, GUIs, Teleports), wird in jedes Plugin eingebettet
Ultra<Name>/
  README.md                  Konzept: Features, Befehle, Permissions, Sprachen
  src/                       Quellcode, config.yml, plugin.yml, lang/*.yml
  assets/                    Logo, Banner, Ingame-Vorschau
  release/                   Upload-Material für SpigotMC, BuiltByBit, Modrinth und Hangar
shared/assets/               Serien-Banner und Abschnitts-Header für SpigotMC
tools/                       Generatoren für Assets, Texte und Sprachdateien
DESIGN.md                    Gemeinsames Design-System
```

Die Regeln, die für alle Plugins gelten, stehen in [DESIGN.md](DESIGN.md).

## Übersetzungen ändern

Die Texte aller Sprachen stehen in `tools/lang/<plugin>.txt` (je Schlüssel fünf Zeilen: en, de,
fr, nl, it). `python3 tools/gen_lang.py` schreibt daraus die `lang/*.yml` der Plugins.

## Assets neu erzeugen

Inhalte stehen in `tools/plugins.py`. Nach einer Änderung:

```bash
python3 tools/generate.py && node tools/render.js
```

`render.js` braucht Node mit Playwright und die Schrift Inter. Die erzeugten Dateien werden
überschrieben – Änderungen also in `tools/plugins.py` machen, nicht in den Ausgaben.

## Veröffentlichen

Pro Plugin liegt in `Ultra<Name>/release/` die Datei `release.txt` mit Titel, Versionstring und
Beschreibung (BBCode für SpigotMC und BuiltByBit), `release-markdown.txt` mit demselben Inhalt in
Markdown (Modrinth und Hangar), die Jar und die Bilder. Übersicht: [RELEASE.md](RELEASE.md).

Die Bilder in den Beschreibungen verweisen auf `raw.githubusercontent.com/.../main/...`. Sie werden
erst angezeigt, wenn dieses Repository öffentlich ist und die Dateien auf `main` liegen. Sonst die
PNGs bei einem Bildhoster hochladen und die Links in den Beschreibungen ersetzen.
"""


def write(path, text):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def main():
    jobs = []  # (source, png, width, height, transparent)
    for p in PLUGINS:
        d = f"Ultra{p['name']}"
        write(f"{d}/assets/logo.svg", logo_svg(p["name"]))
        write(f"{d}/assets/banner.svg", banner_svg(p))
        write(f"tools/.build/{d}-preview.html", preview_html(p))
        write(f"{d}/README.md", concept_md(p))
        jobs += [(f"{d}/assets/logo.svg", f"{d}/assets/logo.png", 512, 512, True),
                 (f"{d}/assets/banner.svg", f"{d}/assets/banner.png", 1920, 480, False),
                 (f"tools/.build/{d}-preview.html", f"{d}/assets/preview.png", 1280, 420, False)]
    for title in ["Features", "In-game Layout", "Languages", "Commands", "Permissions", "Installation", "The Ultra Series", "Support"]:
        slug = re.sub(r"[^a-z]+", "-", title.lower()).strip("-").replace("in-game-", "").replace("the-ultra-", "")
        write(f"shared/assets/header-{slug}.svg", header_svg(title))
        jobs.append((f"shared/assets/header-{slug}.svg", f"shared/assets/header-{slug}.png", 1920, 150, False))
    write("shared/assets/series-banner.svg", series_svg())
    jobs.append(("shared/assets/series-banner.svg", "shared/assets/series-banner.png", 1920, 520, False))
    release.build(write, jobs, bbcode, logo_svg, DEFS, logo_body)
    write("README.md", root_readme())
    write("tools/.build/jobs.json", json.dumps(jobs))
    print(f"{len(jobs)} render jobs written")


if __name__ == "__main__":
    main()

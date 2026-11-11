"""Release material for SpigotMC, BuiltByBit, Modrinth and Hangar.

Called by generate.py. Writes, per plugin:

    Ultra<Name>/release/
      README.md                     upload checklist for all four sites
      spigotmc/description.bbcode
      builtbybit/description.bbcode
      modrinth/description.md
      hangar/description.md
      images/                       icon-512, icon-256, banner, cover, gallery-*.png
"""
import html
import re

import yaml

from details import COMMON_FEATURES, DETAILS
from plugins import ICONS, PLUGINS
import storepage

RAW = "https://raw.githubusercontent.com/creativedm0031/UltraPlugins/main"
VERSION = "1.0.0"
MC_MIN, MC_MAX = "1.20.4", "26.3"
MC_VERSIONS = f"{MC_MIN} – {MC_MAX}"
# Every Minecraft release in that range, for the version pickers on Modrinth and Hangar.
MC_LIST = ["1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7",
           "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3"]
# Paper servers the release jars were started on (all plugins), and how far the test went.
TESTED = [("1.20.4", "bot test"), ("1.20.6", "bot test"), ("1.21.1", "bot test"), ("1.21.4", "full bot test"),
          ("1.21.8", "bot test"), ("1.21.11", "bot test"), ("26.1.2", "bot test"), ("26.3", "start + console")]
JAVA = "Java 17+ (Minecraft 1.20.5+ needs Java 21, 26.x needs Java 25 – that is a server requirement)"
VIOLET, CYAN, GRAY, RED, RED_SOFT, SEP = "#8B5CF6", "#22D3EE", "#94A3B8", "#F87171", "#FCA5A5", "#64748B"
LANGS = [("en", "English"), ("de", "Deutsch"), ("fr", "Français"), ("nl", "Nederlands"), ("it", "Italiano")]

# Categories per site (closest match from each site's own list).
CATEGORIES = {
    "Spawn":     ("Tools and Utilities", "utility, management", "Admin Tools"),
    "Teleports": ("Tools and Utilities", "utility, transportation", "Gameplay"),
    "Punish":    ("Tools and Utilities", "management, utility", "Admin Tools"),
    "Homes":     ("Tools and Utilities", "utility, transportation", "Gameplay"),
    "Warps":     ("Tools and Utilities", "utility, transportation", "Gameplay"),
    "Chat":      ("Chat", "social, utility", "Chat"),
    "Economy":   ("Economy", "economy", "Economy"),
    "Kits":      ("Tools and Utilities", "equipment, utility", "Gameplay"),
    "Vanish":    ("Tools and Utilities", "management, utility", "Admin Tools"),
    "Reports":   ("Tools and Utilities", "management, social", "Admin Tools"),
    "Ranks":     ("Tools and Utilities", "management, utility, social", "Admin Tools"),
}

# One message per plugin, shown in all five languages on the language gallery image.
LANG_SAMPLE = {
    "Spawn": ("spawn-teleport", {}),
    "Teleports": ("request-sent", {"player": "Steve"}),
    "Punish": ("notify-mute", {"player": "Steve", "staff": "Alex", "duration": "1h", "reason": "Spam"}),
    "Homes": ("home-set", {"name": "base"}),
    "Warps": ("warp-teleport", {"name": "shop"}),
    "Chat": ("channel-switched", {"channel": "staff"}),
    "Economy": ("pay-sent", {"amount": "$250.00", "player": "Steve"}),
    "Kits": ("kit-received", {"name": "starter"}),
    "Vanish": ("vanish-status", {"count": "12"}),
    "Reports": ("report-sent", {"player": "Steve"}),
    "Ranks": ("rank-received", {"rank": "<gradient:#FACC15:#F97316>VIP</gradient>", "duration": "30d"}),
}

# Plugins with a main GUI: title key + vars, hovered item name, lore key + vars, number of filled slots.
GUI_SAMPLE = {
    "Punish": (("punish-title", {"player": "Steve"}), "Spam", ("template-lore", {"type": "Mute", "duration": "30m"}), 6),
    "Homes": (("homes-title", {"count": "3", "limit": "5"}), "base", ("home-lore", {"world": "world", "x": "128", "y": "64", "z": "-342", "public": "no"}), 3),
    "Warps": (("warps-title", {}), "shop", ("warp-lore", {"category": "general", "owner": "Server", "visits": "214"}), 9),
    "Economy": (("baltop-title", {"currency": "coins"}), "#1 Steve", ("baltop-lore", {"balance": "$48,250.00"}), 12),
    "Kits": (("kits-title", {}), "starter", ("kit-lore", {"status": "ready", "items": "9", "cost": "free"}), 5),
    "Reports": (("inbox-title", {"state": "open"}), "#42 Steve", ("report-lore", {"reason": "Cheating", "reporter": "Alex", "date": "2026-11-10 18:04", "handler": "–", "online": "online"}), 4),
    "Ranks": (("perms-title", {"rank": "VIP"}), "ultrahomes.limit.5", ("perm-lore", {"state": "allowed"}), 7),
}


def lang(module, code):
    """All texts of one plugin in one language (core texts included)."""
    from pathlib import Path
    root = Path(__file__).resolve().parent.parent
    texts = {}
    for f in (root / f"UltraCore/src/main/resources/core-lang/{code}.yml", root / f"Ultra{module}/src/main/resources/lang/{code}.yml"):
        texts.update(yaml.safe_load(f.read_text(encoding="utf-8")) or {})
    return texts


# ---- MiniMessage -> HTML (only the tags the language files use) ------------------------------------------
STYLES = {
    "accent": f"color:{CYAN}", "white": "color:#fff", "muted": f"color:{GRAY}", "sep": f"color:{SEP}",
    "error": f"color:{RED}", "errortext": f"color:{RED_SOFT}", "green": "color:#4ade80", "primary": f"color:{VIOLET}",
    "bold": "font-weight:800", "dark_gray": "color:#555",
    "brand": f"background:linear-gradient(90deg,{VIOLET},{CYAN});-webkit-background-clip:text;color:transparent;text-shadow:none;filter:drop-shadow(3px 3px 0 rgba(0,0,0,.55))",
}


def mm_html(text, variables=None):
    for key, value in (variables or {}).items():
        text = text.replace(f"<{key}>", value)
    out, depth = [], 0
    for token in re.split(r"(<[^<>]+>)", text):
        tag = token[1:-1] if token.startswith("<") and token.endswith(">") else None
        if tag is None:
            out.append(html.escape(token).replace("\n", "<br>"))
        elif tag.startswith("/"):
            name = tag[1:].split(":")[0]
            if name in STYLES:
                out.append("</span>")
                depth -= 1
        elif tag.split(":")[0] in ("click", "hover"):
            continue
        elif tag in STYLES:
            out.append(f'<span style="{STYLES[tag]}">')
            depth += 1
        else:
            out.append(html.escape(token))
    return "".join(out) + "</span>" * max(0, depth)


def icon_svg(name, size, stroke=1.8):
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24"><defs><linearGradient id="g" gradientUnits="userSpaceOnUse" '
            f'x1="2" y1="2" x2="22" y2="22"><stop offset="0" stop-color="{VIOLET}"/><stop offset="1" stop-color="{CYAN}"/></linearGradient></defs>'
            f'<g fill="none" stroke="url(#g)" stroke-width="{stroke}" stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</g></svg>')


# ---- gallery pages ---------------------------------------------------------------------------------------
BASE_CSS = f"""*{{margin:0;box-sizing:border-box}}
body{{width:1600px;height:900px;overflow:hidden;font-family:Inter,sans-serif;color:#fff;
 background:radial-gradient(circle at 8% 0%,rgba(139,92,246,.35),transparent 45%),radial-gradient(circle at 95% 100%,rgba(34,211,238,.25),transparent 45%),linear-gradient(135deg,#0B1020,#161D3F)}}
body:after{{content:"";position:absolute;left:0;right:0;bottom:0;height:10px;background:linear-gradient(90deg,{VIOLET},{CYAN})}}
.head{{position:absolute;left:80px;top:60px;display:flex;align-items:center;gap:22px}}
.tile{{width:84px;height:84px;border-radius:20px;border:3px solid;border-image:none;background:#0f1530;display:flex;align-items:center;justify-content:center;box-shadow:0 0 0 3px rgba(139,92,246,.6) inset}}
.kicker{{font-size:20px;font-weight:600;color:{GRAY};letter-spacing:.12em;text-transform:uppercase}}
.title{{font-size:52px;font-weight:800;letter-spacing:-1px}}
.title b{{background:linear-gradient(90deg,{VIOLET},{CYAN});-webkit-background-clip:text;color:transparent}}
.mc{{font-family:'DejaVu Sans Mono',monospace;font-weight:700;text-shadow:3px 3px 0 rgba(0,0,0,.6)}}
.note{{position:absolute;right:80px;top:84px;font-size:16px;color:{GRAY};letter-spacing:.08em;text-transform:uppercase}}
"""


def page(p, kicker, title, body, css=""):
    return f"""<!doctype html><meta charset="utf-8"><style>{BASE_CSS}{css}</style>
<div class="head"><div class="tile">{icon_svg(p["name"], 52)}</div>
<div><div class="kicker">Ultra{p["name"]} · {kicker}</div><div class="title">{title}</div></div></div>
<div class="note">Mock-up</div>
{body}"""


def gallery_layout(p):
    m, en = p["name"], lang(p["name"], "en")
    info = mm_html(p["messages"][0][1].replace("<c>", "<accent>").replace("</c>", "</accent>"))
    info2 = mm_html(p["messages"][1][1].replace("<c>", "<accent>").replace("</c>", "</accent>"))
    error = html.escape(p["error"][1])
    slots = "".join("<i></i>" for _ in range(9))
    css = f""".scene{{position:absolute;left:80px;right:80px;top:200px;bottom:70px;border-radius:24px;overflow:hidden;
 background:linear-gradient(#2b3a6b 0%,#3e5292 52%,#22302a 52%,#151e19 100%);box-shadow:0 30px 80px rgba(0,0,0,.45)}}
.scene:before{{content:"";position:absolute;inset:0;background:rgba(8,12,26,.42)}}
.chat{{position:absolute;left:0;top:70px;font-size:24px;line-height:1.55}}
.chat div{{background:rgba(0,0,0,.5);padding:4px 18px}}
.ab{{position:absolute;left:0;right:0;bottom:120px;text-align:center;font-size:28px}}
.bar{{position:absolute;left:50%;bottom:28px;transform:translateX(-50%);display:flex;border:3px solid #0b0b0b;background:rgba(0,0,0,.45)}}
.bar i{{width:66px;height:66px;border:3px solid #6f6f6f;border-right-color:#3a3a3a;border-bottom-color:#3a3a3a}}
.lbl{{position:absolute;font-size:15px;font-weight:600;color:{GRAY};letter-spacing:.1em;text-transform:uppercase;font-family:Inter}}
.side{{position:absolute;right:40px;top:40px;width:430px;font-size:20px;line-height:1.5;color:#dbe3f0}}
.side h3{{font-size:24px;margin-bottom:10px;color:#fff}}"""
    body = f"""<div class="scene">
<div class="lbl" style="left:18px;top:40px">Chat · errors only</div>
<div class="chat mc"><div><span style="color:{RED}">✖ <b>Ultra{m}</b></span> <span style="color:{SEP}">»</span> <span style="color:{RED_SOFT}">{error}</span></div></div>
<div class="side"><h3>Clean by design</h3>Feedback, countdowns and status appear in the action bar.<br>Only errors land in the chat – so nobody misses them.</div>
<div class="lbl" style="left:0;right:0;text-align:center;bottom:172px">Action bar · everything else</div>
<div class="ab mc"><span style="{STYLES['brand']}">✦ {m}</span> <span style="color:{SEP}">»</span> {info}</div>
<div class="bar">{slots}</div></div>"""
    return page(p, "In-game layout", "Action bar first. <b>No chat spam.</b>", body, css)


def gallery_languages(p):
    m = p["name"]
    key, variables = LANG_SAMPLE[m]
    rows = ""
    for code, label in LANGS:
        text = mm_html(lang(m, code)[key], variables)
        rows += (f'<div class="row"><div class="code">{code.upper()}<small>{label}</small></div>'
                 f'<div class="line mc"><span style="{STYLES["brand"]}">✦ {m}</span> <span style="color:{SEP}">»</span> <span style="color:#fff">{text}</span></div></div>')
    css = f""".rows{{position:absolute;left:80px;right:80px;top:200px;display:flex;flex-direction:column;gap:16px}}
.row{{display:flex;align-items:center;gap:28px;background:rgba(9,13,30,.65);border:1px solid rgba(148,163,184,.15);border-radius:18px;padding:16px 30px}}
.code{{width:150px;font-size:34px;font-weight:800;background:linear-gradient(90deg,{VIOLET},{CYAN});-webkit-background-clip:text;color:transparent;display:flex;flex-direction:column}}
.code small{{font-size:16px;font-weight:600;color:{GRAY};-webkit-text-fill-color:{GRAY};letter-spacing:.06em}}
.line{{font-size:27px}}
.foot{{position:absolute;left:80px;bottom:40px;font-size:21px;color:{GRAY}}}"""
    body = f"""<div class="rows">{rows}</div>
<div class="foot">Every player automatically gets the language of their Minecraft client.</div>"""
    return page(p, "5 languages", "Speaks your players' <b>language.</b>", body, css)


def gallery_gui(p):
    m = p["name"]
    (title_key, title_vars), item, (lore_key, lore_vars), filled = GUI_SAMPLE[m]
    en = lang(m, "en")
    title = "<span style='color:%s;font-weight:800'>Ultra</span> <span style='color:#555'>» %s</span>" % (VIOLET, mm_html(en[title_key], title_vars))
    cells = ""
    content = [r * 9 + c for r in range(1, 5) for c in range(1, 8)]
    for slot in range(54):
        row, col = divmod(slot, 9)
        frame = row in (0, 5) or col in (0, 8)
        inner = ""
        cls = "f" if frame else ""
        if not frame and content.index(slot) < filled:
            inner = icon_svg(m, 40, 2.2)
            if content.index(slot) == 0:
                cls = "h"
        nav = {45: "←", 49: "✕", 53: "→"}.get(slot)
        if nav:
            inner, cls = f'<span class="nav">{nav}</span>', ""
        cells += f'<div class="{cls}">{inner}</div>'
    lore = "".join(f"<div>{mm_html('<muted>' + line, lore_vars) or '&nbsp;'}</div>" for line in en[lore_key].split("\n"))
    css = f""".inv{{position:absolute;left:150px;top:220px;width:676px;padding:22px;background:#c6c6c6;border-radius:6px;
 border:4px solid #fff;border-right-color:#555;border-bottom-color:#555;box-shadow:0 30px 80px rgba(0,0,0,.5)}}
.inv h4{{font-family:'DejaVu Sans Mono',monospace;font-size:22px;margin:0 0 14px 2px;font-weight:700}}
.grid{{display:grid;grid-template-columns:repeat(9,70px);gap:0}}
.grid div{{width:70px;height:70px;background:#8b8b8b;border:3px solid #373737;border-right-color:#fff;border-bottom-color:#fff;display:flex;align-items:center;justify-content:center}}
.grid div.f{{background:#1d1d24}}
.grid div.h{{background:#b9b9d6}}
.nav{{font-size:34px;font-weight:800;color:#222}}
.tip{{position:absolute;left:330px;top:350px;min-width:420px;padding:16px 22px;background:#100010f2;border:3px solid #2a0a5e;border-radius:6px;
 font-family:'DejaVu Sans Mono',monospace;font-weight:700;font-size:22px;line-height:1.45;box-shadow:0 20px 50px rgba(0,0,0,.5)}}
.tip .n{{font-size:24px;margin-bottom:4px}}
.side{{position:absolute;left:960px;right:80px;top:250px;font-size:24px;line-height:1.55;color:#dbe3f0}}
.side li{{margin:0 0 14px 0;list-style:none}}
.side li:before{{content:"▸ ";color:{CYAN}}}"""
    side = "".join(f"<li>{html.escape(f[1])}</li>" for f in p["features"][:4])
    body = f"""<div class="inv"><h4>{title}</h4><div class="grid">{cells}</div></div>
<div class="tip"><div class="n"><span style="{STYLES['brand']}">{html.escape(item)}</span></div>{lore}</div>
<ul class="side">{side}</ul>"""
    return page(p, "GUI", "Everything in <b>one menu.</b>", body, css)


def gallery_commands(p):
    rows = "".join(f'<div class="c"><code>{html.escape(c)}</code><span>{html.escape(d[1])}</span><em>{html.escape(perm)}</em></div>'
                   for c, perm, d in p["commands"])
    css = f""".list{{position:absolute;left:80px;right:80px;top:220px;display:grid;grid-template-columns:1fr 1fr;gap:16px}}
.c{{background:rgba(9,13,30,.65);border:1px solid rgba(148,163,184,.15);border-radius:16px;padding:20px 26px;display:flex;flex-direction:column;gap:6px}}
.c code{{font-family:'DejaVu Sans Mono',monospace;font-size:22px;font-weight:700;color:{CYAN}}}
.c span{{font-size:21px;color:#e2e8f0}}
.c em{{font-style:normal;font-size:16px;color:{GRAY}}}"""
    return page(p, "Commands", "Short commands. <b>Full tab completion.</b>", f'<div class="list">{rows}</div>', css)


def cover_svg(p, defs, logo_body):
    name = p["name"]
    chips = f'<tspan fill="{CYAN}">  •  </tspan>'.join(html.escape(c) for c in p["chips"] + ["5 Languages"])
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 720" width="1280" height="720">
{defs}
<rect width="1280" height="720" fill="url(#bg)"/><rect width="1280" height="720" fill="url(#grid)"/>
<circle cx="150" cy="0" r="420" fill="{VIOLET}" opacity=".28" filter="url(#blur)"/>
<circle cx="1180" cy="720" r="420" fill="{CYAN}" opacity=".2" filter="url(#blur)"/>
<rect y="710" width="1280" height="10" fill="url(#edge)"/>
<g transform="translate(490 110) scale(.5859375)">{logo_body}</g>
<text x="640" y="490" text-anchor="middle" font-family="Inter, Arial, sans-serif" font-size="100" font-weight="800" letter-spacing="-2" fill="#fff">Ultra<tspan fill="{CYAN}">{name}</tspan></text>
<text x="640" y="552" text-anchor="middle" font-family="Inter, Arial, sans-serif" font-size="32" font-weight="500" fill="{GRAY}">{html.escape(p["tagline"][1])}</text>
<text x="640" y="618" text-anchor="middle" font-family="Inter, Arial, sans-serif" font-size="24" font-weight="600" fill="#fff" xml:space="preserve">{chips}</text>
</svg>
"""


# ---- descriptions ----------------------------------------------------------------------------------------
def gallery_names(p):
    names = ["layout", "languages"] + (["gui"] if p["name"] in GUI_SAMPLE else []) + ["commands"]
    return [f"gallery-{i + 1}-{n}" for i, n in enumerate(names)]


def checklist(p):
    name, full = p["name"], "Ultra" + p["name"]
    spigot_cat, modrinth_cat, hangar_cat = CATEGORIES[name]
    mc_list = ", ".join(f"`{v}`" for v in MC_LIST)
    tested = "\n".join(f"| {v} | {t} |" for v, t in TESTED)
    gallery = "\n".join(f"- `images/{n}.png`" for n in ["cover"] + gallery_names(p))
    return f"""# {full} – Release

| Datei | Inhalt |
| --- | --- |
| [`release.txt`](release.txt) | **Titel, Versionstring, Beschreibung** (BBCode) – für SpigotMC und BuiltByBit |
| [`release-markdown.txt`](release-markdown.txt) | dasselbe mit Beschreibung in Markdown – für Modrinth und Hangar |
| [`{full}-{VERSION}.jar`]({full}-{VERSION}.jar) | die Plugin-Datei zum Hochladen |
| [`changelog.txt`](changelog.txt) | Changelog dieser Version |
| `images/` | Icon, Banner, Cover und Beispielbilder |

Titel, Versionstring und Beschreibung sind auf allen vier Seiten gleich. Es gibt nur zwei Dateien,
weil SpigotMC und BuiltByBit BBCode verstehen, Modrinth und Hangar dagegen Markdown.

## Bilder

| Datei | Wofür |
| --- | --- |
| `icon-512.png` | Icon auf SpigotMC, BuiltByBit, Hangar |
| `icon-256.png` | Icon auf Modrinth (max. 256 KiB) |
| `banner.png` | steht oben in der Beschreibung |
| `cover.png` | Cover auf BuiltByBit, erstes Galeriebild auf Modrinth |
| `gallery-*.png` | Galerie auf Modrinth / BuiltByBit |

Galerie-Reihenfolge:
{gallery}

## Felder auf den Seiten

| Feld | SpigotMC | BuiltByBit | Modrinth | Hangar |
| --- | --- | --- | --- | --- |
| Kategorie | {spigot_cat} | Plugins → Spigot | {modrinth_cat} | {hangar_cat} |
| Loader / Plattform | Spigot | Spigot, Paper | Bukkit, Spigot, Paper, Purpur | Paper |

Minecraft-Versionen zum Ankreuzen ({MC_VERSIONS}): {mc_list}

Hangar macht aus dem Projektnamen die URL. Falls das Namensfeld dort keine Sonderzeichen oder
Klammern annimmt, `{full}` eintragen – der vollständige Titel steht ohnehin oben in der Beschreibung.

## Getestet

Alle Plugins auf Paper gestartet:

| Minecraft | Test |
| --- | --- |
{tested}
"""


def changelog(p):
    full = "Ultra" + p["name"]
    d = DETAILS[p["name"]]
    feats = "\n".join(f"- {f[1][0]}" for f in d["features"] + COMMON_FEATURES)
    return f"""{full} {VERSION}

First release.

{feats}

Minecraft {MC_VERSIONS} · Spigot, Paper, Purpur · Java 17+
"""


def overview():
    rows = "\n".join(
        f"| [Ultra{p['name']}](Ultra{p['name']}/release) | {storepage.title(p['name'], MC_MIN, MC_MAX).replace('|', chr(92) + '|')} | `{VERSION}` | [Ultra{p['name']}-{VERSION}.jar](Ultra{p['name']}/release/Ultra{p['name']}-{VERSION}.jar) |"
        for p in PLUGINS)
    return f"""# Release-Übersicht

Pro Plugin liegt in `Ultra<Name>/release/`:

- `release.txt` – Titel, Versionstring und Beschreibung (BBCode) für SpigotMC und BuiltByBit
- `release-markdown.txt` – derselbe Inhalt mit Markdown-Beschreibung für Modrinth und Hangar
- die Jar, `changelog.txt`, Bilder und eine Checkliste (`README.md`)

Alle Plugins nutzen denselben Aufbau. Neu bauen und die Jars kopieren: `tools/package.sh`.

| Plugin | Titel | Versionstring | Jar |
| --- | --- | --- | --- |
{rows}
"""


def build(write, jobs, bbcode, logo_svg, defs, logo_body):
    write("RELEASE.md", overview())
    for p in PLUGINS:
        full = "Ultra" + p["name"]
        base = f"{full}/release"
        banner = f"{RAW}/{base}/images/banner.png"
        name = p["name"]
        write(f"{base}/release.txt", storepage.release_file(
            name, VERSION, storepage.bbcode(name, banner, MC_MIN, MC_VERSIONS), "BBCode – SpigotMC, BuiltByBit", MC_MIN, MC_MAX))
        write(f"{base}/release-markdown.txt", storepage.release_file(
            name, VERSION, storepage.markdown(name, banner, MC_MIN, MC_VERSIONS), "Markdown – Modrinth, Hangar", MC_MIN, MC_MAX))
        write(f"{base}/README.md", checklist(p))
        write(f"{base}/changelog.txt", changelog(p))

        svg512 = logo_svg(p["name"])
        write(f"tools/.build/{full}-icon-256.svg", svg512.replace('width="512" height="512"', 'width="256" height="256"'))
        write(f"tools/.build/{full}-cover.svg", cover_svg(p, defs, logo_body(p["name"])))
        jobs += [(f"{full}/assets/logo.svg", f"{base}/images/icon-512.png", 512, 512, True),
                 (f"tools/.build/{full}-icon-256.svg", f"{base}/images/icon-256.png", 256, 256, True),
                 (f"{full}/assets/banner.svg", f"{base}/images/banner.png", 1920, 480, False),
                 (f"tools/.build/{full}-cover.svg", f"{base}/images/cover.png", 1280, 720, False)]
        pages = [gallery_layout(p), gallery_languages(p)] + ([gallery_gui(p)] if p["name"] in GUI_SAMPLE else []) + [gallery_commands(p)]
        for name, content in zip(gallery_names(p), pages):
            write(f"tools/.build/{full}-{name}.html", content)
            jobs.append((f"tools/.build/{full}-{name}.html", f"{base}/images/{name}.png", 1600, 900, False))

"""Title, version string and description for the store pages – identical for every plugin and site.

release.txt           BBCode description (SpigotMC, BuiltByBit)
release-markdown.txt  the same content as Markdown (Modrinth, Hangar)
"""
import re
from pathlib import Path

import yaml

from details import COMMON_FEATURES, DETAILS

ROOT = Path(__file__).resolve().parent.parent
LINE = "-" * 98
ORNAMENT = "──────── ✦ ────────"
RATING = "Please leave a ⭐⭐⭐⭐⭐ rating if you like the plugin!"

# Permissions that are not tied to one command: suffix after "ultra<name>." -> (English, German)
PERM_DESC = {
    "*": ("All permissions of the plugin", "Alle Rechte des Plugins"),
    "admin": ("Help, reload, version and language", "Hilfe, Reload, Version und Sprache"),
    "bypass.warmup": ("Skip the teleport warm-up", "Kein Teleport-Warmup"),
    "bypass.cooldown": ("Skip cooldowns", "Keine Cooldowns"),
    "spawn.others": ("Send other players to spawn", "Andere Spieler zum Spawn schicken"),
    "group.<name>": ("Spawn at the spawn <name> (e.g. a VIP spawn)", "Spawnt am Spawn <name> (z. B. VIP-Spawn)"),
    "others": ("Visit any home", "Jedes Home besuchen"),
    "limit.<number>": ("Maximum number of homes", "Maximale Anzahl Homes"),
    "warp.<name>": ("Access a warp when per-warp-permission is on", "Zugriff auf einen Warp bei per-warp-permission"),
    "player.limit.<number>": ("Maximum number of player warps", "Maximale Anzahl Spieler-Warps"),
    "notify": ("Receive staff notifications", "Erhält Team-Benachrichtigungen"),
    "exempt": ("Cannot be punished / reported", "Kann nicht bestraft / gemeldet werden"),
    "channel.staff": ("Read and write in the staff channel", "Team-Kanal lesen und schreiben"),
    "color": ("Use &-colour codes in chat", "&-Farbcodes im Chat nutzen"),
    "format.<name>": ("Use the chat format <name>", "Chat-Format <name> nutzen"),
    "bypass.filter": ("Bypass the chat filter", "Chat-Filter umgehen"),
    "bypass.slowmode": ("Bypass the slow mode", "Slow-Mode umgehen"),
    "bypass.mutechat": ("Write while the chat is locked", "Schreiben trotz gesperrtem Chat"),
    "bypass.clear": ("Keep your chat on /chatclear", "Chat bleibt bei /chatclear erhalten"),
    "balance.others": ("See other players' balances", "Kontostand anderer sehen"),
    "log": ("View the transaction log", "Transaktions-Log einsehen"),
    "give": ("Give kits to other players", "Kits an andere Spieler geben"),
    "kit.<name>": ("Claim the kit <name>", "Kit <name> abholen"),
    "vanish.others": ("Vanish other players", "Andere Spieler unsichtbar machen"),
    "see": ("See vanished players (same or lower level)", "Unsichtbare Spieler sehen (gleiches oder niedrigeres Level)"),
    "level.<number>": ("Vanish level", "Vanish-Level"),
}
# Permissions that only exist as patterns, not in plugin.yml.
PATTERN_PERMS = {
    "Spawn": ["group.<name>"], "Homes": ["limit.<number>"], "Warps": ["warp.<name>", "player.limit.<number>"],
    "Chat": ["format.<name>"], "Kits": ["kit.<name>"], "Vanish": ["level.<number>"],
}
OPTIONAL = {
    "Teleports": ("Vault (optional, for /rtp costs)", "Vault (optional, für /rtp-Kosten)"),
    "Warps": ("Vault (optional, for player warp fees)", "Vault (optional, für Spieler-Warp-Gebühren)"),
    "Kits": ("Vault (optional, for kit prices)", "Vault (optional, für Kit-Preise)"),
    "Economy": ("Vault (optional, to provide the economy to other plugins)", "Vault (optional, um die Economy anderen Plugins bereitzustellen)"),
    "Reports": ("UltraPunish (optional, punish from a report)", "UltraPunish (optional, aus dem Report bestrafen)"),
}
DEFAULTS = {True: ("everyone", "alle"), "op": ("OP", "OP"), False: ("nobody", "niemand"), "not op": ("non-OPs", "Nicht-OPs")}


def title(name, mc_min, mc_max):
    return f"Ultra{name} [{mc_min}+ - {mc_max}] ✦ {DETAILS[name]['subtitle']}"


def _lang(name, code):
    texts = {}
    for f in (ROOT / f"UltraCore/src/main/resources/core-lang/{code}.yml", ROOT / f"Ultra{name}/src/main/resources/lang/{code}.yml"):
        texts.update(yaml.safe_load(f.read_text(encoding="utf-8")) or {})
    return texts


def commands(name):
    """[(usage, aliases, permission, default, en, de)] in plugin.yml order."""
    low = "ultra" + name.lower()
    meta = yaml.safe_load((ROOT / f"Ultra{name}/src/main/resources/plugin.yml").read_text(encoding="utf-8"))
    source = (ROOT / f"Ultra{name}/src/main/java/de/creativedm/ultra/{name.lower()}/Ultra{name}.java").read_text(encoding="utf-8")
    perm_of = dict(re.findall(r'command\("(\w+)", "([\w.]+)"', source))
    perm_of[low] = f"{low}.admin"
    if name == "Punish":
        perm_of.update({"ban": "ultrapunish.ban", "tempban": "ultrapunish.ban", "banip": "ultrapunish.ban",
                        "mute": "ultrapunish.mute", "tempmute": "ultrapunish.mute", "kick": "ultrapunish.kick", "warn": "ultrapunish.warn"})
    perms = meta.get("permissions", {})
    en, de = _lang(name, "en"), _lang(name, "de")
    out = []
    for cmd, info in meta["commands"].items():
        perm = perm_of[cmd]
        out.append((info["usage"].replace("<command>", cmd), info.get("aliases", []), perm,
                    DEFAULTS[perms.get(perm, {}).get("default", "op")], en.get(f"help-{cmd}", ""), de.get(f"help-{cmd}", "")))
    return out


def other_permissions(name):
    """[(permission, default, en, de)] that are not command permissions."""
    low = "ultra" + name.lower()
    meta = yaml.safe_load((ROOT / f"Ultra{name}/src/main/resources/plugin.yml").read_text(encoding="utf-8"))
    used = {c[2] for c in commands(name)}
    out = []
    for perm, info in meta.get("permissions", {}).items():
        if perm in used:
            continue
        en, de = PERM_DESC[perm[len(low) + 1:]]
        out.append((perm, DEFAULTS[info.get("default", "op")], en, de))
    for suffix in PATTERN_PERMS.get(name, []):
        en, de = PERM_DESC[suffix]
        out.append((f"{low}.{suffix}", ("–", "–"), en, de))
    return out


def config(name):
    return (ROOT / f"Ultra{name}/src/main/resources/config.yml").read_text(encoding="utf-8").strip()


# ---- one content model, two renderers --------------------------------------------------------------------
def _section(name, lang, mc_versions):
    """Text pieces of one language block."""
    i = 0 if lang == "en" else 1
    d = DETAILS[name]
    feats = [(x[0], x[1 + i][0], x[1 + i][1]) for x in d["features"] + COMMON_FEATURES]
    req_parts = ["Spigot, Paper, Purpur", f"Minecraft {mc_versions}", "Java 17+"]
    if lang == "en":
        req_parts.append("no required dependencies")
    else:
        req_parts.append("keine Pflicht-Abhängigkeiten")
    if name in OPTIONAL:
        req_parts.append(OPTIONAL[name][i])
    return {
        "heading": "English Version" if lang == "en" else "Deutsche Version",
        "intro": d["intro"][i],
        "features_title": "⭐ Features at a glance:" if lang == "en" else "⚙️ Deine Vorteile:",
        "features": feats,
        "req_title": "📦 Requirements:" if lang == "en" else "📦 Voraussetzungen:",
        "req": " · ".join(req_parts),
        "lang_title": "🌍 Languages:" if lang == "en" else "🌍 Sprachen:",
        "lang": ("English, Deutsch, Français, Nederlands, Italiano – every player automatically gets the language of their Minecraft client. Change the default with /ultra{low} language <code>."
                 if lang == "en" else
                 "English, Deutsch, Français, Nederlands, Italiano – jeder Spieler bekommt automatisch die Sprache seines Minecraft-Clients. Standardsprache ändern mit /ultra{low} language <code>.").replace("{low}", name.lower()),
        "cmd_title": "Commands & Permissions (EN)" if lang == "en" else "Befehle & Rechte (DE)",
        "commands": [(u, a, p, dflt[i], (en if lang == "en" else de)) for u, a, p, dflt, en, de in commands(name)],
        "perm_title": "More permissions:" if lang == "en" else "Weitere Rechte:",
        "perms": [(p, dflt[i], (en if lang == "en" else de)) for p, dflt, en, de in other_permissions(name)],
        "default_word": "default" if lang == "en" else "Standard",
        "alias_word": "aliases" if lang == "en" else "Aliase",
        "cfg_title": "View Full Configuration (EN)" if lang == "en" else "Vollständige Konfiguration anzeigen (DE)",
    }


def bbcode(name, banner, mc_min, mc_versions):
    out = [f"[CENTER][IMG]{banner}[/IMG]", "",
           f"[SIZE=6][B]✦ Ultra{name} [{mc_min}+] ✦[/B][/SIZE]",
           f"[SIZE=5][I]{DETAILS[name]['slogan']}[/I][/SIZE]",
           f"{ORNAMENT}[/CENTER]", ""]
    for lang in ("en", "de"):
        s = _section(name, lang, mc_versions)
        out += [LINE, f"[SIZE=5][B]{s['heading']}[/B][/SIZE]", s["intro"], "", f"[B]{s['features_title']}[/B]", "[LIST]"]
        out += [f"[*]{e} [B]{h}:[/B] {t}" for e, h, t in s["features"]]
        out += ["[/LIST]", "", f"[B]{s['lang_title']}[/B] {s['lang']}", f"[B]{s['req_title']}[/B] {s['req']}", ""]
        out += [f"[SPOILER={s['cmd_title']}]", "[LIST]"]
        for usage, aliases, perm, dflt, text in s["commands"]:
            alias = f" ({s['alias_word']}: {', '.join('/' + a for a in aliases)})" if aliases else ""
            out.append(f"[*][B]{usage}[/B]{alias} – {text} · [I]{perm}[/I] ({s['default_word']}: {dflt})")
        out += ["[/LIST]", f"[B]{s['perm_title']}[/B]", "[LIST]"]
        for perm, dflt, text in s["perms"]:
            suffix = "" if dflt == "–" else f" ({s['default_word']}: {dflt})"
            out.append(f"[*][I]{perm}[/I] – {text}{suffix}")
        out += ["[/LIST]", "[/SPOILER]", f"[SPOILER={s['cfg_title']}]", "[CODE]", config(name), "[/CODE]", "[/SPOILER]", ""]
    out += [LINE, "", f"[CENTER][B]{RATING}[/B][/CENTER]"]
    return "\n".join(out) + "\n"


def markdown(name, banner, mc_min, mc_versions):
    out = [f"![Ultra{name}]({banner})", "",
           f"# ✦ Ultra{name} [{mc_min}+] ✦", "",
           f"*{DETAILS[name]['slogan']}*", "", ORNAMENT, ""]
    for lang in ("en", "de"):
        s = _section(name, lang, mc_versions)
        out += ["---", "", f"## {s['heading']}", "", s["intro"], "", f"### {s['features_title']}", ""]
        out += [f"- {e} **{h}:** {t}" for e, h, t in s["features"]]
        out += ["", f"**{s['lang_title']}** {s['lang']}", "", f"**{s['req_title']}** {s['req']}", "",
                f"<details><summary>{s['cmd_title']}</summary>", ""]
        for usage, aliases, perm, dflt, text in s["commands"]:
            alias = f" ({s['alias_word']}: {', '.join('`/' + a + '`' for a in aliases)})" if aliases else ""
            out.append(f"- `{usage}`{alias} – {text} · `{perm}` ({s['default_word']}: {dflt})")
        out += ["", f"**{s['perm_title']}**", ""]
        for perm, dflt, text in s["perms"]:
            suffix = "" if dflt == "–" else f" ({s['default_word']}: {dflt})"
            out.append(f"- `{perm}` – {text}{suffix}")
        out += ["", "</details>", "", f"<details><summary>{s['cfg_title']}</summary>", "", "```yaml", config(name), "```", "", "</details>", ""]
    out += ["---", "", f"**{RATING}**"]
    return "\n".join(out) + "\n"


def release_file(name, version, description, kind, mc_min, mc_max):
    return f"""Titel:
{title(name, mc_min, mc_max)}

Versionstring:
{version}

Beschreibung ({kind}):
{description}"""

# UltraTeleports – Release-Material

Alles, was zum Hochladen auf den vier Seiten gebraucht wird.

**Datei zum Hochladen:** [`UltraTeleports-1.0.0.jar`](UltraTeleports-1.0.0.jar) (liegt in diesem Ordner).
Neu bauen mit `tools/package.sh` im Hauptordner.

## Version

| Feld | Wert |
| --- | --- |
| Versionsnummer | `1.0.0` |
| Version String (SpigotMC, BuiltByBit) | `1.0.0` |
| Versionsname (Modrinth, Hangar) | `UltraTeleports 1.0.0` |
| Kanal / Release-Typ | Release |
| Changelog | [`changelog.md`](changelog.md) |

Versionsnummer und Version String stehen auch in `version.txt` zum Kopieren.

## Projekt

| Feld | Wert |
| --- | --- |
| Titel SpigotMC / BuiltByBit | UltraTeleports \| TPA, RTP & Back |
| Name Modrinth / Hangar | UltraTeleports |
| Tagline / Summary (alle vier) | TPA requests, safe random teleport and /back with warm-ups. Action-bar messages in 5 languages. |
| Minecraft-Versionen | 1.20.4 – 1.21.4 (gebaut gegen die API 1.20.4, im Test gelaufen auf 1.21.4) |
| Java | 17+ |
| Abhängigkeiten | keine (Vault optional) |
| Quellcode | https://github.com/creativedm0031/UltraPlugins |

## Bilder (`images/`)

| Datei | Größe | Wofür |
| --- | --- | --- |
| `icon-512.png` | 512 × 512 | Icon auf SpigotMC, BuiltByBit, Hangar |
| `icon-256.png` | 256 × 256, < 256 KiB | Icon auf Modrinth |
| `banner.png` | 1920 × 480 | Oben in jeder Beschreibung |
| `cover.png` | 1280 × 720 | Cover/Vorschaubild auf BuiltByBit, erstes Galeriebild auf Modrinth |
| `gallery-*.png` | 1600 × 900 | Beispielbilder (Galerie bzw. in der Beschreibung) |

## SpigotMC

1. Resource-Typ: kostenlos, Datei hochladen: `UltraTeleports-1.0.0.jar`, Version String: `1.0.0`.
2. Title: `spigotmc/title.txt`, Tag Line: `spigotmc/tagline.txt`
3. Kategorie: **Spigot → Tools and Utilities**
4. Getestete Versionen: 1.20, 1.21
5. Icon: `images/icon-512.png`
6. Beschreibung: Inhalt von `spigotmc/description.bbcode` einfügen (BBCode-Modus).
7. Source-Code-Link: GitHub-Repo.

## BuiltByBit

1. Neue Ressource, Kategorie **Plugins → Spigot** (Unterkategorie passend zu *Tools and Utilities*), Preis kostenlos.
2. Title: `builtbybit/title.txt`, Tag Line: `builtbybit/tagline.txt`
3. Icon: `images/icon-512.png`, Cover/Banner: `images/cover.png`
4. Beschreibung: Inhalt von `builtbybit/description.bbcode` einfügen.
5. Unterstützte Versionen: 1.20 – 1.21, Plattformen Spigot und Paper.
6. Datei: `UltraTeleports-1.0.0.jar`, Version String: `1.0.0`.

## Modrinth

1. Neues Projekt, Typ **Plugin**. Name: `modrinth/title.txt`, Summary: `modrinth/tagline.txt`
2. Icon: `images/icon-256.png`
3. Kategorien: **utility, transportation**; Umgebung: nur Server.
4. Beschreibung: Inhalt von `modrinth/description.md`.
5. Galerie (in dieser Reihenfolge, das erste als „Featured“):
   - `images/cover.png`
   - `images/gallery-1-layout.png`
   - `images/gallery-2-languages.png`
   - `images/gallery-3-commands.png`
6. Version hochladen: Datei `UltraTeleports-1.0.0.jar`, Versionsnummer `1.0.0`, Name `UltraTeleports 1.0.0`, Typ Release, Changelog aus `changelog.md`. Loader **Bukkit, Spigot, Paper, Purpur**, Spielversionen 1.20.4 – 1.21.4.
7. Lizenz und Source-Link (GitHub) eintragen.

## Hangar

1. Neues Projekt, Plattform **Paper**, Kategorie **Gameplay**. Name: `hangar/title.txt`, Summary: `hangar/tagline.txt`
2. Icon (Avatar): `images/icon-512.png`
3. Beschreibung (Hauptseite): Inhalt von `hangar/description.md`.
4. Version: Datei `UltraTeleports-1.0.0.jar`, Version `1.0.0`, Changelog aus `changelog.md`, Plattformversionen 1.20.4 – 1.21.4, Kanal **Release**.
5. Source-Link (GitHub) eintragen.

> Hangar listet nur Paper-Plugins – UltraPlugins laufen auf Paper, deshalb passt es. Spigot wird dort nicht angegeben.

## Bilder in den Beschreibungen

Alle Beschreibungen laden ihre Bilder von `raw.githubusercontent.com/creativedm0031/UltraPlugins/main/…`.
Sie erscheinen erst, wenn das Repository öffentlich ist und dieser Stand auf `main` liegt. Alternativ
die PNGs bei einem Bildhoster hochladen und die Links ersetzen.

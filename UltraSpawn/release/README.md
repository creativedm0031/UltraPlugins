# UltraSpawn – Release-Material

Alles, was zum Hochladen auf den vier Seiten gebraucht wird.

**Datei zum Hochladen:** [`UltraSpawn-1.0.0.jar`](UltraSpawn-1.0.0.jar) (liegt in diesem Ordner).
Neu bauen mit `tools/package.sh` im Hauptordner.

## Version

| Feld | Wert |
| --- | --- |
| Versionsnummer | `1.0.0` |
| Version String (SpigotMC, BuiltByBit) | `1.0.0` |
| Versionsname (Modrinth, Hangar) | `UltraSpawn 1.0.0` |
| Kanal / Release-Typ | Release |
| Changelog | [`changelog.md`](changelog.md) |

Versionsnummer, Version String und Minecraft-Versionen stehen auch in `version.txt` zum Kopieren.

## Unterstützte Minecraft-Versionen

**1.20.4 – 26.3** auf Spigot, Paper und Purpur, Java 17+ (Minecraft 1.20.5+ braucht ohnehin Java 21,
26.x Java 25).

Zum Ankreuzen in den Versionsfeldern:

`1.20.4`, `1.20.5`, `1.20.6`, `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`, `26.3`

Getestet (alle zehn Plugins auf Paper gestartet):

| Minecraft | Test |
| --- | --- |
| 1.20.4 | bot test |
| 1.20.6 | bot test |
| 1.21.1 | bot test |
| 1.21.4 | full bot test |
| 1.21.8 | bot test |
| 1.21.11 | bot test |
| 26.1.2 | bot test |
| 26.3 | start + console |

Auf einem echten Spigot-Server wurde noch nicht getestet; gebaut wird gegen die Spigot-API 1.20.4.

## Projekt

| Feld | Wert |
| --- | --- |
| Titel SpigotMC / BuiltByBit | UltraSpawn \| Multi-Spawn & Void Rescue |
| Name Modrinth / Hangar | UltraSpawn |
| Tagline / Summary (alle vier) | Multiple spawns, first-join spawn and void rescue. Clean action-bar messages in 5 languages. |
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

1. Resource-Typ: kostenlos, Datei hochladen: `UltraSpawn-1.0.0.jar`, Version String: `1.0.0`.
2. Title: `spigotmc/title.txt`, Tag Line: `spigotmc/tagline.txt`
3. Kategorie: **Spigot → Tools and Utilities**
4. Unterstützte Versionen ankreuzen: **1.20, 1.21** und **26.x**, falls SpigotMC sie anbietet
5. Icon: `images/icon-512.png`
6. Beschreibung: Inhalt von `spigotmc/description.bbcode` einfügen (BBCode-Modus).
7. Source-Code-Link: GitHub-Repo.

## BuiltByBit

1. Neue Ressource, Kategorie **Plugins → Spigot** (Unterkategorie passend zu *Tools and Utilities*), Preis kostenlos.
2. Title: `builtbybit/title.txt`, Tag Line: `builtbybit/tagline.txt`
3. Icon: `images/icon-512.png`, Cover/Banner: `images/cover.png`
4. Beschreibung: Inhalt von `builtbybit/description.bbcode` einfügen.
5. Unterstützte Versionen: **1.20 – 1.21 und 26.x** (1.20.4 – 26.3), Plattformen Spigot und Paper.
6. Datei: `UltraSpawn-1.0.0.jar`, Version String: `1.0.0`.

## Modrinth

1. Neues Projekt, Typ **Plugin**. Name: `modrinth/title.txt`, Summary: `modrinth/tagline.txt`
2. Icon: `images/icon-256.png`
3. Kategorien: **utility, management**; Umgebung: nur Server.
4. Beschreibung: Inhalt von `modrinth/description.md`.
5. Galerie (in dieser Reihenfolge, das erste als „Featured“):
   - `images/cover.png`
   - `images/gallery-1-layout.png`
   - `images/gallery-2-languages.png`
   - `images/gallery-3-commands.png`
6. Version hochladen: Datei `UltraSpawn-1.0.0.jar`, Versionsnummer `1.0.0`, Name `UltraSpawn 1.0.0`, Typ Release, Changelog aus `changelog.md`. Loader **Bukkit, Spigot, Paper, Purpur**, Spielversionen: alle aus der Liste oben (1.20.4 – 26.3).
7. Lizenz und Source-Link (GitHub) eintragen.

## Hangar

1. Neues Projekt, Plattform **Paper**, Kategorie **Admin Tools**. Name: `hangar/title.txt`, Summary: `hangar/tagline.txt`
2. Icon (Avatar): `images/icon-512.png`
3. Beschreibung (Hauptseite): Inhalt von `hangar/description.md`.
4. Version: Datei `UltraSpawn-1.0.0.jar`, Version `1.0.0`, Changelog aus `changelog.md`, Plattform **Paper**, Versionen: alle aus der Liste oben (1.20.4 – 26.3), Kanal **Release**.
5. Source-Link (GitHub) eintragen.

> Hangar listet nur Paper-Plugins – UltraPlugins laufen auf Paper, deshalb passt es. Spigot wird dort nicht angegeben.

## Bilder in den Beschreibungen

Alle Beschreibungen laden ihre Bilder von `raw.githubusercontent.com/creativedm0031/UltraPlugins/main/…`.
Sie erscheinen erst, wenn das Repository öffentlich ist und dieser Stand auf `main` liegt. Alternativ
die PNGs bei einem Bildhoster hochladen und die Links ersetzen.

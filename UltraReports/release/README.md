# UltraReports – Release

| Datei | Inhalt |
| --- | --- |
| [`release.txt`](release.txt) | **Titel, Versionstring, Beschreibung** (BBCode) – für SpigotMC und BuiltByBit |
| [`release-markdown.txt`](release-markdown.txt) | dasselbe mit Beschreibung in Markdown – für Modrinth und Hangar |
| [`UltraReports-1.0.0.jar`](UltraReports-1.0.0.jar) | die Plugin-Datei zum Hochladen |
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
- `images/cover.png`
- `images/gallery-1-layout.png`
- `images/gallery-2-languages.png`
- `images/gallery-3-gui.png`
- `images/gallery-4-commands.png`

## Felder auf den Seiten

| Feld | SpigotMC | BuiltByBit | Modrinth | Hangar |
| --- | --- | --- | --- | --- |
| Kategorie | Tools and Utilities | Plugins → Spigot | management, social | Admin Tools |
| Loader / Plattform | Spigot | Spigot, Paper | Bukkit, Spigot, Paper, Purpur | Paper |

Minecraft-Versionen zum Ankreuzen (1.20.4 – 26.3): `1.20.4`, `1.20.5`, `1.20.6`, `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`, `26.3`

Hangar macht aus dem Projektnamen die URL. Falls das Namensfeld dort keine Sonderzeichen oder
Klammern annimmt, `UltraReports` eintragen – der vollständige Titel steht ohnehin oben in der Beschreibung.

## Getestet

Alle zehn Plugins auf Paper gestartet:

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

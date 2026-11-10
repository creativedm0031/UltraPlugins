<p align="center"><img src="shared/assets/series-banner.png" alt="UltraPlugins"></p>

# UltraPlugins

Eine Serie von Minecraft-Plugins, die sich im Spiel alle gleich anfühlen: gleiche Farben, gleiches
Nachrichten-Layout, gleiche Befehlsstruktur, gleiche Logos. Jedes Plugin hat einen eigenen Ordner.

| | Plugin | Kurzbeschreibung |
| --- | --- | --- |
| <img src="UltraSpawn/assets/logo.png" width="48"> | [**UltraSpawn**](UltraSpawn) | Spawn-Verwaltung, wie sie sein sollte. |
| <img src="UltraTeleports/assets/logo.png" width="48"> | [**UltraTeleports**](UltraTeleports) | Anfragen, Random-Teleport & Back in einem Plugin. |
| <img src="UltraPunish/assets/logo.png" width="48"> | [**UltraPunish**](UltraPunish) | Saubere Moderation mit GUI, Vorlagen & Historie. |
| <img src="UltraHomes/assets/logo.png" width="48"> | [**UltraHomes**](UltraHomes) | Homes setzen, verwalten und teilen – in einer GUI. |
| <img src="UltraWarps/assets/logo.png" width="48"> | [**UltraWarps**](UltraWarps) | Server- und Spieler-Warps mit Kategorien. |
| <img src="UltraChat/assets/logo.png" width="48"> | [**UltraChat**](UltraChat) | Formatierung, Kanäle & Moderation für deinen Chat. |
| <img src="UltraEconomy/assets/logo.png" width="48"> | [**UltraEconomy**](UltraEconomy) | Schnelle, Vault-fähige Economy mit mehreren Währungen. |
| <img src="UltraKits/assets/logo.png" width="48"> | [**UltraKits**](UltraKits) | Kits mit Vorschau, Cooldowns & Ingame-Editor. |
| <img src="UltraVanish/assets/logo.png" width="48"> | [**UltraVanish**](UltraVanish) | Echte Unsichtbarkeit für dein Team. |
| <img src="UltraReports/assets/logo.png" width="48"> | [**UltraReports**](UltraReports) | Spieler-Reports mit Team-Workflow in der GUI. |

## Aufbau

```
Ultra<Name>/
  README.md                  Konzept: Features, Befehle, Permissions, Nachrichten
  assets/                    Logo, Banner, Ingame-Vorschau
  spigot/description.bbcode  SpigotMC-Beschreibung
shared/assets/               Serien-Banner und Abschnitts-Header für SpigotMC
tools/                       Generator für Assets und Texte
DESIGN.md                    Gemeinsames Design-System
```

Die Regeln, die für alle Plugins gelten, stehen in [DESIGN.md](DESIGN.md).

## Assets neu erzeugen

Inhalte stehen in `tools/plugins.py`. Nach einer Änderung:

```bash
python3 tools/generate.py && node tools/render.js
```

`render.js` braucht Node mit Playwright und die Schrift Inter. Die erzeugten Dateien werden
überschrieben – Änderungen also in `tools/plugins.py` machen, nicht in den Ausgaben.

## SpigotMC

Die Bilder in den Beschreibungen verweisen auf `raw.githubusercontent.com/.../main/...`. Sie werden
erst angezeigt, wenn dieses Repository öffentlich ist und die Dateien auf `main` liegen. Sonst die
PNGs bei einem Bildhoster hochladen und die Links in den `.bbcode`-Dateien ersetzen.

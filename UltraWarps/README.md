<p align="center"><img src="assets/banner.png" alt="UltraWarps"></p>

# UltraWarps

> Server- und Spieler-Warps mit Kategorien.

UltraWarps bringt Server-Warps in eine GUI mit Kategorien und Icons. Optional dürfen Spieler eigene Warps erstellen, zum Beispiel für ihren Shop.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- Warp-GUI mit Kategorien, Icons und Suche
- Optionale Spieler-Warps mit Kosten und Besucherzähler
- Permission pro Warp möglich
- Sortierung nach Name, Beliebtheit oder Datum
- Warmup, Cooldown und Teleport-Kosten
- Import aus EssentialsX

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/warp [name]` | Teleportiert zu einem Warp | `ultrawarps.warp` |
| `/warps` | Öffnet die Warp-GUI | `ultrawarps.warp` |
| `/setwarp <name>` | Erstellt einen Server-Warp | `ultrawarps.set` |
| `/delwarp <name>` | Löscht einen Warp | `ultrawarps.delete` |
| `/pwarp <create|delete|list>` | Spieler-Warps | `ultrawarps.player` |
| `/ultrawarps <help|reload|version|import>` | Verwaltung | `ultrawarps.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultrawarps.warp.<name>` | Zugriff auf einen bestimmten Warp |
| `ultrawarps.player.limit.<n>` | Maximale Anzahl Spieler-Warps |
| `ultrawarps.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus der `messages.yml`
(Standard: Englisch, `lang/de.yml` liegt bei):

```yaml
actionbar:
  warp-teleport: "Warped to <accent>shop</accent>."
  warp-created: "Warp <accent>arena</accent> created."
error:
  warp-unknown: "Warp arena doesn't exist."
```

Das Präfix (`✦ Warps »` bzw. `✖ UltraWarps »`) setzt das Plugin automatisch davor.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraTeleports](../UltraTeleports) · [UltraPunish](../UltraPunish) · [UltraHomes](../UltraHomes) · [UltraChat](../UltraChat) · [UltraEconomy](../UltraEconomy) · [UltraKits](../UltraKits) · [UltraVanish](../UltraVanish) · [UltraReports](../UltraReports)

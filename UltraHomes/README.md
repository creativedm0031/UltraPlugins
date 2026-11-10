<p align="center"><img src="assets/banner.png" alt="UltraHomes"></p>

# UltraHomes

> Homes setzen, verwalten und teilen – in einer GUI.

UltraHomes gibt Spielern ihre Homes in einer aufgeräumten GUI: eigene Icons, Limits per Permission und Homes, die sich mit Freunden teilen lassen.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- Homes-GUI mit frei wählbaren Icons
- Limits per Permission (ultrahomes.limit.<anzahl>)
- Umbenennen, verschieben und löschen mit Bestätigung
- Homes mit Freunden teilen oder öffentlich machen
- Warmup, Cooldown und Welt-Blacklist
- Import aus EssentialsX

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/home [name]` | Teleportiert zu einem Home | `ultrahomes.home` |
| `/homes` | Öffnet die Homes-GUI | `ultrahomes.home` |
| `/sethome [name]` | Setzt ein Home | `ultrahomes.set` |
| `/delhome <name>` | Löscht ein Home | `ultrahomes.delete` |
| `/ultrahomes <help|reload|version|import>` | Verwaltung | `ultrahomes.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultrahomes.limit.<n>` | Maximale Anzahl Homes |
| `ultrahomes.others` | Homes anderer Spieler verwalten |
| `ultrahomes.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus der `messages.yml`
(Standard: Englisch, `lang/de.yml` liegt bei):

```yaml
actionbar:
  home-set: "Home <accent>base</accent> saved."
  home-teleport: "Welcome home."
error:
  limit-reached: "Home limit reached (3/3)."
```

Das Präfix (`✦ Homes »` bzw. `✖ UltraHomes »`) setzt das Plugin automatisch davor.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraTeleports](../UltraTeleports) · [UltraPunish](../UltraPunish) · [UltraWarps](../UltraWarps) · [UltraChat](../UltraChat) · [UltraEconomy](../UltraEconomy) · [UltraKits](../UltraKits) · [UltraVanish](../UltraVanish) · [UltraReports](../UltraReports)

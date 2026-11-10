<p align="center"><img src="assets/banner.png" alt="UltraKits"></p>

# UltraKits

> Kits mit Vorschau, Cooldowns & Ingame-Editor.

UltraKits macht Kits einfach: im Spiel aus dem eigenen Inventar erstellen, in einer GUI anbieten und per Cooldown, Preis oder Permission begrenzen.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- Kit-GUI mit Vorschau jedes Kits
- Ingame-Editor: Kit direkt aus dem Inventar erstellen
- Cooldowns, Einmal-Kits, Kosten und Permissions
- First-Join-Kit für neue Spieler
- Rüstung wird automatisch angelegt
- Befehle als Kit-Inhalt möglich

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/kit [name] [spieler]` | Kit abholen (an andere: ultrakits.give) | `ultrakits.kit` |
| `/kits` | Öffnet die Kit-GUI | `ultrakits.kit` |
| `/createkit <name>` | Kit aus dem Inventar erstellen | `ultrakits.create` |
| `/editkit <name> <items|cooldown|cost|onetime|firstjoin|icon>` | Kit bearbeiten | `ultrakits.edit` |
| `/delkit <name>` | Kit löschen | `ultrakits.delete` |
| `/ultrakits <help|reload|version|language>` | Verwaltung | `ultrakits.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultrakits.kit.<name>` | Zugriff auf ein bestimmtes Kit |
| `ultrakits.bypass.cooldown` | Kein Cooldown |
| `ultrakits.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus `lang/en.yml`:

```yaml
kit-received: "Kit <accent>starter</accent> received."
kit-created: "Kit <accent>miner</accent> created with <accent>9</accent> items."
kit-cooldown: "Kit starter is on cooldown – 23h 59m left."
```

Das Präfix (`✦ Kits »` bzw. `✖ UltraKits »`) setzt das Plugin automatisch davor.

## Sprachen

Englisch, Deutsch, Französisch, Niederländisch und Italienisch sind eingebaut
(`plugins/UltraKits/lang/en.yml`, `de.yml`, `fr.yml`, `nl.yml`, `it.yml`).

- Jeder Spieler bekommt das Plugin in der Sprache seines Minecraft-Clients
  (`use-client-language: true`). Andere Client-Sprachen erhalten die Standardsprache.
- Standardsprache ändern: `language: de` in der `config.yml` oder im Spiel `/ultrakits language de`.
- Texte lassen sich in den Sprachdateien frei anpassen (MiniMessage). Neue Schlüssel aus Updates
  werden automatisch ergänzt, eigene Änderungen bleiben erhalten.

## Installation

1. `UltraKits-1.0.0.jar` in den Ordner `plugins` legen (Paper 1.20.4 oder neuer, Java 17+).
2. Server neu starten.
3. `config.yml` und die Sprachdateien anpassen, dann `/ultrakits reload`.

Bauen aus dem Quellcode: im Hauptordner `mvn package`, die Jar liegt danach in `UltraKits/target/`.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |
| `src/` | Quellcode, Konfiguration und Sprachdateien |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraTeleports](../UltraTeleports) · [UltraPunish](../UltraPunish) · [UltraHomes](../UltraHomes) · [UltraWarps](../UltraWarps) · [UltraChat](../UltraChat) · [UltraEconomy](../UltraEconomy) · [UltraVanish](../UltraVanish) · [UltraReports](../UltraReports)

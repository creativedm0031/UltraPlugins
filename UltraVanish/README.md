<p align="center"><img src="assets/banner.png" alt="UltraVanish"></p>

# UltraVanish

> Echte Unsichtbarkeit für dein Team.

UltraVanish versteckt Teammitglieder vollständig: aus der Tabliste, der Spielerzahl und der Welt. Solange man unsichtbar ist, zeigt die Actionbar dauerhaft den Status an.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- Vollständig unsichtbar: Tabliste, Spielerzahl, Join- und Quit-Nachrichten
- Kisten lautlos und ohne Animation öffnen
- Kein Mob-Targeting, kein Item-Aufheben, keine Druckplatten
- Dauerhafte Status-Anzeige in der Actionbar
- Vanish-Level: höhere Ränge sehen niedrigere
- Fake-Join und Fake-Quit

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/vanish [spieler]` | Vanish an/aus (andere: ultravanish.vanish.others) | `ultravanish.vanish` |
| `/vanishlist` | Zeigt unsichtbare Spieler | `ultravanish.list` |
| `/fakejoin · /fakequit` | Sendet eine Fake-Nachricht | `ultravanish.fake` |
| `/ultravanish <help|reload|version>` | Verwaltung | `ultravanish.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultravanish.level.<n>` | Vanish-Level |
| `ultravanish.see` | Sieht unsichtbare Spieler |
| `ultravanish.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus der `messages.yml`
(Standard: Englisch, `lang/de.yml` liegt bei):

```yaml
actionbar:
  vanish-status: "VANISHED · invisible to <accent>12</accent> players"
  vanish-off: "You are visible again."
error:
  target-higher-level: "You can't vanish Steve – higher vanish level."
```

Das Präfix (`✦ Vanish »` bzw. `✖ UltraVanish »`) setzt das Plugin automatisch davor.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraTeleports](../UltraTeleports) · [UltraPunish](../UltraPunish) · [UltraHomes](../UltraHomes) · [UltraWarps](../UltraWarps) · [UltraChat](../UltraChat) · [UltraEconomy](../UltraEconomy) · [UltraKits](../UltraKits) · [UltraReports](../UltraReports)

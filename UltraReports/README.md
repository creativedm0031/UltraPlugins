<p align="center"><img src="assets/banner.png" alt="UltraReports"></p>

# UltraReports

> Spieler-Reports mit Team-Workflow in der GUI.

UltraReports gibt Spielern eine einfache Möglichkeit, Regelverstöße zu melden, und dem Team einen Posteingang, in dem jeder Report übernommen, bearbeitet und abgeschlossen wird.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- Report-GUI mit konfigurierbaren Gründen
- Team-Posteingang mit Status: offen, in Bearbeitung, erledigt
- Report übernehmen und direkt zum Spieler teleportieren
- Cooldown und Schutz vor Missbrauch
- Discord-Webhook und netzwerkweite Synchronisation
- Mit UltraPunish direkt aus dem Report bestrafen

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/report <spieler> [grund]` | Spieler melden | `ultrareports.report` |
| `/reports` | Öffnet den Team-Posteingang | `ultrareports.staff` |
| `/ultrareports <help|reload|version>` | Verwaltung | `ultrareports.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultrareports.notify` | Erhält Benachrichtigungen über neue Reports |
| `ultrareports.bypass.cooldown` | Kein Cooldown |
| `ultrareports.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus der `messages.yml`
(Standard: Englisch, `lang/de.yml` liegt bei):

```yaml
actionbar:
  report-sent: "Thanks! Your report on <accent>Steve</accent> was submitted."
  report-claimed: "You claimed report <accent>#42</accent>."
error:
  already-reported: "You already reported Steve recently."
```

Das Präfix (`✦ Reports »` bzw. `✖ UltraReports »`) setzt das Plugin automatisch davor.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraTeleports](../UltraTeleports) · [UltraPunish](../UltraPunish) · [UltraHomes](../UltraHomes) · [UltraWarps](../UltraWarps) · [UltraChat](../UltraChat) · [UltraEconomy](../UltraEconomy) · [UltraKits](../UltraKits) · [UltraVanish](../UltraVanish)

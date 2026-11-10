<p align="center"><img src="assets/banner.png" alt="UltraTeleports"></p>

# UltraTeleports

> Anfragen, Random-Teleport & Back in einem Plugin.

UltraTeleports bündelt alle Spieler-Teleports: TPA-Anfragen, sicherer Random-Teleport, /back und die klassischen Team-Befehle.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- TPA und TPAHere mit Ablaufzeit und klickbarer Annahme
- /back zum letzten Ort oder Todespunkt
- Random-Teleport mit asynchroner, sicherer Positionssuche pro Welt
- Anfragen ausschalten (/tptoggle) und einzelne Spieler ignorieren
- Warmup, Cooldown und optionale Kosten (Vault / UltraEconomy)
- Team-Befehle: /tp, /tphere, /tpall

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/tpa <spieler>` | Teleport-Anfrage senden | `ultrateleports.tpa` |
| `/tpahere <spieler>` | Spieler zu sich anfragen | `ultrateleports.tpahere` |
| `/tpaccept · /tpdeny` | Anfrage annehmen / ablehnen | `ultrateleports.tpa` |
| `/back` | Zurück zum letzten Ort | `ultrateleports.back` |
| `/rtp [welt]` | Zufälliger Teleport | `ultrateleports.rtp` |
| `/tptoggle` | Anfragen an/aus | `ultrateleports.toggle` |
| `/tp · /tphere · /tpall` | Team-Teleports | `ultrateleports.staff` |
| `/ultrateleports <help|reload|version>` | Verwaltung | `ultrateleports.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultrateleports.bypass.warmup` | Kein Warmup |
| `ultrateleports.bypass.cooldown` | Kein Cooldown |
| `ultrateleports.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus der `messages.yml`
(Standard: Englisch, `lang/de.yml` liegt bei):

```yaml
actionbar:
  request-sent: "Request sent to <accent>Steve</accent>."
  rtp-searching: "Searching a safe location…"
error:
  requests-disabled: "Steve has teleport requests disabled."
```

Das Präfix (`✦ Teleports »` bzw. `✖ UltraTeleports »`) setzt das Plugin automatisch davor.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraPunish](../UltraPunish) · [UltraHomes](../UltraHomes) · [UltraWarps](../UltraWarps) · [UltraChat](../UltraChat) · [UltraEconomy](../UltraEconomy) · [UltraKits](../UltraKits) · [UltraVanish](../UltraVanish) · [UltraReports](../UltraReports)

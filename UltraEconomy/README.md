<p align="center"><img src="assets/banner.png" alt="UltraEconomy"></p>

# UltraEconomy

> Schnelle, Vault-fähige Economy mit mehreren Währungen.

UltraEconomy ist die Wirtschafts-Basis der Serie: Vault-Anbindung, mehrere Währungen, eine Baltop-GUI und ein lückenloses Transaktions-Log.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- Vault-Provider, kompatibel mit jedem Vault-Plugin
- Mehrere Währungen (z. B. Coins und Gems)
- /pay mit Bestätigung ab einstellbarem Betrag
- Baltop-GUI mit Spielerköpfen
- Transaktions-Log für jede Buchung
- SQLite oder MySQL, netzwerkweit synchron

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/balance [spieler]` | Kontostand anzeigen | `ultraeconomy.balance` |
| `/pay <spieler> <betrag>` | Geld überweisen | `ultraeconomy.pay` |
| `/baltop` | Öffnet die Rangliste | `ultraeconomy.baltop` |
| `/eco <give|take|set> <spieler> <betrag>` | Kontostände ändern | `ultraeconomy.eco` |
| `/ultraeconomy <help|reload|version>` | Verwaltung | `ultraeconomy.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultraeconomy.balance.others` | Kontostand anderer sehen |
| `ultraeconomy.log` | Transaktions-Log einsehen |
| `ultraeconomy.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus der `messages.yml`
(Standard: Englisch, `lang/de.yml` liegt bei):

```yaml
actionbar:
  pay-sent: "You sent <accent>$250</accent> to <accent>Steve</accent>."
  pay-received: "<accent>+ $250</accent> from <accent>Alex</accent>"
error:
  insufficient-funds: "Insufficient funds – you're missing $120."
```

Das Präfix (`✦ Economy »` bzw. `✖ UltraEconomy »`) setzt das Plugin automatisch davor.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraTeleports](../UltraTeleports) · [UltraPunish](../UltraPunish) · [UltraHomes](../UltraHomes) · [UltraWarps](../UltraWarps) · [UltraChat](../UltraChat) · [UltraKits](../UltraKits) · [UltraVanish](../UltraVanish) · [UltraReports](../UltraReports)

<p align="center"><img src="assets/banner.png" alt="UltraPunish"></p>

# UltraPunish

> Saubere Moderation mit GUI, Vorlagen & Historie.

UltraPunish ist das Moderations-Plugin der Serie: Bans, Mutes, Kicks und Verwarnungen über Befehle oder eine übersichtliche GUI mit Grund-Vorlagen und automatisch steigenden Strafen.

Teil der [Ultra-Serie](../README.md). Layout, Farben und Verhalten folgen dem gemeinsamen
[Design-System](../DESIGN.md).

## Features

- Ban, Tempban, Mute, Tempmute, Kick und Warn
- Punish-GUI mit Grund-Vorlagen und eskalierenden Dauern
- Vollständige Historie pro Spieler
- IP-Bans und Alt-Account-Erkennung
- SQLite oder MySQL, netzwerkweit synchron
- Team-Benachrichtigungen und automatische Aktionen ab X Verwarnungen

## Befehle

| Befehl | Beschreibung | Permission |
| --- | --- | --- |
| `/punish <spieler>` | Öffnet die Punish-GUI | `ultrapunish.gui` |
| `/ban · /tempban <spieler> [dauer] [grund]` | Spieler bannen | `ultrapunish.ban` |
| `/mute · /tempmute <spieler> [dauer] [grund]` | Spieler muten | `ultrapunish.mute` |
| `/kick <spieler> [grund]` | Spieler kicken | `ultrapunish.kick` |
| `/warn <spieler> [grund]` | Spieler verwarnen | `ultrapunish.warn` |
| `/unban · /unmute <spieler>` | Strafe aufheben | `ultrapunish.revoke` |
| `/history <spieler>` | Historie anzeigen | `ultrapunish.history` |
| `/ultrapunish <help|reload|version>` | Verwaltung | `ultrapunish.admin` |

## Weitere Permissions

| Permission | Beschreibung |
| --- | --- |
| `ultrapunish.notify` | Erhält Team-Benachrichtigungen |
| `ultrapunish.exempt` | Kann nicht bestraft werden |
| `ultrapunish.*` | Alle Rechte des Plugins |

## Ingame-Layout

![Ingame-Vorschau](assets/preview.png)

Rückmeldungen erscheinen in der Actionbar, Fehler im Chat. Auszug aus der `messages.yml`
(Standard: Englisch, `lang/de.yml` liegt bei):

```yaml
actionbar:
  mute-applied: "<accent>Steve</accent> was muted for <accent>1h</accent> · Spam"
  ban-revoked: "<accent>Steve</accent> was unbanned."
error:
  target-exempt: "You can't punish Steve – higher rank."
```

Das Präfix (`✦ Punish »` bzw. `✖ UltraPunish »`) setzt das Plugin automatisch davor.

## Dateien in diesem Ordner

| Datei | Inhalt |
| --- | --- |
| `assets/logo.svg` · `logo.png` | Logo (512 × 512, Resource-Icon auf SpigotMC) |
| `assets/banner.svg` · `banner.png` | Banner (1920 × 480) |
| `assets/preview.png` | Mock-up des Ingame-Layouts |
| `spigot/description.bbcode` | Fertige SpigotMC-Beschreibung |

## Serie

[UltraSpawn](../UltraSpawn) · [UltraTeleports](../UltraTeleports) · [UltraHomes](../UltraHomes) · [UltraWarps](../UltraWarps) · [UltraChat](../UltraChat) · [UltraEconomy](../UltraEconomy) · [UltraKits](../UltraKits) · [UltraVanish](../UltraVanish) · [UltraReports](../UltraReports)

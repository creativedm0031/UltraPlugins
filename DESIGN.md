# Ultra Design-System

Diese Regeln gelten für **jedes** Plugin der Serie. Ein Spieler, der ein Ultra-Plugin kennt, soll
sich in allen anderen sofort zurechtfinden.

## 1. Farben

| Rolle | Hex | MiniMessage | Verwendung |
| --- | --- | --- | --- |
| Primär | `#8B5CF6` | `<#8B5CF6>` | Start des Marken-Verlaufs |
| Akzent | `#22D3EE` | `<accent>` | Ende des Verlaufs, hervorgehobene Werte (Namen, Zahlen, Zeiten) |
| Text | `#FFFFFF` | `<white>` | Nachrichtentext |
| Gedämpft | `#94A3B8` | `<muted>` | Lore, Beschreibungen, Hinweise |
| Trenner | `#64748B` | `<sep>` | `»` und Rahmen |
| Fehler | `#F87171` / `#FCA5A5` | `<error>` | Präfix / Text von Fehlermeldungen |
| Hintergrund | `#0B1020` → `#161D3F` | – | Logos, Banner |

Marken-Verlauf: `<gradient:#8B5CF6:#22D3EE>`. Die Tags `<accent>`, `<muted>`, `<sep>` und `<error>`
sind eigene MiniMessage-Tags, die jedes Plugin registriert – so ändert sich das Farbschema an einer
Stelle (`config.yml → theme`).

## 2. Nachrichten

**Grundregel: Alles in die Actionbar, nur Fehler in den Chat.**

| Art | Kanal | Format |
| --- | --- | --- |
| Bestätigung, Info, Status | Actionbar | `<gradient:#8B5CF6:#22D3EE>✦ <Modul></gradient> <sep>»</sep> <white>Text` |
| Countdown (Warmup) | Actionbar, jede Sekunde aktualisiert | wie oben |
| Dauer-Status (z. B. Vanish) | Actionbar, dauerhaft | wie oben |
| Fehler | Chat | `<error>✖ <bold>Ultra<Modul></bold></error> <sep>»</sep> <#FCA5A5>Text` |

- `<Modul>` ist der Plugin-Name ohne „Ultra" (`Spawn`, `Homes`, …).
- Actionbar-Nachrichten bleiben 3 Sekunden stehen (`config.yml → actionbar.duration`).
- Jede Nachricht ist ein Satz und passt in eine Zeile (Richtwert: höchstens 60 Zeichen).
- Variable Werte stehen immer in `<accent>`.
- Fehler stehen im Chat, damit man sie nachlesen kann und sie nicht nach drei Sekunden weg sind.

**Bewusste Ausnahmen** – Inhalte, die nicht in eine Actionbar-Zeile passen:

- Listen und Hilfe werden als GUI gezeigt, nicht als Chat-Text. `/ultra<name> help` in der Konsole
  gibt Text aus.
- Klickbare Anfragen (z. B. TPA annehmen) gehen zusätzlich als eine Chat-Zeile raus, weil die
  Actionbar nicht klickbar ist.
- Spielerchat und Privatnachrichten (UltraChat) sind Chat-Inhalt, kein Plugin-Feedback.
- Kick- und Ban-Bildschirme (UltraPunish) nutzen das Farbschema, sind aber eigene Screens.

## 3. Sounds

| Ereignis | Sound | Pitch |
| --- | --- | --- |
| Erfolg | `ENTITY_EXPERIENCE_ORB_PICKUP` | 1.4 |
| Fehler | `BLOCK_NOTE_BLOCK_BASS` | 0.6 |
| Countdown-Tick | `BLOCK_NOTE_BLOCK_HAT` | 1.0 |
| Teleport | `ENTITY_ENDERMAN_TELEPORT` | 1.0 |
| GUI-Klick | `UI_BUTTON_CLICK` | 1.2 |

Alle Sounds lassen sich in der `config.yml` einzeln abschalten.

## 4. GUIs

- Titel: `Ultra » <Seite>` (z. B. `Ultra » Homes`).
- Rahmen aus `BLACK_STAINED_GLASS_PANE` ohne Namen, Inhalt in der Mitte.
- Unterste Reihe ist die Navigation: Slot 1 = Zurück (`ARROW`), Slot 5 = Schließen (`BARRIER`),
  Slot 9 = Nächste Seite (`ARROW`).
- Item-Name im Marken-Verlauf, Lore in `<muted>`, Aktionen als `<accent>▸</accent> Linksklick: …`.
- Löschen und andere endgültige Aktionen brauchen immer eine Bestätigungs-GUI
  (grün = bestätigen links, rot = abbrechen rechts).

## 5. Befehle und Permissions

- Jedes Plugin hat einen Verwaltungsbefehl `/ultra<name>` mit `help`, `reload` und `version`.
- Spielerbefehle sind kurz und üblich (`/spawn`, `/home`, `/warp`) und lassen sich einzeln
  deaktivieren oder umbenennen, falls ein anderes Plugin sie belegt.
- Permissions: `ultra<name>.<aktion>`, dazu `ultra<name>.admin`, `ultra<name>.*` und
  `ultra<name>.bypass.<cooldown|warmup>`.
- Tab-Vervollständigung für jedes Argument.

## 6. Dateien

```
plugins/Ultra<Name>/
  config.yml      Einstellungen, theme, actionbar, sounds
  messages.yml    Alle Texte (MiniMessage), getrennt nach actionbar / error / gui
  lang/           en.yml (Standard), de.yml
  data/           SQLite, falls kein MySQL eingestellt ist
```

Gleiche Schlüssel heißen überall gleich (`warmup`, `cooldown`, `storage`, `theme`).

## 7. Technik

- Paper-API 1.20.4 bis 1.21.x, Java 17, Folia-kompatibel; Spigot über mitgelieferte Adventure-Bibliothek.
- MiniMessage für alle Texte, PlaceholderAPI (`%ultra<name>_…%`), Vault wo sinnvoll.
- Gemeinsamer Code (Nachrichten-Versand, Theme, GUI-Baukasten, Warmup/Cooldown, Storage) liegt in
  einer Bibliothek **UltraCore**, die in jedes Plugin eingebunden (geshadet) wird. So bleibt jedes
  Plugin einzeln installierbar und das Layout kann nicht auseinanderlaufen.

## 8. Logos und Banner

- Logo: abgerundete Kachel im Hintergrund-Verlauf, Rand und Symbol im Marken-Verlauf. Kein Text,
  damit es als 96-px-Icon auf SpigotMC lesbar bleibt. Pro Plugin ändert sich nur das Symbol.
- Banner (1920 × 480): Logo links, `Ultra` weiß + Name im Verlauf, Slogan, drei Schlagworte.
- Schrift: Inter (ExtraBold für Titel).
- Erzeugt werden alle Grafiken aus `tools/plugins.py`, siehe [README](README.md).

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
- Jede Nachricht ist ein Satz und passt in eine Zeile (Richtwert: höchstens 60 Zeichen).
- Variable Werte stehen immer in `<accent>`.
- Fehler stehen im Chat, damit man sie nachlesen kann und sie nicht nach drei Sekunden weg sind.

**Bewusste Ausnahmen** – Inhalte, die nicht in eine Actionbar-Zeile passen:

- Listen werden als GUI gezeigt. Nur `/ultra<name> help` gibt eine kurze Liste im Chat aus, weil
  Hilfe nachlesbar sein muss.
- Klickbare Zeilen (TPA annehmen, große Überweisung bestätigen, neuer Report fürs Team) gehen als
  eine Chat-Zeile raus, weil die Actionbar nicht klickbar ist.
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

- Jedes Plugin hat einen Verwaltungsbefehl `/ultra<name>` mit `help`, `reload`, `version` und `language`.
- Spielerbefehle sind kurz und üblich (`/spawn`, `/home`, `/warp`). Belegt ein anderes Plugin
  denselben Befehl, ist der Ultra-Befehl immer als `/ultra<name>:<befehl>` erreichbar.
- Permissions: `ultra<name>.<aktion>`, dazu `ultra<name>.admin`, `ultra<name>.*` und
  `ultra<name>.bypass.<cooldown|warmup>`.
- Tab-Vervollständigung für jedes Argument.

## 6. Sprachen

Jedes Plugin enthält **Englisch (`en`), Deutsch (`de`), Französisch (`fr`), Niederländisch (`nl`)
und Italienisch (`it`)**.

- Jeder Empfänger bekommt eine Nachricht in seiner Sprache: Spieler in der Sprache ihres
  Minecraft-Clients, sonst die Standardsprache (`config.yml → language`). Das gilt auch für
  Team-Benachrichtigungen, GUIs und den Ban-Screen.
- `use-client-language: false` schaltet die Client-Erkennung ab, dann gilt für alle die Standardsprache.
- `/ultra<name> language <code>` ändert die Standardsprache im Spiel und speichert sie.
- Die Sprachdateien liegen in `plugins/Ultra<Name>/lang/`. Beim Start werden fehlende Schlüssel
  ergänzt, angepasste Texte bleiben erhalten.
- Gemeinsame Texte (keine Berechtigung, Spieler nicht online, GUI-Knöpfe …) kommen aus UltraCore
  und lauten in allen Plugins gleich.
- Quelle aller Übersetzungen: `tools/lang/*.txt`, daraus erzeugt `tools/gen_lang.py` die YAML-Dateien.

## 7. Dateien und Technik

```
plugins/Ultra<Name>/
  config.yml      language, use-client-language, theme, sounds, Plugin-Einstellungen
  lang/           en.yml, de.yml, fr.yml, nl.yml, it.yml
  *.yml           Daten (z. B. homes.yml, warps.yml, punishments.yml)
```

- Spigot, Paper und Purpur, Minecraft 1.20.4 bis 26.3 (getestet auf 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.8,
  1.21.11, 26.1.2 und 26.3), Java 17+. Gebaut wird gegen die Spigot-API; Paper-eigene
  Funktionen werden nicht benutzt.
- Texte in MiniMessage. Die Text-Bibliothek Adventure ist in jede Jar eingebettet und umbenannt
  (`de.creativedm.ultra.<name>.lib.kyori`), damit Farbverläufe, Hover-Texte und die Actionbar auch
  auf Spigot funktionieren und sich nicht mit anderen Plugins beißen.
- Itemnamen, Lore, Inventar-Titel und Kick-Screens verlangen auf Spigot Text statt Komponenten; sie
  werden aus MiniMessage in Farbcodes mit Hex-Farben umgewandelt (`Messages.legacy`). Vault für Kosten (Teleports, Warps, Kits) und als Schnittstelle von UltraEconomy.
- Daten liegen in YAML-Dateien, die asynchron geschrieben werden.
- UltraRanks setzt bei jedem Spieler die Metadaten `ultraranks_prefix`, `ultraranks_suffix` und
  `ultraranks_rank` (MiniMessage). UltraChat liest sie für die Platzhalter `<prefix>`, `<suffix>` und
  `<rank>`; ohne UltraRanks kommen Prefix und Suffix über Vault von jedem anderen Rang-Plugin.
- Gemeinsamer Code (Nachrichten, Sprachen, Theme, GUI-Baukasten, Warmup/Cooldown, Datendateien)
  liegt im Modul **UltraCore** und wird in jedes Plugin eingebettet und umbenannt (Shading). So ist
  jedes Plugin einzeln installierbar, und das Layout kann nicht auseinanderlaufen.

## 8. Logos und Banner

- Logo: abgerundete Kachel im Hintergrund-Verlauf, Rand und Symbol im Marken-Verlauf. Kein Text,
  damit es als 96-px-Icon auf SpigotMC lesbar bleibt. Pro Plugin ändert sich nur das Symbol.
- Banner (1920 × 480): Logo links, `Ultra` weiß + Name im Verlauf, Slogan, drei Schlagworte.
- Schrift: Inter (ExtraBold für Titel).
- Erzeugt werden alle Grafiken aus `tools/plugins.py`, siehe [README](README.md).

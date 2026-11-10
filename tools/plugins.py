"""Single source of truth for the Ultra plugin series.

Every text field that exists in two languages is a (German, English) tuple:
German goes into the concept README, English into the SpigotMC description.
"""

PLUGINS = [
    {
        "name": "Spawn",
        "tagline": ("Spawn-Verwaltung, wie sie sein sollte.", "Spawn management done right."),
        "chips": ["Multi-Spawn", "First Join", "Void Rescue"],
        "intro": (
            "UltraSpawn kümmert sich um alles rund um den Spawn: ein oder mehrere Spawnpunkte, "
            "ein eigener Spawn für neue Spieler und automatisches Teleportieren bei Join, Tod oder Sturz ins Void.",
            "UltraSpawn handles everything around your spawn: one or many spawn points, a dedicated "
            "first-join spawn and automatic teleports on join, respawn or when falling into the void.",
        ),
        "features": [
            ("Mehrere Spawns pro Welt oder Rang (z. B. VIP-Spawn)", "Multiple spawns per world or rank (e.g. a VIP spawn)"),
            ("Eigener First-Join-Spawn für neue Spieler", "Dedicated first-join spawn for new players"),
            ("Teleport bei Join, Respawn und Void einzeln schaltbar", "Teleport on join, respawn and void, each toggleable"),
            ("Warmup-Countdown in der Actionbar, Abbruch bei Bewegung oder Schaden", "Warm-up countdown in the action bar, cancelled on movement or damage"),
            ("Cooldowns und Bypass-Permissions", "Cooldowns and bypass permissions"),
            ("Void-Rescue: kein Tod mehr in der Lobby", "Void rescue: no more deaths in your lobby"),
        ],
        "commands": [
            ("/spawn [spieler]", "ultraspawn.spawn", ("Teleportiert zum Spawn (andere: ultraspawn.spawn.others)", "Teleport to spawn (others: ultraspawn.spawn.others)")),
            ("/setspawn [name]", "ultraspawn.set", ("Setzt einen Spawnpunkt", "Set a spawn point")),
            ("/delspawn <name>", "ultraspawn.delete", ("Löscht einen Spawnpunkt", "Delete a spawn point")),
            ("/ultraspawn <help|reload|version>", "ultraspawn.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultraspawn.bypass.warmup", ("Kein Warmup", "Skip the warm-up")), ("ultraspawn.bypass.cooldown", ("Kein Cooldown", "Skip the cooldown"))],
        "messages": [("teleport-warmup", "Teleporting in <c>3s</c> – don't move!"), ("teleport-success", "Welcome back at <c>spawn</c>.")],
        "error": ("teleport-cancelled", "You moved! Teleport cancelled."),
    },
    {
        "name": "Teleports",
        "tagline": ("Anfragen, Random-Teleport & Back in einem Plugin.", "Requests, random teleport & back in one plugin."),
        "chips": ["TPA", "RTP", "Back"],
        "intro": (
            "UltraTeleports bündelt alle Spieler-Teleports: TPA-Anfragen, sicherer Random-Teleport, /back "
            "und die klassischen Team-Befehle.",
            "UltraTeleports bundles every player teleport: TPA requests, safe random teleport, /back "
            "and the classic staff commands.",
        ),
        "features": [
            ("TPA und TPAHere mit Ablaufzeit und klickbarer Annahme", "TPA and TPAHere with expiry and click-to-accept"),
            ("/back zum letzten Ort oder Todespunkt", "/back to your last location or death point"),
            ("Random-Teleport mit asynchroner, sicherer Positionssuche pro Welt", "Random teleport with async, safe location search per world"),
            ("Anfragen ausschalten (/tptoggle) und einzelne Spieler ignorieren", "Disable requests (/tptoggle) and ignore single players"),
            ("Warmup, Cooldown und optionale Kosten (Vault / UltraEconomy)", "Warm-up, cooldown and optional costs (Vault / UltraEconomy)"),
            ("Team-Befehle: /tp, /tphere, /tpall", "Staff commands: /tp, /tphere, /tpall"),
        ],
        "commands": [
            ("/tpa <spieler>", "ultrateleports.tpa", ("Teleport-Anfrage senden", "Send a teleport request")),
            ("/tpahere <spieler>", "ultrateleports.tpahere", ("Spieler zu sich anfragen", "Request a player to come to you")),
            ("/tpaccept · /tpdeny", "ultrateleports.tpa", ("Anfrage annehmen / ablehnen", "Accept / deny a request")),
            ("/back", "ultrateleports.back", ("Zurück zum letzten Ort", "Return to your last location")),
            ("/rtp [welt]", "ultrateleports.rtp", ("Zufälliger Teleport", "Random teleport")),
            ("/tptoggle", "ultrateleports.toggle", ("Anfragen an/aus", "Toggle incoming requests")),
            ("/tp · /tphere · /tpall", "ultrateleports.staff", ("Team-Teleports", "Staff teleports")),
            ("/ultrateleports <help|reload|version>", "ultrateleports.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultrateleports.bypass.warmup", ("Kein Warmup", "Skip the warm-up")), ("ultrateleports.bypass.cooldown", ("Kein Cooldown", "Skip the cooldown"))],
        "messages": [("request-sent", "Request sent to <c>Steve</c>."), ("rtp-searching", "Searching a safe location…")],
        "error": ("requests-disabled", "Steve has teleport requests disabled."),
    },
    {
        "name": "Punish",
        "tagline": ("Saubere Moderation mit GUI, Vorlagen & Historie.", "Clean moderation with GUI, templates & history."),
        "chips": ["Ban · Mute · Warn", "Templates", "History"],
        "intro": (
            "UltraPunish ist das Moderations-Plugin der Serie: Bans, Mutes, Kicks und Verwarnungen über Befehle "
            "oder eine übersichtliche GUI mit Grund-Vorlagen und automatisch steigenden Strafen.",
            "UltraPunish is the moderation plugin of the series: bans, mutes, kicks and warnings via commands "
            "or a clean GUI with reason templates and automatically escalating punishments.",
        ),
        "features": [
            ("Ban, Tempban, Mute, Tempmute, Kick und Warn", "Ban, temp-ban, mute, temp-mute, kick and warn"),
            ("Punish-GUI mit Grund-Vorlagen und eskalierenden Dauern", "Punish GUI with reason templates and escalating durations"),
            ("Vollständige Historie pro Spieler", "Full history per player"),
            ("IP-Bans und Alt-Account-Erkennung", "IP bans and alt account detection"),
            ("SQLite oder MySQL, netzwerkweit synchron", "SQLite or MySQL, synced across your network"),
            ("Team-Benachrichtigungen und automatische Aktionen ab X Verwarnungen", "Staff notifications and automatic actions after X warnings"),
        ],
        "commands": [
            ("/punish <spieler>", "ultrapunish.gui", ("Öffnet die Punish-GUI", "Open the punish GUI")),
            ("/ban · /tempban <spieler> [dauer] [grund]", "ultrapunish.ban", ("Spieler bannen", "Ban a player")),
            ("/mute · /tempmute <spieler> [dauer] [grund]", "ultrapunish.mute", ("Spieler muten", "Mute a player")),
            ("/kick <spieler> [grund]", "ultrapunish.kick", ("Spieler kicken", "Kick a player")),
            ("/warn <spieler> [grund]", "ultrapunish.warn", ("Spieler verwarnen", "Warn a player")),
            ("/unban · /unmute <spieler>", "ultrapunish.revoke", ("Strafe aufheben", "Revoke a punishment")),
            ("/history <spieler>", "ultrapunish.history", ("Historie anzeigen", "Show the history")),
            ("/ultrapunish <help|reload|version>", "ultrapunish.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultrapunish.notify", ("Erhält Team-Benachrichtigungen", "Receive staff notifications")), ("ultrapunish.exempt", ("Kann nicht bestraft werden", "Cannot be punished"))],
        "messages": [("mute-applied", "<c>Steve</c> was muted for <c>1h</c> · Spam"), ("ban-revoked", "<c>Steve</c> was unbanned.")],
        "error": ("target-exempt", "You can't punish Steve – higher rank."),
    },
    {
        "name": "Homes",
        "tagline": ("Homes setzen, verwalten und teilen – in einer GUI.", "Set, manage and share homes in one GUI."),
        "chips": ["Homes GUI", "Limits", "Sharing"],
        "intro": (
            "UltraHomes gibt Spielern ihre Homes in einer aufgeräumten GUI: eigene Icons, Limits per Permission "
            "und Homes, die sich mit Freunden teilen lassen.",
            "UltraHomes gives players their homes in a tidy GUI: custom icons, permission-based limits "
            "and homes that can be shared with friends.",
        ),
        "features": [
            ("Homes-GUI mit frei wählbaren Icons", "Homes GUI with freely selectable icons"),
            ("Limits per Permission (ultrahomes.limit.<anzahl>)", "Limits by permission (ultrahomes.limit.<amount>)"),
            ("Umbenennen, verschieben und löschen mit Bestätigung", "Rename, relocate and delete with confirmation"),
            ("Homes mit Freunden teilen oder öffentlich machen", "Share homes with friends or make them public"),
            ("Warmup, Cooldown und Welt-Blacklist", "Warm-up, cooldown and world blacklist"),
            ("Import aus EssentialsX", "Import from EssentialsX"),
        ],
        "commands": [
            ("/home [name]", "ultrahomes.home", ("Teleportiert zu einem Home", "Teleport to a home")),
            ("/homes", "ultrahomes.home", ("Öffnet die Homes-GUI", "Open the homes GUI")),
            ("/sethome [name]", "ultrahomes.set", ("Setzt ein Home", "Set a home")),
            ("/delhome <name>", "ultrahomes.delete", ("Löscht ein Home", "Delete a home")),
            ("/ultrahomes <help|reload|version|import>", "ultrahomes.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultrahomes.limit.<n>", ("Maximale Anzahl Homes", "Maximum number of homes")), ("ultrahomes.others", ("Homes anderer Spieler verwalten", "Manage other players' homes"))],
        "messages": [("home-set", "Home <c>base</c> saved."), ("home-teleport", "Welcome home.")],
        "error": ("limit-reached", "Home limit reached (3/3)."),
    },
    {
        "name": "Warps",
        "tagline": ("Server- und Spieler-Warps mit Kategorien.", "Server & player warps with categories."),
        "chips": ["Warp GUI", "Player Warps", "Categories"],
        "intro": (
            "UltraWarps bringt Server-Warps in eine GUI mit Kategorien und Icons. Optional dürfen Spieler "
            "eigene Warps erstellen, zum Beispiel für ihren Shop.",
            "UltraWarps puts server warps into a GUI with categories and icons. Optionally players can "
            "create their own warps, for example for their shop.",
        ),
        "features": [
            ("Warp-GUI mit Kategorien, Icons und Suche", "Warp GUI with categories, icons and search"),
            ("Optionale Spieler-Warps mit Kosten und Besucherzähler", "Optional player warps with costs and visit counter"),
            ("Permission pro Warp möglich", "Optional permission per warp"),
            ("Sortierung nach Name, Beliebtheit oder Datum", "Sort by name, popularity or date"),
            ("Warmup, Cooldown und Teleport-Kosten", "Warm-up, cooldown and teleport costs"),
            ("Import aus EssentialsX", "Import from EssentialsX"),
        ],
        "commands": [
            ("/warp [name]", "ultrawarps.warp", ("Teleportiert zu einem Warp", "Teleport to a warp")),
            ("/warps", "ultrawarps.warp", ("Öffnet die Warp-GUI", "Open the warp GUI")),
            ("/setwarp <name>", "ultrawarps.set", ("Erstellt einen Server-Warp", "Create a server warp")),
            ("/delwarp <name>", "ultrawarps.delete", ("Löscht einen Warp", "Delete a warp")),
            ("/pwarp <create|delete|list>", "ultrawarps.player", ("Spieler-Warps", "Player warps")),
            ("/ultrawarps <help|reload|version|import>", "ultrawarps.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultrawarps.warp.<name>", ("Zugriff auf einen bestimmten Warp", "Access to a specific warp")), ("ultrawarps.player.limit.<n>", ("Maximale Anzahl Spieler-Warps", "Maximum number of player warps"))],
        "messages": [("warp-teleport", "Warped to <c>shop</c>."), ("warp-created", "Warp <c>arena</c> created.")],
        "error": ("warp-unknown", "Warp arena doesn't exist."),
    },
    {
        "name": "Chat",
        "tagline": ("Formatierung, Kanäle & Moderation für deinen Chat.", "Formatting, channels & moderation for your chat."),
        "chips": ["Formats", "Channels", "Filter"],
        "intro": (
            "UltraChat übernimmt den kompletten Chat: Formate pro Rang, Kanäle, Privatnachrichten und ein Filter "
            "gegen Spam, Caps und Werbung.",
            "UltraChat takes over the entire chat: formats per rank, channels, private messages and a filter "
            "against spam, caps and advertising.",
        ),
        "features": [
            ("MiniMessage-Formate pro Rang mit Hover- und Klick-Aktionen", "MiniMessage formats per rank with hover and click actions"),
            ("Kanäle: Global, Lokal, Team und eigene", "Channels: global, local, staff and custom ones"),
            ("Privatnachrichten mit /msg und /r, SocialSpy fürs Team", "Private messages with /msg and /r, social spy for staff"),
            ("Filter gegen Beleidigungen, Spam, Caps und Werbung", "Filter against swearing, spam, caps and advertising"),
            ("Erwähnungen mit Sound, [item] zum Zeigen von Items", "Mentions with sound, [item] to show items"),
            ("Chat leeren, sperren und Slow-Mode", "Clear chat, mute chat and slow mode"),
        ],
        "commands": [
            ("/msg <spieler> <text> · /r <text>", "ultrachat.msg", ("Privatnachrichten", "Private messages")),
            ("/channel [name]", "ultrachat.channel", ("Kanal wechseln", "Switch channel")),
            ("/chatclear", "ultrachat.clear", ("Chat leeren", "Clear the chat")),
            ("/mutechat", "ultrachat.mutechat", ("Chat sperren / freigeben", "Lock / unlock the chat")),
            ("/slowmode <sekunden>", "ultrachat.slowmode", ("Slow-Mode setzen", "Set slow mode")),
            ("/socialspy", "ultrachat.socialspy", ("Privatnachrichten mitlesen", "Read private messages")),
            ("/ultrachat <help|reload|version>", "ultrachat.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultrachat.color", ("Farben im Chat nutzen", "Use colours in chat")), ("ultrachat.bypass.filter", ("Filter umgehen", "Bypass the filter"))],
        "messages": [("channel-switched", "Channel switched to <c>Staff</c>."), ("chat-cleared", "Chat cleared by <c>Alex</c>.")],
        "error": ("slowmode", "Slow down! You can chat again in 3s."),
    },
    {
        "name": "Economy",
        "tagline": ("Schnelle, Vault-fähige Economy mit mehreren Währungen.", "Fast, Vault-ready economy with multiple currencies."),
        "chips": ["Vault", "Multi-Currency", "Baltop GUI"],
        "intro": (
            "UltraEconomy ist die Wirtschafts-Basis der Serie: Vault-Anbindung, mehrere Währungen, "
            "eine Baltop-GUI und ein lückenloses Transaktions-Log.",
            "UltraEconomy is the economy base of the series: Vault support, multiple currencies, "
            "a baltop GUI and a complete transaction log.",
        ),
        "features": [
            ("Vault-Provider, kompatibel mit jedem Vault-Plugin", "Vault provider, compatible with any Vault plugin"),
            ("Mehrere Währungen (z. B. Coins und Gems)", "Multiple currencies (e.g. coins and gems)"),
            ("/pay mit Bestätigung ab einstellbarem Betrag", "/pay with confirmation above a configurable amount"),
            ("Baltop-GUI mit Spielerköpfen", "Baltop GUI with player heads"),
            ("Transaktions-Log für jede Buchung", "Transaction log for every booking"),
            ("SQLite oder MySQL, netzwerkweit synchron", "SQLite or MySQL, synced across your network"),
        ],
        "commands": [
            ("/balance [spieler]", "ultraeconomy.balance", ("Kontostand anzeigen", "Show a balance")),
            ("/pay <spieler> <betrag>", "ultraeconomy.pay", ("Geld überweisen", "Send money")),
            ("/baltop", "ultraeconomy.baltop", ("Öffnet die Rangliste", "Open the leaderboard")),
            ("/eco <give|take|set> <spieler> <betrag>", "ultraeconomy.eco", ("Kontostände ändern", "Modify balances")),
            ("/ultraeconomy <help|reload|version>", "ultraeconomy.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultraeconomy.balance.others", ("Kontostand anderer sehen", "See other players' balances")), ("ultraeconomy.log", ("Transaktions-Log einsehen", "View the transaction log"))],
        "messages": [("pay-sent", "You sent <c>$250</c> to <c>Steve</c>."), ("pay-received", "<c>+ $250</c> from <c>Alex</c>")],
        "error": ("insufficient-funds", "Insufficient funds – you're missing $120."),
    },
    {
        "name": "Kits",
        "tagline": ("Kits mit Vorschau, Cooldowns & Ingame-Editor.", "Kits with previews, cooldowns & in-game editor."),
        "chips": ["Kit GUI", "Editor", "Cooldowns"],
        "intro": (
            "UltraKits macht Kits einfach: im Spiel aus dem eigenen Inventar erstellen, in einer GUI anbieten "
            "und per Cooldown, Preis oder Permission begrenzen.",
            "UltraKits makes kits easy: create them in-game from your own inventory, offer them in a GUI "
            "and limit them by cooldown, price or permission.",
        ),
        "features": [
            ("Kit-GUI mit Vorschau jedes Kits", "Kit GUI with a preview of every kit"),
            ("Ingame-Editor: Kit direkt aus dem Inventar erstellen", "In-game editor: create a kit straight from your inventory"),
            ("Cooldowns, Einmal-Kits, Kosten und Permissions", "Cooldowns, one-time kits, costs and permissions"),
            ("First-Join-Kit für neue Spieler", "First-join kit for new players"),
            ("Rüstung wird automatisch angelegt", "Armour is equipped automatically"),
            ("Befehle als Kit-Inhalt möglich", "Kits can run commands as well"),
        ],
        "commands": [
            ("/kit [name]", "ultrakits.kit", ("Kit abholen", "Claim a kit")),
            ("/kits", "ultrakits.kit", ("Öffnet die Kit-GUI", "Open the kit GUI")),
            ("/createkit <name>", "ultrakits.create", ("Kit aus dem Inventar erstellen", "Create a kit from your inventory")),
            ("/editkit <name>", "ultrakits.edit", ("Kit bearbeiten", "Edit a kit")),
            ("/delkit <name>", "ultrakits.delete", ("Kit löschen", "Delete a kit")),
            ("/ultrakits <help|reload|version>", "ultrakits.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultrakits.kit.<name>", ("Zugriff auf ein bestimmtes Kit", "Access to a specific kit")), ("ultrakits.bypass.cooldown", ("Kein Cooldown", "Skip the cooldown"))],
        "messages": [("kit-received", "Kit <c>Starter</c> received."), ("kit-created", "Kit <c>Miner</c> created.")],
        "error": ("kit-cooldown", "Kit Starter is on cooldown – 23h 59m left."),
    },
    {
        "name": "Vanish",
        "tagline": ("Echte Unsichtbarkeit für dein Team.", "True invisibility for your staff team."),
        "chips": ["Full Vanish", "Silent Chests", "Levels"],
        "intro": (
            "UltraVanish versteckt Teammitglieder vollständig: aus der Tabliste, der Spielerzahl und der Welt. "
            "Solange man unsichtbar ist, zeigt die Actionbar dauerhaft den Status an.",
            "UltraVanish hides staff members completely: from the tab list, the player count and the world. "
            "While vanished, the action bar permanently shows your status.",
        ),
        "features": [
            ("Vollständig unsichtbar: Tabliste, Spielerzahl, Join- und Quit-Nachrichten", "Completely invisible: tab list, player count, join and quit messages"),
            ("Kisten lautlos und ohne Animation öffnen", "Open chests silently and without animation"),
            ("Kein Mob-Targeting, kein Item-Aufheben, keine Druckplatten", "No mob targeting, no item pickup, no pressure plates"),
            ("Dauerhafte Status-Anzeige in der Actionbar", "Persistent status indicator in the action bar"),
            ("Vanish-Level: höhere Ränge sehen niedrigere", "Vanish levels: higher ranks see lower ones"),
            ("Fake-Join und Fake-Quit", "Fake join and fake quit"),
        ],
        "commands": [
            ("/vanish [spieler]", "ultravanish.vanish", ("Vanish an/aus (andere: ultravanish.vanish.others)", "Toggle vanish (others: ultravanish.vanish.others)")),
            ("/vanishlist", "ultravanish.list", ("Zeigt unsichtbare Spieler", "List vanished players")),
            ("/fakejoin · /fakequit", "ultravanish.fake", ("Sendet eine Fake-Nachricht", "Send a fake message")),
            ("/ultravanish <help|reload|version>", "ultravanish.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultravanish.level.<n>", ("Vanish-Level", "Vanish level")), ("ultravanish.see", ("Sieht unsichtbare Spieler", "See vanished players"))],
        "messages": [("vanish-status", "VANISHED · invisible to <c>12</c> players"), ("vanish-off", "You are visible again.")],
        "error": ("target-higher-level", "You can't vanish Steve – higher vanish level."),
    },
    {
        "name": "Reports",
        "tagline": ("Spieler-Reports mit Team-Workflow in der GUI.", "Player reports with a staff GUI workflow."),
        "chips": ["Report GUI", "Staff Inbox", "Discord"],
        "intro": (
            "UltraReports gibt Spielern eine einfache Möglichkeit, Regelverstöße zu melden, und dem Team einen "
            "Posteingang, in dem jeder Report übernommen, bearbeitet und abgeschlossen wird.",
            "UltraReports gives players an easy way to report rule breakers and gives staff an inbox "
            "where every report is claimed, handled and closed.",
        ),
        "features": [
            ("Report-GUI mit konfigurierbaren Gründen", "Report GUI with configurable reasons"),
            ("Team-Posteingang mit Status: offen, in Bearbeitung, erledigt", "Staff inbox with status: open, in progress, closed"),
            ("Report übernehmen und direkt zum Spieler teleportieren", "Claim a report and teleport straight to the player"),
            ("Cooldown und Schutz vor Missbrauch", "Cooldown and abuse protection"),
            ("Discord-Webhook und netzwerkweite Synchronisation", "Discord webhook and network-wide sync"),
            ("Mit UltraPunish direkt aus dem Report bestrafen", "Punish straight from the report with UltraPunish"),
        ],
        "commands": [
            ("/report <spieler> [grund]", "ultrareports.report", ("Spieler melden", "Report a player")),
            ("/reports", "ultrareports.staff", ("Öffnet den Team-Posteingang", "Open the staff inbox")),
            ("/ultrareports <help|reload|version>", "ultrareports.admin", ("Verwaltung", "Administration")),
        ],
        "extra_perms": [("ultrareports.notify", ("Erhält Benachrichtigungen über neue Reports", "Receive notifications about new reports")), ("ultrareports.bypass.cooldown", ("Kein Cooldown", "Skip the cooldown"))],
        "messages": [("report-sent", "Thanks! Your report on <c>Steve</c> was submitted."), ("report-claimed", "You claimed report <c>#42</c>.")],
        "error": ("already-reported", "You already reported Steve recently."),
    },
]

# 24x24 stroke icons, drawn for this series.
ICONS = {
    "Spawn": '<circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="1.6"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/>',
    "Teleports": '<path d="M4 8h14M14 4l4 4-4 4M20 16H6M10 12l-4 4 4 4"/>',
    "Punish": '<circle cx="12" cy="12" r="9"/><path d="M5.7 5.7l12.6 12.6"/>',
    "Homes": '<path d="M3 11.5l9-8 9 8M5.5 9.5V20.5h13V9.5M10 20.5v-6h4v6"/>',
    "Warps": '<path d="M12 22s7-7.2 7-12a7 7 0 1 0-14 0c0 4.8 7 12 7 12z"/><circle cx="12" cy="10" r="2.5"/>',
    "Chat": '<path d="M5 4h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-8l-5 4v-4H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"/><path d="M8 10.5h.01M12 10.5h.01M16 10.5h.01"/>',
    "Economy": '<circle cx="12" cy="12" r="9"/><path d="M15 9.6c0-1.3-1.3-2.3-3-2.3s-3 1-3 2.3 1.2 2 3 2.4 3 1.1 3 2.4-1.3 2.3-3 2.3-3-1-3-2.3M12 5.5v13"/>',
    "Kits": '<path d="M3 9h18v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 19zM5 9V6.5A2.5 2.5 0 0 1 7.500 4h9A2.5 2.5 0 0 1 19 6.5V9M3 13h7M14 13h7"/><rect x="10" y="11.2" width="4" height="3.6" rx=".6"/>',
    "Vanish": '<path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.600 6.500-10 6.500S2 12 2 12z"/><circle cx="12" cy="12" r="2.8"/><path d="M4.500 3.500l15 17"/>',
    "Reports": '<path d="M5 21.500V3.500M5 4.500h13l-2.500 4.500 2.500 4.500H5"/>',
}

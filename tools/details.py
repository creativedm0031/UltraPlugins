"""Long-form store page content per plugin (English).

tagline:  one sentence, under 100 characters
features: (heading, explanation) – what it does and how it works
storage:  files the plugin writes in plugins/Ultra<Name>/
faq:      (question, answer)
"""

DETAILS = {
    "Spawn": {
        "tagline": "Multiple spawns, a first-join spawn and void rescue with clean action-bar messages.",
        "features": [
            ("One or many spawns", "Set the main spawn with /setspawn or name extra spawns with /setspawn <name>. Every spawn keeps its exact position and view direction."),
            ("First-join spawn", "A spawn named firstjoin is used for players who join for the very first time – perfect for a tutorial or rules area."),
            ("Rank and world spawns", "Give a group the permission ultraspawn.group.<name> and they spawn at that spawn. With per-world-spawns enabled, a spawn named like a world is used inside that world."),
            ("Automatic teleports", "Choose separately whether players are sent to spawn on join and on respawn. Players with a bed or respawn anchor can keep respawning there."),
            ("Void rescue", "Players who fall out of the world are caught a few blocks below the bottom and placed back at spawn instead of dying – per world or everywhere."),
            ("Warm-up and cooldown", "A countdown runs in the action bar and is cancelled when the player moves or takes damage. Staff can skip both with bypass permissions."),
        ],
        "storage": [("spawns.yml", "all spawn points")],
        "faq": [
            ("How do I make a VIP spawn?", "Stand at the spot, run /setspawn vip and give your VIP group the permission ultraspawn.group.vip."),
            ("Can players still use their bed?", "Yes. With respect-bed-spawn: true (default) players with a bed or respawn anchor respawn there."),
            ("Does /spawn work from the console?", "Yes, /spawn <player> sends a player to spawn instantly (needs ultraspawn.spawn.others)."),
        ],
    },
    "Teleports": {
        "tagline": "TPA requests, safe random teleport, /back and staff teleports in one clean plugin.",
        "features": [
            ("Teleport requests", "/tpa asks to teleport to a player, /tpahere asks them to come to you. The receiver gets one clickable chat line with [Accept] and [Deny]; requests expire after 60 seconds (configurable)."),
            ("Privacy controls", "Players turn incoming requests off with /tptoggle or ignore single players with /tpignore."),
            ("/back", "Return to the place you were before your last teleport – or to where you died, if back-on-death is enabled."),
            ("Safe random teleport", "/rtp picks a random spot between a minimum and maximum distance from the world border centre and only lands on solid, harmless ground – no lava, water, cactus or magma. Works in the Nether as well."),
            ("Costs and cooldowns", "Every teleport type has its own cooldown (rtp defaults to 60 seconds). Random teleports can cost money through Vault, for example with UltraEconomy."),
            ("Staff teleports", "/tp, /tphere and /tpall teleport instantly without warm-up, for moderators and admins."),
        ],
        "storage": [("players.yml", "who disabled requests and who ignores whom")],
        "faq": [
            ("Which worlds allow /rtp?", "Only the worlds listed under rtp.worlds in config.yml – by default just world."),
            ("Can I charge for /rtp?", "Yes, set rtp.cost. It needs Vault and any economy plugin, such as UltraEconomy."),
            ("Does the warm-up apply to staff teleports?", "No, /tp, /tphere and /tpall are always instant."),
        ],
    },
    "Punish": {
        "tagline": "Bans, mutes, kicks and warnings with a template GUI, escalation and full player history.",
        "features": [
            ("All punishment types", "Ban, temp-ban, IP ban, mute, temp-mute, kick and warn. Durations are written like 30m, 12h, 7d or 2w and can be combined (1d12h)."),
            ("Punish GUI with templates", "/punish <player> opens a menu with your reason templates. Each repeat of the same reason moves one step further, e.g. Spam: 30m → 2h → 1d."),
            ("Automatic actions", "Run console commands when a player reaches a number of warnings – for example a one-day ban at three warnings."),
            ("History", "/history <player> shows every punishment with reason, staff member, date, duration and whether it is still active."),
            ("Alt detection", "Staff are notified when someone joins from the same IP address as a banned account. IP bans block every account on that address."),
            ("Localised for everyone", "Staff notifications, mute messages and the kick screen are shown to each player in their own language."),
        ],
        "storage": [("punishments.yml", "all punishments, names and last IP addresses")],
        "faq": [
            ("Can staff punish each other?", "Players with ultrapunish.exempt cannot be punished by other players (the console still can)."),
            ("What does a muted player see?", "Chat and the commands listed in muted-commands are blocked, and they get an error with the remaining time and reason."),
            ("Can I punish offline players?", "Yes, as long as they joined the server once while UltraPunish was installed."),
        ],
    },
    "Homes": {
        "tagline": "Set, manage and publish homes in a clean GUI with permission-based limits.",
        "features": [
            ("Homes GUI", "/homes shows every home with world and coordinates. Left-click teleports, right-click opens the edit menu."),
            ("Edit menu", "Change the icon to the item in your hand, move a home to your current position, make it public or delete it with a confirmation menu."),
            ("Public homes", "Public homes can be visited by everyone with /home <player>:<home>. Staff with ultrahomes.others can visit any home."),
            ("Limits by permission", "Everyone gets default-limit homes (3). Give ultrahomes.limit.<number> for more – the highest number a player has counts."),
            ("Blocked worlds", "List worlds where no home may be set, such as event or minigame worlds."),
            ("EssentialsX import", "/ultrahomes import copies all homes from EssentialsX. Existing homes with the same name are kept."),
        ],
        "storage": [("homes.yml", "all homes with position, icon and public flag")],
        "faq": [
            ("What does /home without a name do?", "With one home it teleports there directly, with more it opens the GUI."),
            ("How do I give VIPs more homes?", "Give the VIP group ultrahomes.limit.10 (or any number)."),
            ("Is there a warm-up?", "Yes, 3 seconds by default, cancelled on movement or damage. Set warmup: 0 to disable it."),
        ],
    },
    "Warps": {
        "tagline": "Server and player warps with categories, custom icons and a visit counter in one GUI.",
        "features": [
            ("Warp GUI", "/warps lists every warp with category, owner and visits. Filter by category and sort by name, popularity or newest."),
            ("Categories and icons", "/setwarp <name> [category] creates a warp; the item in your hand becomes its icon."),
            ("Player warps", "With /pwarp players create their own warps, for example for their shop. Limit them with ultrawarps.player.limit.<number> and charge a creation fee through Vault."),
            ("Visit counter", "Every visit is counted, so popular warps rise to the top when sorting by popularity."),
            ("Per-warp permissions", "Optionally require ultrawarps.warp.<name> for every server warp to build VIP-only or staff-only warps."),
            ("EssentialsX import", "/ultrawarps import copies all warps from EssentialsX in one go."),
        ],
        "storage": [("warps.yml", "all warps with position, category, icon, owner and visits")],
        "faq": [
            ("How do I change a warp icon?", "Hold the new item and run /setwarp <name> again – the position and icon are updated, visits are kept."),
            ("Can players delete other players' warps?", "No, /pwarp delete only works for your own warps. Staff use /delwarp."),
            ("Where are player warps listed?", "In their own category (player by default), also reachable with /pwarp list."),
        ],
    },
    "Chat": {
        "tagline": "Rank-based chat formats, channels, private messages and a smart spam and advertising filter.",
        "features": [
            ("Formats per rank", "Write chat formats in MiniMessage with gradients, hover and click actions. Players get the first format they have ultrachat.format.<name> for."),
            ("Channels", "Global, local (with radius) and a staff channel out of the box, plus as many custom channels as you like. Switch with /channel."),
            ("Private messages", "/msg and /r with the usual aliases (tell, w, whisper, pm, reply). Staff can read along with /socialspy."),
            ("Filter", "Blocks advertising (with an allowlist for your own domain), repeated messages and replaces blocked words. Messages written mostly in capitals are turned into lower case."),
            ("Mentions and [item]", "@Name highlights the name and notifies that player in the action bar. [item] shows the item in your hand with its tooltip on hover."),
            ("Moderation tools", "/chatclear clears the chat, /mutechat locks it for everyone without bypass, /slowmode sets a delay between messages."),
        ],
        "storage": [],
        "faq": [
            ("Does it work with LuckPerms prefixes?", "Formats use the player's display name. Put the prefix into the format or use a plugin that sets the display name."),
            ("Can players use colour codes?", "Players with ultrachat.color can use &-codes in chat."),
            ("Do other plugins still see chat messages?", "Yes, the chat event stays active, so logging and Discord bridges keep working."),
        ],
    },
    "Economy": {
        "tagline": "A fast Vault economy with multiple currencies, a baltop GUI and a complete transaction log.",
        "features": [
            ("Vault provider", "UltraEconomy registers itself in Vault, so every shop, job or rank plugin that supports Vault works with it immediately."),
            ("Multiple currencies", "Define as many currencies as you like with their own symbol, decimals and start balance. The first one is used by Vault."),
            ("/pay with confirmation", "Payments above a configurable amount must be confirmed with a click, so nobody sends a fortune by accident. A minimum amount prevents spam."),
            ("Baltop GUI", "/baltop shows the richest players with their heads and switches between currencies."),
            ("Admin commands", "/eco give, take and set change balances of any player and any currency."),
            ("Transaction log", "Every booking is written to transactions.log. Browse a player's history in-game with /ultraeconomy log <player>."),
        ],
        "storage": [("balances.yml", "all balances"), ("transactions.log", "every transaction with time, type, amount and both players")],
        "faq": [
            ("Do I need Vault?", "Only if other plugins should use the economy. UltraEconomy itself works without it."),
            ("How do I give money to everyone who joins?", "Set the start value of a currency – new players begin with that balance."),
            ("Can I use whole numbers only?", "Yes, set decimals: 0 for that currency."),
        ],
    },
    "Kits": {
        "tagline": "Kits with a preview GUI, cooldowns, prices and an in-game editor – no config editing needed.",
        "features": [
            ("Create in-game", "Fill your inventory and run /createkit <name>. Enchantments, names and lore are kept exactly."),
            ("Kit GUI with preview", "/kits shows every kit with status, item count and price. Left-click claims, right-click opens a preview of all items."),
            ("Cooldowns, prices, one-time kits", "/editkit sets the cooldown (e.g. 12h), a price through Vault or makes a kit one-time only."),
            ("First-join kit", "Mark any kit with /editkit <kit> firstjoin and new players receive it automatically."),
            ("Smart giving", "Armour is equipped automatically when the slot is empty, and items that do not fit are dropped at the player's feet."),
            ("Commands as rewards", "Add console commands to a kit in kits.yml, e.g. to give money or a rank together with the items."),
        ],
        "storage": [("kits.yml", "all kits"), ("usage.yml", "when each player last claimed each kit")],
        "faq": [
            ("Why can't players claim a new kit?", "New kits need ultrakits.kit.<name>. Set permission: false in kits.yml to make a kit available to everyone."),
            ("How do I change the items of a kit?", "/editkit <kit> items opens an inventory – put the items in and close it to save."),
            ("Can staff give kits to others?", "Yes, /kit <kit> <player> with ultrakits.give – no cooldown or price applies."),
        ],
    },
    "Vanish": {
        "tagline": "True invisibility for staff, hidden from the tab list, player count and every interaction.",
        "features": [
            ("Completely hidden", "Vanished players disappear from the world, the tab list and the server list player count. No join or quit message is shown."),
            ("Always know your status", "While vanished, the action bar permanently shows how many players cannot see you."),
            ("Silent chests", "Open chests and other containers without animation or sound. Sneak while clicking to open them normally."),
            ("No interactions", "Mobs ignore vanished players, items are not picked up and pressure plates or tripwires are not triggered."),
            ("Vanish levels", "Staff with ultravanish.see see vanished players of the same or a lower level (ultravanish.level.<number>)."),
            ("Fake join and quit", "Send a fake quit message when vanishing and a fake join message when reappearing – automatically or with /fakequit and /fakejoin."),
        ],
        "storage": [("config.yml", "the list of vanished players, so vanish survives restarts")],
        "faq": [
            ("Can I vanish automatically on join?", "Yes, set vanish-on-join: true – staff with ultravanish.vanish join invisibly."),
            ("Can a moderator see an admin?", "Only if the moderator's vanish level is the same or higher."),
            ("Do changes in silent chests save?", "Yes, everything you move is written back into the real container when you close it."),
        ],
    },
    "Reports": {
        "tagline": "Player reports with a reason GUI, a staff inbox with claim workflow and a Discord webhook.",
        "features": [
            ("Simple reporting", "/report <player> opens a menu with your configured reasons, or players write their own reason directly after the name."),
            ("Staff inbox", "/reports lists all reports by status: open, in progress and closed – with reason, reporter, date and whether the player is online."),
            ("Claim workflow", "Claim a report so the team knows who handles it. Claiming can teleport you straight to the reported player."),
            ("Punish from the report", "With UltraPunish installed, one click opens the punish GUI for the reported player."),
            ("Notifications", "Staff get a clickable chat line for every new report, and the reporter is thanked when the report is closed."),
            ("Discord webhook", "New reports are posted to a Discord channel with player, reporter and reason."),
        ],
        "storage": [("reports.yml", "all reports with status and handler")],
        "faq": [
            ("Can players spam reports?", "No, there is a cooldown and a player cannot report the same person again while that report is open."),
            ("Can staff be protected from reports?", "Give them ultrareports.exempt."),
            ("How do I connect Discord?", "Create a webhook in your Discord channel settings and paste the URL into discord-webhook."),
        ],
    },
}

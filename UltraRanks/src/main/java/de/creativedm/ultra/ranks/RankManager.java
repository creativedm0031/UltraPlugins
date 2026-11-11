package de.creativedm.ultra.ranks;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Messages;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Ranks, player assignments, effective permissions, tab list, name tags and the change log. */
final class RankManager {

    /** Player assignment. until = 0 means permanent. */
    record Assignment(String rank, long until, String previous) {}

    private static final Method LIST_ORDER = findListOrder();

    private final UltraRanks plugin;
    private final DataFile rankFile;
    private final DataFile playerFile;
    private final Map<String, Rank> ranks = new LinkedHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Object logLock = new Object();

    RankManager(UltraRanks plugin) {
        this.plugin = plugin;
        this.rankFile = new DataFile(plugin, "ranks.yml");
        this.playerFile = new DataFile(plugin, "players.yml");
        load();
    }

    // ---- ranks ----------------------------------------------------------------------------------------------

    private void load() {
        ranks.clear();
        ConfigurationSection section = rankFile.yaml().getConfigurationSection("ranks");
        if (section == null || section.getKeys(false).isEmpty()) {
            createDefaults();
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            Rank rank = new Rank(id);
            rank.display = s.getString("display", id);
            rank.prefix = s.getString("prefix", "");
            rank.suffix = s.getString("suffix", "");
            rank.weight = s.getInt("weight");
            rank.nameColor = s.getString("name-color", "WHITE");
            rank.icon = s.getString("icon", "NAME_TAG");
            rank.description = s.getString("description", "");
            rank.price = s.getDouble("price");
            rank.purchaseDuration = s.getString("purchase-duration", "permanent");
            rank.parents.addAll(s.getStringList("parents"));
            // "node" grants, "-node" negates
            for (String entry : s.getStringList("permissions")) {
                if (entry.startsWith("-")) rank.permissions.put(entry.substring(1), false);
                else rank.permissions.put(entry, true);
            }
            ranks.put(id, rank);
        }
    }

    private void createDefaults() {
        Rank player = new Rank("default");
        player.display = "<gray>Player</gray>";
        player.prefix = "<gray>Player</gray> <dark_gray>|</dark_gray> ";
        player.nameColor = "GRAY";
        player.icon = "LEATHER_HELMET";
        player.description = "Every new player starts here.";
        ranks.put(player.id, player);

        Rank vip = new Rank("vip");
        vip.display = "<gradient:#FACC15:#F97316>VIP</gradient>";
        vip.prefix = "<gradient:#FACC15:#F97316><bold>VIP</bold></gradient> <dark_gray>|</dark_gray> ";
        vip.weight = 10;
        vip.nameColor = "YELLOW";
        vip.icon = "GOLD_INGOT";
        vip.description = "Supporter rank with extra homes and colours in chat.";
        vip.parents.add("default");
        vip.permissions.put("ultrahomes.limit.5", true);
        vip.permissions.put("ultrachat.color", true);
        vip.price = 5000;
        vip.purchaseDuration = "30d";
        ranks.put(vip.id, vip);

        Rank mod = new Rank("mod");
        mod.display = "<gradient:#22D3EE:#3B82F6>Moderator</gradient>";
        mod.prefix = "<gradient:#22D3EE:#3B82F6><bold>MOD</bold></gradient> <dark_gray>|</dark_gray> ";
        mod.weight = 50;
        mod.nameColor = "AQUA";
        mod.icon = "IRON_SWORD";
        mod.description = "Keeps the server safe.";
        mod.parents.add("vip");
        for (String perm : List.of("ultrapunish.*", "ultrareports.staff", "ultrareports.notify", "ultravanish.vanish", "ultravanish.see", "ultrachat.socialspy")) {
            mod.permissions.put(perm, true);
        }
        ranks.put(mod.id, mod);

        Rank admin = new Rank("admin");
        admin.display = "<gradient:#8B5CF6:#EF4444>Admin</gradient>";
        admin.prefix = "<gradient:#8B5CF6:#EF4444><bold>ADMIN</bold></gradient> <dark_gray>|</dark_gray> ";
        admin.weight = 100;
        admin.nameColor = "RED";
        admin.icon = "NETHERITE_HELMET";
        admin.description = "Full access to everything.";
        admin.parents.add("mod");
        admin.permissions.put("*", true);
        ranks.put(admin.id, admin);
        save();
    }

    void save() {
        rankFile.yaml().set("ranks", null);
        for (Rank rank : ranks.values()) {
            String path = "ranks." + rank.id + ".";
            rankFile.yaml().set(path + "display", rank.display);
            rankFile.yaml().set(path + "prefix", rank.prefix);
            rankFile.yaml().set(path + "suffix", rank.suffix);
            rankFile.yaml().set(path + "weight", rank.weight);
            rankFile.yaml().set(path + "name-color", rank.nameColor);
            rankFile.yaml().set(path + "icon", rank.icon);
            rankFile.yaml().set(path + "description", rank.description);
            rankFile.yaml().set(path + "price", rank.price);
            rankFile.yaml().set(path + "purchase-duration", rank.purchaseDuration);
            rankFile.yaml().set(path + "parents", new ArrayList<>(rank.parents));
            List<String> perms = new ArrayList<>();
            for (Map.Entry<String, Boolean> e : rank.permissions.entrySet()) perms.add(e.getValue() ? e.getKey() : "-" + e.getKey());
            rankFile.yaml().set(path + "permissions", perms);
        }
        rankFile.save();
    }

    Collection<Rank> ranks() {
        return ranks.values();
    }

    /** Highest weight first. */
    List<Rank> sorted() {
        List<Rank> list = new ArrayList<>(ranks.values());
        list.sort(Comparator.comparingInt((Rank r) -> r.weight).reversed().thenComparing(r -> r.id));
        return list;
    }

    Rank rank(String id) {
        return id == null ? null : ranks.get(id.toLowerCase(Locale.ROOT));
    }

    Rank defaultRank() {
        Rank rank = rank(plugin.getConfig().getString("default-rank", "default"));
        if (rank != null) return rank;
        List<Rank> sorted = sorted();
        return sorted.isEmpty() ? null : sorted.get(sorted.size() - 1);
    }

    Rank create(String id) {
        Rank rank = new Rank(id);
        Rank fallback = defaultRank();
        if (fallback != null) rank.parents.add(fallback.id);
        ranks.put(id, rank);
        save();
        return rank;
    }

    void delete(Rank rank) {
        ranks.remove(rank.id);
        for (Rank other : ranks.values()) other.parents.remove(rank.id);
        Rank fallback = defaultRank();
        for (String id : playerFile.yaml().getKeys(false)) {
            if (rank.id.equals(playerFile.yaml().getString(id + ".rank"))) {
                playerFile.yaml().set(id + ".rank", fallback == null ? null : fallback.id);
                playerFile.yaml().set(id + ".until", null);
            }
            if (rank.id.equals(playerFile.yaml().getString(id + ".previous"))) playerFile.yaml().set(id + ".previous", null);
        }
        playerFile.save();
        save();
        refreshAll();
    }

    /** True if making child inherit from parent would create a loop. */
    boolean wouldLoop(Rank child, Rank parent) {
        Set<String> seen = new HashSet<>();
        List<String> todo = new ArrayList<>(List.of(parent.id));
        while (!todo.isEmpty()) {
            String id = todo.remove(todo.size() - 1);
            if (id.equals(child.id)) return true;
            Rank r = ranks.get(id);
            if (r != null && seen.add(id)) todo.addAll(r.parents);
        }
        return false;
    }

    /** Own permissions plus everything inherited; own entries win over inherited ones. */
    Map<String, Boolean> permissions(Rank rank) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        collect(rank, out, new HashSet<>());
        return out;
    }

    private void collect(Rank rank, Map<String, Boolean> out, Set<String> visited) {
        if (rank == null || !visited.add(rank.id)) return;
        List<Rank> parents = new ArrayList<>();
        for (String id : rank.parents) if (ranks.containsKey(id)) parents.add(ranks.get(id));
        parents.sort(Comparator.comparingInt(r -> r.weight));
        for (Rank parent : parents) collect(parent, out, visited);
        out.putAll(rank.permissions);
    }

    /** Which rank a permission comes from, for the GUI. */
    String source(Rank rank, String permission) {
        if (rank.permissions.containsKey(permission)) return rank.id;
        for (String id : rank.parents) {
            Rank parent = ranks.get(id);
            if (parent != null && permissions(parent).containsKey(permission)) return source(parent, permission);
        }
        return null;
    }

    // ---- players --------------------------------------------------------------------------------------------

    Assignment assignment(UUID player) {
        String path = player.toString();
        Rank rank = rank(playerFile.yaml().getString(path + ".rank"));
        if (rank == null) rank = defaultRank();
        return new Assignment(rank == null ? null : rank.id, playerFile.yaml().getLong(path + ".until"), playerFile.yaml().getString(path + ".previous"));
    }

    Rank rankOf(UUID player) {
        return rank(assignment(player).rank());
    }

    /** until = 0: permanent. A temporary rank remembers the rank the player had before. */
    void set(UUID player, String name, Rank rank, long until) {
        String path = player.toString();
        Assignment current = assignment(player);
        playerFile.yaml().set(path + ".name", name);
        if (until > 0 && current.until() == 0 && !rank.id.equals(current.rank())) {
            playerFile.yaml().set(path + ".previous", current.rank());
        } else if (until == 0) {
            playerFile.yaml().set(path + ".previous", null);
        }
        playerFile.yaml().set(path + ".rank", rank.id);
        playerFile.yaml().set(path + ".until", until > 0 ? until : null);
        playerFile.save();
        Player online = Bukkit.getPlayer(player);
        if (online != null) refresh(online);
    }

    /** Ends an expired temporary rank. Returns the rank the player falls back to, or null. */
    Rank expire(UUID player) {
        Assignment a = assignment(player);
        if (a.until() == 0 || a.until() > System.currentTimeMillis()) return null;
        Rank back = rank(a.previous());
        if (back == null) back = defaultRank();
        String path = player.toString();
        playerFile.yaml().set(path + ".rank", back == null ? null : back.id);
        playerFile.yaml().set(path + ".until", null);
        playerFile.yaml().set(path + ".previous", null);
        playerFile.save();
        return back;
    }

    void remember(Player player) {
        String path = player.getUniqueId().toString();
        if (!player.getName().equals(playerFile.yaml().getString(path + ".name"))) {
            playerFile.yaml().set(path + ".name", player.getName());
            playerFile.save();
        }
    }

    /** Players (online or offline) stored with this rank. */
    List<Map.Entry<UUID, String>> members(Rank rank) {
        List<Map.Entry<UUID, String>> out = new ArrayList<>();
        for (String id : playerFile.yaml().getKeys(false)) {
            if (rank.id.equals(playerFile.yaml().getString(id + ".rank"))) {
                out.add(Map.entry(UUID.fromString(id), playerFile.yaml().getString(id + ".name", id.substring(0, 8))));
            }
        }
        out.sort(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    UUID find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        for (String id : playerFile.yaml().getKeys(false)) {
            if (name.equalsIgnoreCase(playerFile.yaml().getString(id + ".name"))) return UUID.fromString(id);
        }
        return null;
    }

    String name(UUID player) {
        Player online = Bukkit.getPlayer(player);
        return online != null ? online.getName() : playerFile.yaml().getString(player + ".name", player.toString().substring(0, 8));
    }

    // ---- applying a rank to an online player ----------------------------------------------------------------

    void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    void refresh(Player player) {
        Rank rank = rankOf(player.getUniqueId());
        applyPermissions(player, rank);
        applyDisplay(player, rank);
    }

    private void applyPermissions(Player player, Rank rank) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            try {
                player.removeAttachment(old);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
        }
        PermissionAttachment attachment = player.addAttachment(plugin);
        if (rank != null) {
            Map<String, Boolean> perms = permissions(rank);
            // Wildcards first (e.g. essentials.* or *), explicit entries override them.
            for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                String key = e.getKey();
                if (!key.equals("*") && !key.endsWith(".*")) continue;
                attachment.setPermission(key, e.getValue());
                if (!plugin.getConfig().getBoolean("wildcards", true)) continue;
                String prefix = key.equals("*") ? "" : key.substring(0, key.length() - 1);
                for (Permission known : Bukkit.getPluginManager().getPermissions()) {
                    if (known.getName().startsWith(prefix) && !perms.containsKey(known.getName())) attachment.setPermission(known.getName(), e.getValue());
                }
            }
            for (Map.Entry<String, Boolean> e : perms.entrySet()) {
                if (!e.getKey().equals("*") && !e.getKey().endsWith(".*")) attachment.setPermission(e.getKey(), e.getValue());
            }
        }
        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
        player.updateCommands();
    }

    private void applyDisplay(Player player, Rank rank) {
        String prefix = rank == null ? "" : rank.prefix;
        String suffix = rank == null ? "" : rank.suffix;
        String display = rank == null ? "" : rank.display;
        // Shared with UltraChat (and any other plugin) through player metadata.
        player.setMetadata("ultraranks_prefix", new FixedMetadataValue(plugin, prefix));
        player.setMetadata("ultraranks_suffix", new FixedMetadataValue(plugin, suffix));
        player.setMetadata("ultraranks_rank", new FixedMetadataValue(plugin, display));

        if (plugin.getConfig().getBoolean("tablist.enabled", true)) {
            Component name = plugin.msg().parse(plugin.getConfig().getString("tablist.format", "<prefix><white><player></white><suffix>"),
                    "prefix", plugin.msg().parse(prefix), "suffix", plugin.msg().parse(suffix), "player", player.getName(), "rank", plugin.msg().parse(display));
            player.setPlayerListName(Messages.legacy(name));
            if (LIST_ORDER != null && rank != null) {
                try {
                    LIST_ORDER.invoke(player, rank.weight);
                } catch (ReflectiveOperationException ignored) {
                    // older server
                }
            }
        }
        if (plugin.getConfig().getBoolean("nametag.enabled", true)) applyTeam(player, rank, prefix, suffix);
    }

    private static Method findListOrder() {
        try {
            return Player.class.getMethod("setPlayerListOrder", int.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /** Teams sort the tab list (higher weight first) and show prefix/suffix above the head. */
    private void applyTeam(Player player, Rank rank, String prefix, String suffix) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith("ur") && team.hasEntry(player.getName())) team.removeEntry(player.getName());
        }
        if (rank == null) return;
        int order = Math.max(0, Math.min(999, 500 - rank.weight));
        String teamName = String.format("ur%03d%s", order, rank.id.length() > 11 ? rank.id.substring(0, 11) : rank.id);
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        String format = plugin.getConfig().getString("nametag.prefix", "<prefix>");
        String formatSuffix = plugin.getConfig().getString("nametag.suffix", "<suffix>");
        team.setPrefix(Messages.legacy(plugin.msg().parse(format, "prefix", plugin.msg().parse(prefix), "rank", plugin.msg().parse(rank.display))));
        team.setSuffix(Messages.legacy(plugin.msg().parse(formatSuffix, "suffix", plugin.msg().parse(suffix), "rank", plugin.msg().parse(rank.display))));
        try {
            team.setColor(ChatColor.valueOf(rank.nameColor));
        } catch (IllegalArgumentException e) {
            team.setColor(ChatColor.WHITE);
        }
        team.addEntry(player.getName());
    }

    void forget(Player player) {
        attachments.remove(player.getUniqueId());
    }

    void cleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PermissionAttachment attachment = attachments.remove(player.getUniqueId());
            if (attachment != null) {
                try {
                    player.removeAttachment(attachment);
                } catch (IllegalArgumentException ignored) {
                    // already gone
                }
            }
        }
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : new ArrayList<>(board.getTeams())) if (team.getName().startsWith("ur")) team.unregister();
    }

    // ---- change log -----------------------------------------------------------------------------------------

    /** One line per change in changes.log: time|actor|type|a|b */
    void log(String actor, String type, String a, String b) {
        String line = System.currentTimeMillis() + "|" + actor + "|" + type + "|" + clean(a) + "|" + clean(b) + "\n";
        Runnable write = () -> {
            synchronized (logLock) {
                try {
                    Files.createDirectories(plugin.getDataFolder().toPath());
                    Files.writeString(new File(plugin.getDataFolder(), "changes.log").toPath(), line, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not write changes.log: " + e.getMessage());
                }
            }
        };
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTaskAsynchronously(plugin, write);
        else write.run();
    }

    private static String clean(String text) {
        return text == null ? "" : text.replace("|", "/").replace("\n", " ");
    }

    /** Newest first, at most limit lines. */
    List<String[]> readLog(int limit) {
        List<String[]> out = new ArrayList<>();
        File file = new File(plugin.getDataFolder(), "changes.log");
        if (!file.exists()) return out;
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0 && out.size() < limit; i--) {
                String[] parts = lines.get(i).split("\\|", -1);
                if (parts.length >= 5) out.add(parts);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read changes.log: " + e.getMessage());
        }
        return out;
    }
}

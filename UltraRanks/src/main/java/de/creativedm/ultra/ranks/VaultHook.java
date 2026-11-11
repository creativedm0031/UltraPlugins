package de.creativedm.ultra.ranks;

import de.creativedm.ultra.core.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

/** Ranks as Vault groups and prefixes, for shops, scoreboards and chat plugins. Only loaded when Vault is installed. */
final class VaultHook {

    private VaultHook() {}

    static void register(UltraRanks plugin) {
        Perms perms = new Perms(plugin);
        Bukkit.getServicesManager().register(Permission.class, perms, plugin, ServicePriority.High);
        Bukkit.getServicesManager().register(Chat.class, new Texts(plugin, perms), plugin, ServicePriority.High);
    }

    @SuppressWarnings("deprecation")
    private static UUID id(UltraRanks plugin, String name) {
        UUID id = plugin.ranks().find(name);
        return id != null ? id : Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    private static String legacy(UltraRanks plugin, String miniMessage) {
        return Messages.legacy(plugin.msg().parse(miniMessage == null ? "" : miniMessage));
    }

    static final class Perms extends Permission {
        private final UltraRanks ranks;

        Perms(UltraRanks plugin) {
            this.ranks = plugin;
            this.plugin = plugin;
        }

        @Override public String getName() { return "UltraRanks"; }
        @Override public boolean isEnabled() { return ranks.isEnabled(); }
        @Override public boolean hasSuperPermsCompat() { return true; }
        @Override public boolean hasGroupSupport() { return true; }

        @Override
        public boolean playerHas(String world, String name, String permission) {
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) return online.hasPermission(permission);
            Map<String, Boolean> perms = ranks.effectivePermissions(id(ranks, name));
            return Boolean.TRUE.equals(perms.get(permission));
        }

        // UltraRanks has no per-player permissions – only ranks.
        @Override public boolean playerAdd(String world, String name, String permission) { return false; }
        @Override public boolean playerRemove(String world, String name, String permission) { return false; }

        @Override
        public boolean groupHas(String world, String group, String permission) {
            Rank rank = ranks.ranks().rank(group);
            return rank != null && Boolean.TRUE.equals(ranks.ranks().permissions(rank).get(permission));
        }

        @Override
        public boolean groupAdd(String world, String group, String permission) {
            Rank rank = ranks.ranks().rank(group);
            if (rank == null) return false;
            rank.permissions.put(permission, true);
            ranks.ranks().save();
            ranks.ranks().refreshAll();
            return true;
        }

        @Override
        public boolean groupRemove(String world, String group, String permission) {
            Rank rank = ranks.ranks().rank(group);
            if (rank == null || rank.permissions.remove(permission) == null) return false;
            ranks.ranks().save();
            ranks.ranks().refreshAll();
            return true;
        }

        @Override
        public boolean playerInGroup(String world, String name, String group) {
            Rank rank = ranks.ranks().rankOf(id(ranks, name));
            if (rank == null) return false;
            if (rank.id.equalsIgnoreCase(group)) return true;
            Rank target = ranks.ranks().rank(group);
            return target != null && ranks.ranks().wouldLoop(target, rank); // rank inherits from target
        }

        @Override
        public boolean playerAddGroup(String world, String name, String group) {
            Rank rank = ranks.ranks().rank(group);
            if (rank == null) return false;
            UUID id = id(ranks, name);
            ranks.ranks().set(id, name, rank, 0);
            ranks.ranks().log("Vault", "set", name, rank.id);
            return true;
        }

        @Override
        public boolean playerRemoveGroup(String world, String name, String group) {
            UUID id = id(ranks, name);
            Rank current = ranks.ranks().rankOf(id);
            Rank fallback = ranks.ranks().defaultRank();
            if (current == null || fallback == null || !current.id.equalsIgnoreCase(group)) return false;
            ranks.ranks().set(id, name, fallback, 0);
            ranks.ranks().log("Vault", "set", name, fallback.id);
            return true;
        }

        @Override
        public String[] getPlayerGroups(String world, String name) {
            Rank rank = ranks.ranks().rankOf(id(ranks, name));
            return rank == null ? new String[0] : new String[] {rank.id};
        }

        @Override
        public String getPrimaryGroup(String world, String name) {
            Rank rank = ranks.ranks().rankOf(id(ranks, name));
            return rank == null ? null : rank.id;
        }

        @Override
        public String[] getGroups() {
            List<String> ids = new ArrayList<>();
            for (Rank rank : ranks.ranks().sorted()) ids.add(rank.id);
            return ids.toArray(new String[0]);
        }

        @Override public boolean playerHas(String world, OfflinePlayer player, String permission) { return playerHas(world, String.valueOf(player.getName()), permission); }
    }

    static final class Texts extends Chat {
        private final UltraRanks ranks;

        Texts(UltraRanks plugin, Permission perms) {
            super(perms);
            this.ranks = plugin;
        }

        @Override public String getName() { return "UltraRanks"; }
        @Override public boolean isEnabled() { return ranks.isEnabled(); }

        private Rank rankOf(String name) {
            return ranks.ranks().rankOf(id(ranks, name));
        }

        @Override public String getPlayerPrefix(String world, String name) { Rank r = rankOf(name); return r == null ? "" : legacy(ranks, r.prefix); }
        @Override public String getPlayerSuffix(String world, String name) { Rank r = rankOf(name); return r == null ? "" : legacy(ranks, r.suffix); }
        @Override public String getGroupPrefix(String world, String group) { Rank r = ranks.ranks().rank(group); return r == null ? "" : legacy(ranks, r.prefix); }
        @Override public String getGroupSuffix(String world, String group) { Rank r = ranks.ranks().rank(group); return r == null ? "" : legacy(ranks, r.suffix); }

        // Prefixes are set per rank in the GUI, not per player or through other plugins.
        @Override public void setPlayerPrefix(String world, String name, String prefix) {}
        @Override public void setPlayerSuffix(String world, String name, String suffix) {}
        @Override public void setGroupPrefix(String world, String group, String prefix) {}
        @Override public void setGroupSuffix(String world, String group, String suffix) {}

        @Override public int getPlayerInfoInteger(String world, String name, String node, int def) { return def; }
        @Override public void setPlayerInfoInteger(String world, String name, String node, int value) {}
        @Override public int getGroupInfoInteger(String world, String group, String node, int def) { return def; }
        @Override public void setGroupInfoInteger(String world, String group, String node, int value) {}
        @Override public double getPlayerInfoDouble(String world, String name, String node, double def) { return def; }
        @Override public void setPlayerInfoDouble(String world, String name, String node, double value) {}
        @Override public double getGroupInfoDouble(String world, String group, String node, double def) { return def; }
        @Override public void setGroupInfoDouble(String world, String group, String node, double value) {}
        @Override public boolean getPlayerInfoBoolean(String world, String name, String node, boolean def) { return def; }
        @Override public void setPlayerInfoBoolean(String world, String name, String node, boolean value) {}
        @Override public boolean getGroupInfoBoolean(String world, String group, String node, boolean def) { return def; }
        @Override public void setGroupInfoBoolean(String world, String group, String node, boolean value) {}
        @Override public String getPlayerInfoString(String world, String name, String node, String def) { return def; }
        @Override public void setPlayerInfoString(String world, String name, String node, String value) {}
        @Override public String getGroupInfoString(String world, String group, String node, String def) { return def; }
        @Override public void setGroupInfoString(String world, String group, String node, String value) {}
    }
}

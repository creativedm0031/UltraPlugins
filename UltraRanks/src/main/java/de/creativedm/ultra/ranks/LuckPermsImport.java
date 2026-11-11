package de.creativedm.ultra.ranks;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.command.CommandSender;

/** Copies groups (weight, prefix, suffix, permissions, parents) and players' primary groups from LuckPerms. */
final class LuckPermsImport {

    private LuckPermsImport() {}

    static void run(UltraRanks plugin, CommandSender sender) {
        LuckPerms lp = LuckPermsProvider.get();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                lp.getGroupManager().loadAllGroups().join();
                Map<UUID, String[]> users = new ConcurrentHashMap<>();
                for (UUID id : lp.getUserManager().getUniqueUsers().join()) {
                    User user = lp.getUserManager().loadUser(id).join();
                    users.put(id, new String[] {user.getUsername() == null ? id.toString().substring(0, 8) : user.getUsername(), user.getPrimaryGroup()});
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> apply(plugin, sender, lp, users));
            } catch (RuntimeException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.msg().error(sender, "import-failed", "error", String.valueOf(e.getMessage())));
            }
        });
    }

    private static String mini(String legacy) {
        if (legacy == null || legacy.isEmpty()) return "";
        LegacyComponentSerializer serializer = legacy.indexOf('§') >= 0 ? LegacyComponentSerializer.legacySection() : LegacyComponentSerializer.legacyAmpersand();
        return MiniMessage.miniMessage().serialize(serializer.deserialize(legacy));
    }

    private static void apply(UltraRanks plugin, CommandSender sender, LuckPerms lp, Map<UUID, String[]> users) {
        RankManager ranks = plugin.ranks();
        int groups = 0;
        for (Group group : lp.getGroupManager().getLoadedGroups()) {
            String id = group.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
            Rank rank = ranks.rank(id) != null ? ranks.rank(id) : ranks.create(id);
            rank.weight = group.getWeight().orElse(rank.weight);
            if (group.getDisplayName() != null) rank.display = mini(group.getDisplayName());
            var meta = group.getCachedData().getMetaData();
            if (meta.getPrefix() != null) rank.prefix = mini(meta.getPrefix());
            if (meta.getSuffix() != null) rank.suffix = mini(meta.getSuffix());
            rank.parents.clear();
            for (InheritanceNode node : group.getNodes(NodeType.INHERITANCE)) {
                if (node.getContexts().isEmpty()) rank.parents.add(node.getGroupName().toLowerCase(Locale.ROOT));
            }
            for (PermissionNode node : group.getNodes(NodeType.PERMISSION)) {
                if (node.getContexts().isEmpty() && !node.hasExpiry()) rank.permissions.put(node.getPermission(), node.getValue());
            }
            groups++;
        }
        int players = 0;
        for (Map.Entry<UUID, String[]> user : users.entrySet()) {
            Rank rank = ranks.rank(user.getValue()[1]);
            if (rank == null) continue;
            ranks.set(user.getKey(), user.getValue()[0], rank, 0);
            players++;
        }
        ranks.save();
        ranks.refreshAll();
        ranks.log(sender.getName(), "import", String.valueOf(groups), String.valueOf(players));
        plugin.msg().info(sender, "import-done", "groups", groups, "players", players);
    }
}

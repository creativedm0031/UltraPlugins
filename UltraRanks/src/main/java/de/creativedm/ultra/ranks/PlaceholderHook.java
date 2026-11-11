package de.creativedm.ultra.ranks;

import de.creativedm.ultra.core.Durations;
import de.creativedm.ultra.core.Messages;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * %ultraranks_rank%, %ultraranks_rank_name%, %ultraranks_prefix%, %ultraranks_suffix%,
 * %ultraranks_weight%, %ultraranks_expires%. Only loaded when PlaceholderAPI is installed.
 */
final class PlaceholderHook extends PlaceholderExpansion {

    private final UltraRanks plugin;

    PlaceholderHook(UltraRanks plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "ultraranks"; }
    @Override public String getAuthor() { return "creativedm"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        RankManager.Assignment a = plugin.ranks().assignment(player.getUniqueId());
        Rank rank = plugin.ranks().rank(a.rank());
        if (rank == null) return "";
        return switch (params.toLowerCase()) {
            case "rank" -> rank.id;
            case "rank_name" -> Messages.legacy(plugin.msg().parse(rank.display));
            case "prefix" -> Messages.legacy(plugin.msg().parse(rank.prefix));
            case "suffix" -> Messages.legacy(plugin.msg().parse(rank.suffix));
            case "weight" -> String.valueOf(rank.weight);
            case "expires" -> a.until() == 0 ? "-" : Durations.format(a.until() - System.currentTimeMillis());
            default -> null;
        };
    }
}

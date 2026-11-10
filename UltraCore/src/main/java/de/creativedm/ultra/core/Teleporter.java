package de.creativedm.ultra.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Teleports with the shared warm-up and cooldown behaviour.
 * Reads "warmup" and "cooldown" (seconds) from config.yml; "cooldowns.KEY" overrides the cooldown of one teleport type.
 */
public final class Teleporter implements Listener {

    private final UltraPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    Teleporter(UltraPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * @param cooldownKey separates cooldowns inside one plugin (e.g. "rtp"), null for no cooldown
     * @param arrived     runs after the player arrived; send the success message here with msg().status(...)
     */
    public void teleport(Player player, Location target, String cooldownKey, Runnable arrived) {
        if (target == null || target.getWorld() == null) {
            plugin.msg().error(player, "world-missing");
            return;
        }
        String key = player.getUniqueId() + ":" + cooldownKey;
        long left = cooldownKey == null ? 0 : cooldowns.getOrDefault(key, 0L) - System.currentTimeMillis();
        if (left > 0 && !player.hasPermission(plugin.id() + ".bypass.cooldown")) {
            plugin.msg().error(player, "cooldown", "time", Durations.format(left));
            return;
        }
        int warmup = player.hasPermission(plugin.id() + ".bypass.warmup") ? 0 : plugin.getConfig().getInt("warmup", 0);
        cancel(player);
        if (warmup <= 0) {
            go(player, target, key, cooldownKey, arrived);
            return;
        }
        int[] seconds = {warmup};
        pending.put(player.getUniqueId(), plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (seconds[0] <= 0) {
                cancel(player);
                go(player, target, key, cooldownKey, arrived);
                return;
            }
            plugin.msg().status(player, "teleport-warmup", "time", seconds[0] + "s");
            plugin.msg().sound(player, "tick");
            seconds[0]--;
        }, 0L, 20L));
    }

    private void go(Player player, Location target, String key, String cooldownKey, Runnable arrived) {
        {
            if (!player.teleport(target)) return;
            int cooldown = plugin.getConfig().getInt("cooldowns." + cooldownKey, plugin.getConfig().getInt("cooldown", 0));
            if (cooldownKey != null && cooldown > 0) cooldowns.put(key, System.currentTimeMillis() + cooldown * 1000L);
            plugin.msg().sound(player, "teleport");
            if (arrived != null) arrived.run();
        }
    }

    private boolean cancel(Player player) {
        BukkitTask task = pending.remove(player.getUniqueId());
        if (task != null) task.cancel();
        return task != null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (pending.isEmpty() || event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        if (cancel(event.getPlayer())) plugin.msg().error(event.getPlayer(), "teleport-cancelled");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && cancel(player)) plugin.msg().error(player, "teleport-cancelled");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }
}

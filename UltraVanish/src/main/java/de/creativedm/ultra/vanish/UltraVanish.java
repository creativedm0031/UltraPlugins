package de.creativedm.ultra.vanish;

import de.creativedm.ultra.core.UltraPlugin;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

public final class UltraVanish extends UltraPlugin implements Listener {

    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    /** Copies of containers opened silently, written back when closed. */
    private final java.util.Map<Inventory, org.bukkit.block.Block> silent = new java.util.HashMap<>();
    private BukkitTask statusTask;

    @Override
    public String module() {
        return "Vanish";
    }

    @Override
    protected void enable() {
        for (String id : getConfig().getStringList("vanished")) vanished.add(UUID.fromString(id));
        getServer().getPluginManager().registerEvents(this, this);
        command("vanish", "ultravanish.vanish", false, this::vanishCommand, (s, a) -> a.length == 1 ? null : List.of());
        command("vanishlist", "ultravanish.list", false, (s, a) -> list(s), (s, a) -> List.of());
        command("fakejoin", "ultravanish.fake", true, (s, a) -> fake((Player) s, true), (s, a) -> List.of());
        command("fakequit", "ultravanish.fake", true, (s, a) -> fake((Player) s, false), (s, a) -> List.of());
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
        startStatus();
    }

    @Override
    protected void reloaded() {
        startStatus();
    }

    @Override
    protected void disable() {
        for (Player player : Bukkit.getOnlinePlayers()) for (Player other : Bukkit.getOnlinePlayers()) other.showPlayer(this, player);
        save();
    }

    private void save() {
        List<String> ids = new ArrayList<>();
        for (UUID id : vanished) ids.add(id.toString());
        getConfig().set("vanished", ids);
        saveConfig();
    }

    /** The permanent status line in the action bar while vanished. */
    private void startStatus() {
        if (statusTask != null) statusTask.cancel();
        statusTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (UUID id : vanished) {
                Player player = Bukkit.getPlayer(id);
                if (player == null) continue;
                long hiddenFrom = Bukkit.getOnlinePlayers().stream().filter(other -> other != player && !canSee(other, player)).count();
                msg().status(player, "vanish-status", "count", hiddenFrom);
            }
        }, 20L, 40L);
    }

    // ---- visibility -----------------------------------------------------------------------------------------

    private int level(Player player) {
        return permissionNumber(player, "ultravanish.level.", 1);
    }

    /** Players with ultravanish.see see vanished players of the same or a lower level. */
    private boolean canSee(Player viewer, Player target) {
        if (!vanished.contains(target.getUniqueId()) || viewer == target) return true;
        return viewer.hasPermission("ultravanish.see") && level(viewer) >= level(target);
    }

    private void refresh(Player target) {
        boolean hidden = vanished.contains(target.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer == target) continue;
            if (canSee(viewer, target)) viewer.showPlayer(this, target);
            else viewer.hidePlayer(this, target);
            // the vanished player also has to see or not see others correctly
            if (canSee(target, viewer)) target.showPlayer(this, viewer);
            else target.hidePlayer(this, viewer);
        }
        target.setMetadata("vanished", new FixedMetadataValue(this, hidden));
        target.setSleepingIgnored(hidden);
        target.setCollidable(!hidden);
        target.setSilent(hidden);
        target.setInvisible(false);
    }

    private void setVanished(Player player, boolean on) {
        if (on) vanished.add(player.getUniqueId());
        else vanished.remove(player.getUniqueId());
        refresh(player);
        save();
        if (getConfig().getBoolean("fake-messages-on-toggle")) broadcast(player, !on);
        if (on) msg().info(player, "vanish-on");
        else msg().info(player, "vanish-off");
    }

    private void vanishCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("ultravanish.vanish.others")) {
                msg().error(sender, "no-permission");
                return;
            }
            Player target = player(sender, args[0]);
            if (target == null) return;
            if (sender instanceof Player player && target != player && level(target) > level(player)) {
                msg().error(sender, "target-higher-level", "player", target.getName());
                return;
            }
            boolean on = !vanished.contains(target.getUniqueId());
            setVanished(target, on);
            if (target != sender) msg().info(sender, on ? "vanish-other-on" : "vanish-other-off", "player", target.getName());
            return;
        }
        if (!(sender instanceof Player player)) {
            usage(sender, "vanish");
            return;
        }
        setVanished(player, !vanished.contains(player.getUniqueId()));
    }

    private void list(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (UUID id : vanished) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && (!(sender instanceof Player viewer) || canSee(viewer, player))) names.add(player.getName());
        }
        if (names.isEmpty()) msg().info(sender, "list-empty");
        else msg().info(sender, "list", "count", names.size(), "players", String.join(", ", names));
    }

    private void fake(Player player, boolean join) {
        broadcast(player, join);
        msg().info(player, join ? "fake-join-sent" : "fake-quit-sent");
    }

    /** Vanilla style join/quit line, translated by the client itself. */
    private void broadcast(Player player, boolean join) {
        Component line = Component.translatable(join ? "multiplayer.player.joined" : "multiplayer.player.left",
                net.kyori.adventure.text.format.NamedTextColor.YELLOW, player.displayName());
        Bukkit.getServer().sendMessage(line);
    }

    // ---- events ---------------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!vanished.contains(player.getUniqueId()) && getConfig().getBoolean("vanish-on-join") && player.hasPermission("ultravanish.vanish")) {
            vanished.add(player.getUniqueId());
        }
        if (vanished.contains(player.getUniqueId())) {
            event.joinMessage(null);
            msg().status(player, "vanish-join");
        }
        refresh(player);
        // hide every vanished player from the one who just joined
        for (UUID id : vanished) {
            Player other = Bukkit.getPlayer(id);
            if (other != null && other != player && !canSee(player, other)) player.hidePlayer(this, other);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        if (vanished.contains(event.getPlayer().getUniqueId())) event.quitMessage(null);
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        if (!getConfig().getBoolean("hide-from-player-count")) return;
        event.getPlayerSample().removeIf(profile -> profile.getId() != null && vanished.contains(profile.getId()));
        long hidden = Bukkit.getOnlinePlayers().stream().filter(p -> vanished.contains(p.getUniqueId())).count();
        event.setNumPlayers((int) Math.max(0, event.getNumPlayers() - hidden));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player && vanished.contains(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && vanished.contains(player.getUniqueId())
                && !getConfig().getBoolean("allow-item-pickup")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!vanished.contains(player.getUniqueId())) return;
        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true); // pressure plates, tripwires, farmland
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || player.isSneaking() || !getConfig().getBoolean("silent-chests")) return;
        if (!(event.getClickedBlock().getState() instanceof Container container)) return;
        // Open a copy: no animation, no sound, nobody notices. Changes are written back on close.
        event.setCancelled(true);
        Inventory real = container.getInventory();
        int size = Math.max(9, (real.getSize() + 8) / 9 * 9);
        Inventory copy = Bukkit.createInventory(null, Math.min(54, size), msg().title(player, "silent-chest-title"));
        for (int i = 0; i < Math.min(real.getSize(), copy.getSize()); i++) {
            org.bukkit.inventory.ItemStack item = real.getItem(i);
            copy.setItem(i, item == null ? null : item.clone());
        }
        silent.put(copy, event.getClickedBlock());
        player.openInventory(copy);
        msg().status(player, "silent-chest");
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        org.bukkit.block.Block block = silent.remove(event.getInventory());
        if (block == null || !(block.getState() instanceof Container container)) return;
        Inventory real = container.getInventory();
        for (int i = 0; i < Math.min(real.getSize(), event.getInventory().getSize()); i++) real.setItem(i, event.getInventory().getItem(i));
    }

}

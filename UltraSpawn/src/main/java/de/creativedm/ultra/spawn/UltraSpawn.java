package de.creativedm.ultra.spawn;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.UltraPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class UltraSpawn extends UltraPlugin implements Listener {

    private static final String DEFAULT = "default";
    private static final String FIRST_JOIN = "firstjoin";

    private DataFile data;

    @Override
    public String module() {
        return "Spawn";
    }

    @Override
    protected void enable() {
        data = new DataFile(this, "spawns.yml");
        getServer().getPluginManager().registerEvents(this, this);
        command("spawn", "ultraspawn.spawn", false, this::spawn, (sender, args) -> args.length == 1 ? null : List.of());
        command("setspawn", "ultraspawn.set", true, this::setSpawn, (sender, args) -> args.length == 1 ? names(true) : List.of());
        command("delspawn", "ultraspawn.delete", false, this::delSpawn, (sender, args) -> args.length == 1 ? names(false) : List.of());
    }

    private List<String> names(boolean suggestions) {
        List<String> names = new ArrayList<>(data.yaml().getKeys(false));
        if (suggestions) {
            if (!names.contains(DEFAULT)) names.add(DEFAULT);
            if (!names.contains(FIRST_JOIN)) names.add(FIRST_JOIN);
        }
        return names;
    }

    /**
     * The spawn of a player: a rank spawn they have ultraspawn.group.&lt;name&gt; for,
     * then the spawn named like their world (if enabled), then "default".
     */
    private Location resolve(Player player) {
        for (String name : data.yaml().getKeys(false)) {
            String permission = "ultraspawn.group." + name;
            if (!name.equals(DEFAULT) && !name.equals(FIRST_JOIN) && player.isPermissionSet(permission) && player.hasPermission(permission)) {
                Location location = data.getLocation(name);
                if (location != null) return location;
            }
        }
        if (getConfig().getBoolean("per-world-spawns")) {
            Location location = data.getLocation(player.getWorld().getName().toLowerCase(Locale.ROOT));
            if (location != null) return location;
        }
        return data.getLocation(DEFAULT);
    }

    private void spawn(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("ultraspawn.spawn.others")) {
                msg().error(sender, "no-permission");
                return;
            }
            Player target = player(sender, args[0]);
            if (target == null) return;
            Location location = resolve(target);
            if (location == null) {
                msg().error(sender, "spawn-not-set");
                return;
            }
            target.teleport(location);
            msg().sound(target, "teleport");
            msg().status(target, "spawn-teleport");
            msg().info(sender, "spawn-other", "player", target.getName());
            return;
        }
        if (!(sender instanceof Player player)) {
            usage(sender, "spawn");
            return;
        }
        Location location = resolve(player);
        if (location == null) {
            msg().error(player, "spawn-not-set");
            return;
        }
        teleporter().teleport(player, location, "spawn", () -> msg().status(player, "spawn-teleport"));
    }

    private void setSpawn(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String name = args.length == 0 ? DEFAULT : args[0].toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]{1,32}")) {
            msg().error(sender, "spawn-invalid-name");
            return;
        }
        data.setLocation(name, player.getLocation());
        data.save();
        msg().info(sender, "spawn-set", "name", name);
    }

    private void delSpawn(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "delspawn");
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!data.yaml().contains(name)) {
            msg().error(sender, "spawn-unknown", "name", name);
            return;
        }
        data.yaml().set(name, null);
        data.save();
        msg().info(sender, "spawn-deleted", "name", name);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location location = null;
        if (!player.hasPlayedBefore()) {
            location = data.getLocation(FIRST_JOIN);
            if (location == null) location = resolve(player);
        } else if (getConfig().getBoolean("teleport-on-join")) {
            location = resolve(player);
        }
        if (location != null) player.teleport(location);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!getConfig().getBoolean("teleport-on-respawn")) return;
        if (getConfig().getBoolean("respect-bed-spawn") && (event.isBedSpawn() || event.isAnchorSpawn())) return;
        Location location = resolve(event.getPlayer());
        if (location != null) event.setRespawnLocation(location);
    }

    private boolean voidRescue(Player player) {
        if (!getConfig().getBoolean("void-rescue.enabled")) return false;
        List<String> worlds = getConfig().getStringList("void-rescue.worlds");
        return worlds.isEmpty() || worlds.contains(player.getWorld().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo().getY() >= player.getWorld().getMinHeight() - getConfig().getInt("void-rescue.depth", 5)) return;
        if (!voidRescue(player)) return;
        Location location = resolve(player);
        if (location == null) return;
        player.setFallDistance(0);
        player.teleport(location);
        msg().sound(player, "teleport");
        msg().status(player, "void-rescued");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID && event.getEntity() instanceof Player player
                && voidRescue(player) && resolve(player) != null) {
            event.setCancelled(true);
        }
    }
}

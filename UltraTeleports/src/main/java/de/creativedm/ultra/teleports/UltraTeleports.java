package de.creativedm.ultra.teleports;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Eco;
import de.creativedm.ultra.core.UltraPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class UltraTeleports extends UltraPlugin implements Listener {

    /** A pending request: "from" asked "to". here = "to" should come to "from". */
    private record Request(UUID from, boolean here, long expires) {}

    private final Map<UUID, Request> requests = new HashMap<>();
    private final Map<UUID, Location> back = new HashMap<>();
    private DataFile data;

    @Override
    public String module() {
        return "Teleports";
    }

    @Override
    protected void enable() {
        data = new DataFile(this, "players.yml");
        getServer().getPluginManager().registerEvents(this, this);
        Completer players = (sender, args) -> args.length == 1 ? null : List.of();
        Completer none = (sender, args) -> List.of();
        command("tpa", "ultrateleports.tpa", true, (s, a) -> request((Player) s, a, false), players);
        command("tpahere", "ultrateleports.tpahere", true, (s, a) -> request((Player) s, a, true), players);
        command("tpaccept", "ultrateleports.tpa", true, (s, a) -> answer((Player) s, true), none);
        command("tpdeny", "ultrateleports.tpa", true, (s, a) -> answer((Player) s, false), none);
        command("back", "ultrateleports.back", true, (s, a) -> back((Player) s), none);
        command("rtp", "ultrateleports.rtp", true, (s, a) -> randomTeleport((Player) s, a),
                (s, a) -> a.length == 1 ? getConfig().getStringList("rtp.worlds") : List.of());
        command("tptoggle", "ultrateleports.toggle", true, (s, a) -> toggle((Player) s), none);
        command("tpignore", "ultrateleports.toggle", true, (s, a) -> ignore((Player) s, a), players);
        command("tp", "ultrateleports.staff", false, this::staffTeleport, (s, a) -> a.length <= 2 ? null : List.of());
        command("tphere", "ultrateleports.staff", true, (s, a) -> staffHere((Player) s, a), players);
        command("tpall", "ultrateleports.staff", true, (s, a) -> staffAll((Player) s), none);
    }

    // ---- requests -------------------------------------------------------------------------------------------

    private void request(Player sender, String[] args, boolean here) {
        if (args.length == 0) {
            usage(sender, here ? "tpahere" : "tpa");
            return;
        }
        Player target = player(sender, args[0]);
        if (target == null) return;
        if (target.equals(sender)) {
            msg().error(sender, "request-self");
            return;
        }
        if (data.yaml().getBoolean("disabled." + target.getUniqueId())
                || data.yaml().getStringList("ignored." + target.getUniqueId()).contains(sender.getUniqueId().toString())) {
            msg().error(sender, "requests-disabled", "player", target.getName());
            return;
        }
        int timeout = getConfig().getInt("request-timeout", 60);
        requests.put(target.getUniqueId(), new Request(sender.getUniqueId(), here, System.currentTimeMillis() + timeout * 1000L));
        msg().info(sender, "request-sent", "player", target.getName());
        // The action bar is not clickable, so the request itself is one line in the chat.
        msg().chat(target, here ? "request-received-here" : "request-received", "player", sender.getName(), "time", timeout + "s");
        msg().sound(target, "success");
    }

    private void answer(Player player, boolean accept) {
        Request request = requests.remove(player.getUniqueId());
        Player requester = request == null ? null : Bukkit.getPlayer(request.from());
        if (request == null || requester == null || request.expires() < System.currentTimeMillis()) {
            msg().error(player, "request-none");
            return;
        }
        if (!accept) {
            msg().info(player, "request-denied", "player", requester.getName());
            msg().error(requester, "request-denied-sender", "player", player.getName());
            return;
        }
        msg().info(player, "request-accepted", "player", requester.getName());
        Player mover = request.here() ? player : requester;
        Player destination = request.here() ? requester : player;
        if (mover != player) msg().info(mover, "request-accepted-sender", "player", player.getName());
        teleporter().teleport(mover, destination.getLocation(), "tpa",
                () -> msg().status(mover, "teleported-to", "player", destination.getName()));
    }

    private void toggle(Player player) {
        String path = "disabled." + player.getUniqueId();
        boolean disabled = !data.yaml().getBoolean(path);
        data.yaml().set(path, disabled ? true : null);
        data.save();
        msg().info(player, disabled ? "toggle-off" : "toggle-on");
    }

    private void ignore(Player player, String[] args) {
        if (args.length == 0) {
            usage(player, "tpignore");
            return;
        }
        Player target = player(player, args[0]);
        if (target == null) return;
        String path = "ignored." + player.getUniqueId();
        List<String> ignored = new ArrayList<>(data.yaml().getStringList(path));
        String id = target.getUniqueId().toString();
        boolean now = !ignored.remove(id);
        if (now) ignored.add(id);
        data.yaml().set(path, ignored.isEmpty() ? null : ignored);
        data.save();
        msg().info(player, now ? "ignore-on" : "ignore-off", "player", target.getName());
    }

    // ---- back -----------------------------------------------------------------------------------------------

    private void back(Player player) {
        Location location = back.get(player.getUniqueId());
        if (location == null) {
            msg().error(player, "back-none");
            return;
        }
        teleporter().teleport(player, location, "back", () -> msg().status(player, "back-teleport"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.COMMAND || cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            back.put(event.getPlayer().getUniqueId(), event.getFrom());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (getConfig().getBoolean("back-on-death", true)) back.put(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        requests.remove(event.getPlayer().getUniqueId());
        back.remove(event.getPlayer().getUniqueId());
    }

    // ---- random teleport ------------------------------------------------------------------------------------

    private void randomTeleport(Player player, String[] args) {
        World world = args.length > 0 ? Bukkit.getWorld(args[0]) : player.getWorld();
        if (world == null || !getConfig().getStringList("rtp.worlds").contains(world.getName())) {
            msg().error(player, "rtp-world-disabled");
            return;
        }
        double cost = getConfig().getDouble("rtp.cost");
        if (!Eco.has(player, cost)) {
            msg().error(player, "not-enough-money", "amount", Eco.format(cost));
            return;
        }
        msg().status(player, "rtp-searching");
        search(player, world, getConfig().getInt("rtp.max-attempts", 12), cost);
    }

    private void search(Player player, World world, int attemptsLeft, double cost) {
        if (!player.isOnline()) return;
        if (attemptsLeft <= 0) {
            msg().error(player, "rtp-failed");
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int min = getConfig().getInt("rtp.min-radius", 200);
        int max = Math.max(min + 1, getConfig().getInt("rtp.max-radius", 3000));
        double angle = random.nextDouble(Math.PI * 2);
        double distance = random.nextDouble(min, max);
        Location center = world.getWorldBorder().getCenter();
        int x = (int) Math.floor(center.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(center.getZ() + Math.sin(angle) * distance);
        // One attempt per tick, so loading new chunks never freezes the server for long.
        getServer().getScheduler().runTask(this, () -> {
            world.getChunkAt(x >> 4, z >> 4);
            Location location = safeLocation(world, x, z);
            if (location == null || !world.getWorldBorder().isInside(location)) {
                search(player, world, attemptsLeft - 1, cost);
                return;
            }
            teleporter().teleport(player, location, "rtp", () -> {
                Eco.charge(player, cost);
                msg().status(player, "rtp-teleport", "x", x, "z", z);
            });
        });
    }

    /** Standing position on solid, harmless ground with two free blocks above, or null. */
    private Location safeLocation(World world, int x, int z) {
        int y;
        if (world.getEnvironment() == World.Environment.NETHER) {
            y = -1;
            for (int candidate = 110; candidate > 32; candidate--) {
                if (world.getBlockAt(x, candidate, z).getType().isSolid() && world.getBlockAt(x, candidate + 1, z).getType().isAir()
                        && world.getBlockAt(x, candidate + 2, z).getType().isAir()) {
                    y = candidate;
                    break;
                }
            }
            if (y < 0) return null;
        } else {
            y = world.getHighestBlockYAt(x, z);
        }
        Block ground = world.getBlockAt(x, y, z);
        Material type = ground.getType();
        if (!type.isSolid() || type == Material.MAGMA_BLOCK || type == Material.CACTUS || type == Material.BEDROCK
                || type == Material.POWDER_SNOW || ground.getRelative(0, 1, 0).isLiquid()) {
            return null;
        }
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }

    // ---- staff ----------------------------------------------------------------------------------------------

    private void staffTeleport(CommandSender sender, String[] args) {
        if (args.length == 0 || (args.length == 1 && !(sender instanceof Player))) {
            usage(sender, "tp");
            return;
        }
        Player mover = args.length == 1 ? (Player) sender : player(sender, args[0]);
        Player target = mover == null ? null : player(sender, args[args.length == 1 ? 0 : 1]);
        if (mover == null || target == null) return;
        mover.teleport(target.getLocation());
        if (mover != sender) msg().status(mover, "teleported-to", "player", target.getName());
        msg().info(sender, "staff-teleported", "player", mover.getName(), "target", target.getName());
    }

    private void staffHere(Player sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "tphere");
            return;
        }
        Player target = player(sender, args[0]);
        if (target == null) return;
        target.teleport(sender.getLocation());
        msg().status(target, "teleported-to", "player", sender.getName());
        msg().info(sender, "staff-teleported", "player", target.getName(), "target", sender.getName());
    }

    private void staffAll(Player sender) {
        int count = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(sender)) continue;
            target.teleport(sender.getLocation());
            msg().status(target, "teleported-to", "player", sender.getName());
            count++;
        }
        msg().info(sender, "tpall-done", "count", count);
    }
}

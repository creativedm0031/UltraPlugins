package de.creativedm.ultra.homes;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Menu;
import de.creativedm.ultra.core.UltraPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class UltraHomes extends UltraPlugin {

    private DataFile data;

    @Override
    public String module() {
        return "Homes";
    }

    @Override
    protected void enable() {
        data = new DataFile(this, "homes.yml");
        Completer own = (sender, args) -> args.length == 1 && sender instanceof Player p ? names(p.getUniqueId()) : List.of();
        command("home", "ultrahomes.home", true, (s, a) -> home((Player) s, a), own);
        command("homes", "ultrahomes.home", true, (s, a) -> openList((Player) s, 0), (s, a) -> List.of());
        command("sethome", "ultrahomes.set", true, (s, a) -> setHome((Player) s, a), own);
        command("delhome", "ultrahomes.delete", true, (s, a) -> delHome((Player) s, a), own);
    }

    @Override
    protected List<String> adminSubcommands() {
        return List.of("import");
    }

    @Override
    protected boolean adminCommand(CommandSender sender, String[] args) {
        if (!args[0].equalsIgnoreCase("import")) return false;
        importEssentials(sender);
        return true;
    }

    private List<String> names(UUID owner) {
        ConfigurationSection section = data.yaml().getConfigurationSection(owner.toString());
        return section == null ? new ArrayList<>() : new ArrayList<>(section.getKeys(false));
    }

    private int limit(Player player) {
        return permissionNumber(player, "ultrahomes.limit.", getConfig().getInt("default-limit", 3));
    }

    // ---- commands -------------------------------------------------------------------------------------------

    private void home(Player player, String[] args) {
        if (args.length == 0) {
            List<String> names = names(player.getUniqueId());
            if (names.size() == 1) teleport(player, player.getUniqueId(), names.get(0));
            else openList(player, 0);
            return;
        }
        String arg = args[0].toLowerCase(Locale.ROOT);
        if (!arg.contains(":")) {
            teleport(player, player.getUniqueId(), arg);
            return;
        }
        // "player:home" visits a public home (or any home with ultrahomes.others)
        String ownerName = args[0].substring(0, args[0].indexOf(':'));
        String name = arg.substring(arg.indexOf(':') + 1);
        OfflinePlayer owner = Bukkit.getPlayerExact(ownerName);
        if (owner == null) {
            for (OfflinePlayer known : Bukkit.getOfflinePlayers()) {
                if (ownerName.equalsIgnoreCase(known.getName())) {
                    owner = known;
                    break;
                }
            }
        }
        String path = owner == null ? null : owner.getUniqueId() + "." + name;
        if (path == null || !data.yaml().contains(path)) {
            msg().error(player, "home-unknown", "name", args[0]);
        } else if (!data.yaml().getBoolean(path + ".public") && !player.hasPermission("ultrahomes.others")) {
            msg().error(player, "home-not-public", "name", args[0]);
        } else {
            teleport(player, owner.getUniqueId(), name);
        }
    }

    private void teleport(Player player, UUID owner, String name) {
        String path = owner + "." + name;
        if (!data.yaml().contains(path)) {
            msg().error(player, "home-unknown", "name", name);
            return;
        }
        teleporter().teleport(player, data.getLocation(path), "home", () -> msg().status(player, "home-teleport", "name", name));
    }

    private void setHome(Player player, String[] args) {
        String name = args.length == 0 ? "home" : args[0].toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]{1,24}")) {
            msg().error(player, "home-invalid-name");
            return;
        }
        if (getConfig().getStringList("blocked-worlds").contains(player.getWorld().getName())) {
            msg().error(player, "world-blocked");
            return;
        }
        String path = player.getUniqueId() + "." + name;
        int limit = limit(player);
        if (!data.yaml().contains(path) && names(player.getUniqueId()).size() >= limit) {
            msg().error(player, "limit-reached", "limit", limit);
            return;
        }
        data.setLocation(path, player.getLocation());
        data.save();
        msg().info(player, "home-set", "name", name);
    }

    private void delHome(Player player, String[] args) {
        if (args.length == 0) {
            usage(player, "delhome");
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        String path = player.getUniqueId() + "." + name;
        if (!data.yaml().contains(path)) {
            msg().error(player, "home-unknown", "name", name);
            return;
        }
        data.yaml().set(path, null);
        data.save();
        msg().info(player, "home-deleted", "name", name);
    }

    // ---- GUI ------------------------------------------------------------------------------------------------

    private void openList(Player player, int page) {
        UUID id = player.getUniqueId();
        List<String> names = names(id);
        if (names.isEmpty()) {
            msg().error(player, "home-none");
            return;
        }
        List<Menu.Entry> entries = new ArrayList<>();
        for (String name : names) {
            String path = id + "." + name;
            Material icon = Menu.material(data.yaml().getString(path + ".icon"), Menu.material(getConfig().getString("default-icon"), Material.RED_BED));
            entries.add(new Menu.Entry(Menu.item(icon, msg().parse("<brand><name></brand>", "name", name),
                    msg().lore(player, "home-lore", "world", data.yaml().getString(path + ".world", "?"),
                            "x", (int) data.yaml().getDouble(path + ".x"), "y", (int) data.yaml().getDouble(path + ".y"),
                            "z", (int) data.yaml().getDouble(path + ".z"),
                            "public", msg().raw(player, data.yaml().getBoolean(path + ".public") ? "yes" : "no"))),
                    click -> {
                        if (click.isRightClick()) {
                            openEdit(player, name);
                        } else {
                            player.closeInventory();
                            teleport(player, id, name);
                        }
                    }));
        }
        Menu.paged(this, player, msg().title(player, "homes-title", "count", names.size(), "limit", limit(player)), entries, page,
                next -> openList(player, next), null).open(player);
    }

    private void openEdit(Player player, String name) {
        String path = player.getUniqueId() + "." + name;
        boolean open = data.yaml().getBoolean(path + ".public");
        new Menu(msg().title(player, "edit-title", "name", name), 3).frame()
                .set(10, Menu.item(Material.ITEM_FRAME, msg().text(player, "edit-icon"), msg().lore(player, "edit-icon-lore")), click -> {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        player.closeInventory();
                        msg().error(player, "icon-empty-hand");
                        return;
                    }
                    data.yaml().set(path + ".icon", hand.getType().name());
                    data.save();
                    msg().info(player, "icon-set", "name", name);
                    openList(player, 0);
                })
                .set(12, Menu.item(Material.COMPASS, msg().text(player, "edit-relocate"), msg().lore(player, "edit-relocate-lore")), click -> {
                    player.closeInventory();
                    if (getConfig().getStringList("blocked-worlds").contains(player.getWorld().getName())) {
                        msg().error(player, "world-blocked");
                        return;
                    }
                    data.setLocation(path, player.getLocation());
                    data.save();
                    msg().info(player, "home-set", "name", name);
                })
                .set(14, Menu.item(open ? Material.ENDER_EYE : Material.ENDER_PEARL, msg().text(player, "edit-public"),
                        msg().lore(player, "edit-public-lore", "public", msg().raw(player, open ? "yes" : "no"), "player", player.getName(), "name", name)), click -> {
                    data.yaml().set(path + ".public", open ? null : true);
                    data.save();
                    msg().info(player, open ? "public-off" : "public-on", "name", name);
                    openEdit(player, name);
                })
                .set(16, Menu.item(Material.TNT, msg().text(player, "edit-delete"), List.of()), click ->
                        Menu.confirm(this, player, msg().title(player, "delete-title", "name", name), () -> {
                            player.closeInventory();
                            delHome(player, new String[] {name});
                        }, () -> openEdit(player, name)))
                .set(22, Menu.item(Material.ARROW, msg().text(player, "gui-back"), List.of()), click -> openList(player, 0))
                .open(player);
    }

    // ---- import ---------------------------------------------------------------------------------------------

    /** Copies homes from plugins/Essentials/userdata. Existing Ultra homes with the same name are kept. */
    private void importEssentials(CommandSender sender) {
        File[] files = new File(getDataFolder().getParentFile(), "Essentials/userdata").listFiles((dir, file) -> file.endsWith(".yml"));
        if (files == null) {
            msg().error(sender, "import-missing");
            return;
        }
        int count = 0;
        for (File file : files) {
            UUID owner;
            try {
                owner = UUID.fromString(file.getName().replace(".yml", ""));
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection homes = YamlConfiguration.loadConfiguration(file).getConfigurationSection("homes");
            if (homes == null) continue;
            for (String key : homes.getKeys(false)) {
                ConfigurationSection home = homes.getConfigurationSection(key);
                String name = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
                String path = owner + "." + name;
                if (home == null || data.yaml().contains(path)) continue;
                World world = Bukkit.getWorld(home.getString("world-name", home.getString("world", "")));
                if (world == null) {
                    try {
                        world = Bukkit.getWorld(UUID.fromString(home.getString("world", "")));
                    } catch (IllegalArgumentException ignored) {
                        // not a world id
                    }
                }
                if (world == null) continue;
                data.setLocation(path, new Location(world, home.getDouble("x"), home.getDouble("y"), home.getDouble("z"),
                        (float) home.getDouble("yaw"), (float) home.getDouble("pitch")));
                count++;
            }
        }
        data.save();
        msg().info(sender, "import-done", "count", count);
    }
}

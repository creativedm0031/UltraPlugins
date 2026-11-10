package de.creativedm.ultra.warps;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Eco;
import de.creativedm.ultra.core.Menu;
import de.creativedm.ultra.core.UltraPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class UltraWarps extends UltraPlugin {

    private static final List<String> SORTS = List.of("name", "visits", "newest");

    private DataFile data;

    @Override
    public String module() {
        return "Warps";
    }

    @Override
    protected void enable() {
        data = new DataFile(this, "warps.yml");
        Completer warps = (sender, args) -> args.length == 1 ? names() : List.of();
        command("warp", "ultrawarps.warp", true, (s, a) -> warp((Player) s, a), warps);
        command("warps", "ultrawarps.warp", true, (s, a) -> openList((Player) s, 0, null, "name"), (s, a) -> List.of());
        command("setwarp", "ultrawarps.set", true, (s, a) -> setWarp((Player) s, a), (s, a) -> a.length == 2 ? categories() : List.of());
        command("delwarp", "ultrawarps.delete", false, this::delWarp, warps);
        command("pwarp", "ultrawarps.player", true, (s, a) -> playerWarp((Player) s, a),
                (s, a) -> a.length == 1 ? List.of("create", "delete", "list") : a.length == 2 && a[0].equalsIgnoreCase("delete") ? own((Player) s) : List.of());
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

    private List<String> names() {
        return new ArrayList<>(data.yaml().getKeys(false));
    }

    private List<String> categories() {
        TreeSet<String> categories = new TreeSet<>();
        for (String name : names()) categories.add(data.yaml().getString(name + ".category", "general"));
        return new ArrayList<>(categories);
    }

    private List<String> own(Player player) {
        List<String> own = new ArrayList<>();
        for (String name : names()) if (player.getUniqueId().toString().equals(data.yaml().getString(name + ".owner"))) own.add(name);
        return own;
    }

    private boolean allowed(Player player, String name) {
        return !getConfig().getBoolean("per-warp-permission") || data.yaml().contains(name + ".owner")
                || player.hasPermission("ultrawarps.warp." + name) || player.hasPermission("ultrawarps.warp.*");
    }

    // ---- commands -------------------------------------------------------------------------------------------

    private void warp(Player player, String[] args) {
        if (args.length == 0) {
            openList(player, 0, null, "name");
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!data.yaml().contains(name)) {
            msg().error(player, "warp-unknown", "name", name);
        } else if (!allowed(player, name)) {
            msg().error(player, "no-permission");
        } else {
            teleporter().teleport(player, data.getLocation(name), "warp", () -> {
                data.yaml().set(name + ".visits", data.yaml().getInt(name + ".visits") + 1);
                data.save();
                msg().status(player, "warp-teleport", "name", name);
            });
        }
    }

    private boolean create(Player player, String name, String category, UUID owner) {
        if (!name.matches("[a-z0-9_-]{1,24}")) {
            msg().error(player, "warp-invalid-name");
            return false;
        }
        if (data.yaml().contains(name) && (owner != null || data.yaml().contains(name + ".owner"))) {
            msg().error(player, "warp-exists", "name", name);
            return false;
        }
        int visits = data.yaml().getInt(name + ".visits");
        data.yaml().set(name, null);
        data.setLocation(name, player.getLocation());
        data.yaml().set(name + ".category", category);
        data.yaml().set(name + ".created", System.currentTimeMillis());
        data.yaml().set(name + ".visits", visits);
        if (owner != null) data.yaml().set(name + ".owner", owner.toString());
        Material hand = player.getInventory().getItemInMainHand().getType();
        if (!hand.isAir()) data.yaml().set(name + ".icon", hand.name());
        data.save();
        return true;
    }

    private void setWarp(Player player, String[] args) {
        if (args.length == 0) {
            usage(player, "setwarp");
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        String category = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "general";
        if (create(player, name, category, null)) msg().info(player, "warp-created", "name", name);
    }

    private void delWarp(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "delwarp");
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!data.yaml().contains(name)) {
            msg().error(sender, "warp-unknown", "name", name);
            return;
        }
        data.yaml().set(name, null);
        data.save();
        msg().info(sender, "warp-deleted", "name", name);
    }

    private void playerWarp(Player player, String[] args) {
        if (!getConfig().getBoolean("player-warps.enabled")) {
            msg().error(player, "pwarp-disabled");
            return;
        }
        String category = getConfig().getString("player-warps.category", "player");
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            openList(player, 0, category, "visits");
            return;
        }
        if (args.length < 2 || !(sub.equals("create") || sub.equals("delete"))) {
            usage(player, "pwarp");
            return;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("delete")) {
            if (!player.getUniqueId().toString().equals(data.yaml().getString(name + ".owner"))) {
                msg().error(player, "pwarp-not-yours", "name", name);
                return;
            }
            delWarp(player, new String[] {name});
            return;
        }
        int limit = permissionNumber(player, "ultrawarps.player.limit.", getConfig().getInt("player-warps.default-limit", 1));
        if (own(player).size() >= limit) {
            msg().error(player, "pwarp-limit", "limit", limit);
            return;
        }
        double cost = getConfig().getDouble("player-warps.cost");
        if (!Eco.has(player, cost)) {
            msg().error(player, "not-enough-money", "amount", Eco.format(cost));
            return;
        }
        if (!create(player, name, category, player.getUniqueId())) return;
        Eco.charge(player, cost);
        msg().info(player, "warp-created", "name", name);
    }

    // ---- GUI ------------------------------------------------------------------------------------------------

    private void openList(Player player, int page, String category, String sort) {
        List<String> names = new ArrayList<>();
        for (String name : names()) {
            if (allowed(player, name) && (category == null || category.equals(data.yaml().getString(name + ".category", "general")))) names.add(name);
        }
        if (names.isEmpty() && category == null) {
            msg().error(player, "warps-none");
            return;
        }
        switch (sort) {
            case "visits" -> names.sort(Comparator.comparingInt((String n) -> data.yaml().getInt(n + ".visits")).reversed());
            case "newest" -> names.sort(Comparator.comparingLong((String n) -> data.yaml().getLong(n + ".created")).reversed());
            default -> names.sort(Comparator.naturalOrder());
        }
        List<Menu.Entry> entries = new ArrayList<>();
        for (String name : names) {
            String ownerId = data.yaml().getString(name + ".owner");
            String owner = ownerId == null ? "Server" : String.valueOf(Bukkit.getOfflinePlayer(UUID.fromString(ownerId)).getName());
            entries.add(new Menu.Entry(Menu.item(Menu.material(data.yaml().getString(name + ".icon"), ownerId == null ? Material.ENDER_PEARL : Material.OAK_SIGN),
                    msg().parse("<brand><name></brand>", "name", name),
                    msg().lore(player, "warp-lore", "category", data.yaml().getString(name + ".category", "general"),
                            "visits", data.yaml().getInt(name + ".visits"), "owner", owner)),
                    click -> {
                        player.closeInventory();
                        warp(player, new String[] {name});
                    }));
        }
        Menu menu = Menu.paged(this, player, msg().title(player, "warps-title"), entries, page, next -> openList(player, next, category, sort), null);
        List<String> categories = categories();
        menu.set(47, Menu.item(Material.HOPPER, msg().text(player, "filter-button"),
                msg().lore(player, "filter-lore", "category", category == null ? msg().raw(player, "all-categories") : category)), click -> {
            int index = category == null ? -1 : categories.indexOf(category);
            openList(player, 0, index + 1 >= categories.size() ? null : categories.get(index + 1), sort);
        });
        menu.set(51, Menu.item(Material.COMPARATOR, msg().text(player, "sort-button"),
                msg().lore(player, "sort-lore", "sort", msg().raw(player, "sort-" + sort))), click ->
                openList(player, 0, category, SORTS.get((SORTS.indexOf(sort) + 1) % SORTS.size())));
        menu.open(player);
    }

    // ---- import ---------------------------------------------------------------------------------------------

    /** Copies warps from plugins/Essentials/warps. Existing Ultra warps with the same name are kept. */
    private void importEssentials(CommandSender sender) {
        File[] files = new File(getDataFolder().getParentFile(), "Essentials/warps").listFiles((dir, file) -> file.endsWith(".yml"));
        if (files == null) {
            msg().error(sender, "import-missing");
            return;
        }
        int count = 0;
        for (File file : files) {
            YamlConfiguration warp = YamlConfiguration.loadConfiguration(file);
            String name = warp.getString("name", file.getName().replace(".yml", "")).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
            if (data.yaml().contains(name)) continue;
            World world = Bukkit.getWorld(warp.getString("world-name", warp.getString("world", "")));
            if (world == null) {
                try {
                    world = Bukkit.getWorld(UUID.fromString(warp.getString("world", "")));
                } catch (IllegalArgumentException ignored) {
                    // not a world id
                }
            }
            if (world == null) continue;
            data.setLocation(name, new Location(world, warp.getDouble("x"), warp.getDouble("y"), warp.getDouble("z"),
                    (float) warp.getDouble("yaw"), (float) warp.getDouble("pitch")));
            data.yaml().set(name + ".category", "general");
            data.yaml().set(name + ".created", System.currentTimeMillis());
            count++;
        }
        data.save();
        msg().info(sender, "import-done", "count", count);
    }
}

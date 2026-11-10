package de.creativedm.ultra.kits;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Durations;
import de.creativedm.ultra.core.Eco;
import de.creativedm.ultra.core.Menu;
import de.creativedm.ultra.core.UltraPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class UltraKits extends UltraPlugin implements Listener {

    /** Holder of the editor inventory; the content is saved when it is closed. */
    private static final class Editor implements InventoryHolder {
        final String kit;
        Inventory inventory;

        Editor(String kit) {
            this.kit = kit;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private DataFile kits;
    private DataFile usage;

    @Override
    public String module() {
        return "Kits";
    }

    @Override
    protected void enable() {
        kits = new DataFile(this, "kits.yml");
        usage = new DataFile(this, "usage.yml");
        getServer().getPluginManager().registerEvents(this, this);
        Completer names = (s, a) -> a.length == 1 ? names() : List.of();
        command("kit", "ultrakits.kit", false, this::kitCommand, (s, a) -> a.length == 1 ? names() : a.length == 2 ? null : List.of());
        command("kits", "ultrakits.kit", true, (s, a) -> openList((Player) s, 0), (s, a) -> List.of());
        command("createkit", "ultrakits.create", true, (s, a) -> create((Player) s, a), (s, a) -> List.of());
        command("editkit", "ultrakits.edit", true, (s, a) -> edit((Player) s, a), (s, a) -> a.length == 1 ? names() : a.length == 2
                ? List.of("items", "cooldown", "cost", "onetime", "icon", "firstjoin") : List.of());
        command("delkit", "ultrakits.delete", false, this::delete, names);
    }

    private List<String> names() {
        return new ArrayList<>(kits.yaml().getKeys(false));
    }

    private boolean allowed(Player player, String kit) {
        return player.hasPermission("ultrakits.kit." + kit) || player.hasPermission("ultrakits.kit.*")
                || !kits.yaml().getBoolean(kit + ".permission", true);
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> items(String kit) {
        List<ItemStack> items = new ArrayList<>();
        for (Object o : kits.yaml().getList(kit + ".items", List.of())) if (o instanceof ItemStack item) items.add(item.clone());
        return items;
    }

    /** Milliseconds until the kit can be claimed again: 0 = now, -1 = never (one-time kit). */
    private long remaining(Player player, String kit) {
        long last = usage.yaml().getLong(player.getUniqueId() + "." + kit);
        if (last == 0 || player.hasPermission("ultrakits.bypass.cooldown")) return 0;
        if (kits.yaml().getBoolean(kit + ".one-time")) return -1;
        long cooldown = kits.yaml().getLong(kit + ".cooldown") * 1000L;
        return Math.max(0, last + cooldown - System.currentTimeMillis());
    }

    // ---- claiming -------------------------------------------------------------------------------------------

    private void kitCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) openList(player, 0);
            else usage(sender, "kit");
            return;
        }
        String kit = args[0].toLowerCase(Locale.ROOT);
        if (!kits.yaml().contains(kit)) {
            msg().error(sender, "kit-unknown", "name", kit);
            return;
        }
        if (args.length > 1) {
            if (!sender.hasPermission("ultrakits.give")) {
                msg().error(sender, "no-permission");
                return;
            }
            Player target = player(sender, args[1]);
            if (target == null) return;
            give(target, kit);
            msg().status(target, "kit-received", "name", kit);
            msg().info(sender, "kit-given", "name", kit, "player", target.getName());
            return;
        }
        if (sender instanceof Player player) claim(player, kit);
        else usage(sender, "kit");
    }

    private void claim(Player player, String kit) {
        if (!allowed(player, kit)) {
            msg().error(player, "no-permission");
            return;
        }
        long left = remaining(player, kit);
        if (left < 0) {
            msg().error(player, "kit-one-time", "name", kit);
            return;
        }
        if (left > 0) {
            msg().error(player, "kit-cooldown", "name", kit, "time", Durations.format(left));
            return;
        }
        double cost = kits.yaml().getDouble(kit + ".cost");
        if (!Eco.charge(player, cost)) {
            msg().error(player, "not-enough-money", "amount", Eco.format(cost));
            return;
        }
        usage.yaml().set(player.getUniqueId() + "." + kit, System.currentTimeMillis());
        usage.save();
        give(player, kit);
        msg().info(player, "kit-received", "name", kit);
    }

    /** Armour goes straight into empty armour slots, the rest into the inventory; overflow is dropped. */
    private void give(Player player, String kit) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack item : items(kit)) {
            EquipmentSlot slot = item.getType().getEquipmentSlot();
            boolean armour = getConfig().getBoolean("auto-equip", true) && slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND;
            if (armour && (inventory.getItem(slot) == null || inventory.getItem(slot).getType().isAir())) inventory.setItem(slot, item);
            else rest.add(item);
        }
        Map<Integer, ItemStack> overflow = inventory.addItem(rest.toArray(new ItemStack[0]));
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        if (!overflow.isEmpty()) msg().error(player, "inventory-full");
        for (String command : kits.yaml().getStringList(kit + ".commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) return;
        for (String kit : names()) {
            if (!kits.yaml().getBoolean(kit + ".first-join")) continue;
            give(event.getPlayer(), kit);
            usage.yaml().set(event.getPlayer().getUniqueId() + "." + kit, System.currentTimeMillis());
        }
        usage.save();
    }

    // ---- GUIs -----------------------------------------------------------------------------------------------

    private void openList(Player player, int page) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (String kit : names()) {
            if (!allowed(player, kit) && getConfig().getBoolean("hide-locked-kits")) continue;
            long left = remaining(player, kit);
            String status = !allowed(player, kit) ? msg().raw(player, "status-locked") : left < 0 ? msg().raw(player, "status-claimed")
                    : left > 0 ? Durations.format(left) : msg().raw(player, "status-ready");
            double cost = kits.yaml().getDouble(kit + ".cost");
            entries.add(new Menu.Entry(Menu.item(Menu.material(kits.yaml().getString(kit + ".icon"), Material.CHEST),
                    msg().parse("<brand><name></brand>", "name", kit),
                    msg().lore(player, "kit-lore", "status", status, "items", items(kit).size(),
                            "cost", cost > 0 ? Eco.format(cost) : msg().raw(player, "free"))),
                    click -> {
                        if (click.isRightClick()) {
                            openPreview(player, kit);
                        } else {
                            player.closeInventory();
                            claim(player, kit);
                        }
                    }));
        }
        if (entries.isEmpty()) {
            msg().error(player, "kits-none");
            return;
        }
        Menu.paged(this, player, msg().title(player, "kits-title"), entries, page, next -> openList(player, next), null).open(player);
    }

    private void openPreview(Player player, String kit) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (ItemStack item : items(kit)) entries.add(new Menu.Entry(item, null));
        Menu menu = Menu.paged(this, player, msg().title(player, "preview-title", "name", kit), entries, 0, p -> openPreview(player, kit), () -> openList(player, 0));
        menu.set(53, Menu.item(Material.LIME_CONCRETE, msg().text(player, "preview-claim"), List.of()), click -> {
            player.closeInventory();
            claim(player, kit);
        });
        menu.open(player);
    }

    // ---- editor ---------------------------------------------------------------------------------------------

    private void create(Player player, String[] args) {
        if (args.length == 0) {
            usage(player, "createkit");
            return;
        }
        String kit = args[0].toLowerCase(Locale.ROOT);
        if (!kit.matches("[a-z0-9_-]{1,24}")) {
            msg().error(player, "kit-invalid-name");
            return;
        }
        if (kits.yaml().contains(kit)) {
            msg().error(player, "kit-exists", "name", kit);
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) if (item != null && !item.getType().isAir()) items.add(item.clone());
        if (items.isEmpty()) {
            msg().error(player, "kit-empty-inventory");
            return;
        }
        kits.yaml().set(kit + ".items", items);
        kits.yaml().set(kit + ".cooldown", getConfig().getLong("default-cooldown", 86400));
        kits.yaml().set(kit + ".cost", 0.0);
        kits.yaml().set(kit + ".one-time", false);
        kits.yaml().set(kit + ".first-join", false);
        kits.yaml().set(kit + ".permission", true);
        kits.yaml().set(kit + ".icon", items.get(0).getType().name());
        kits.yaml().set(kit + ".commands", List.of());
        kits.save();
        msg().info(player, "kit-created", "name", kit, "items", items.size());
    }

    private void edit(Player player, String[] args) {
        if (args.length < 2) {
            usage(player, "editkit");
            return;
        }
        String kit = args[0].toLowerCase(Locale.ROOT);
        if (!kits.yaml().contains(kit)) {
            msg().error(player, "kit-unknown", "name", kit);
            return;
        }
        String field = args[1].toLowerCase(Locale.ROOT);
        String value = args.length > 2 ? args[2] : "";
        switch (field) {
            case "items" -> {
                Editor editor = new Editor(kit);
                editor.inventory = Bukkit.createInventory(editor, 54, de.creativedm.ultra.core.Messages.legacy(msg().title(player, "editor-title", "name", kit)));
                items(kit).stream().limit(54).forEach(editor.inventory::addItem);
                player.openInventory(editor.inventory);
                msg().status(player, "editor-hint");
                return;
            }
            case "cooldown" -> {
                long millis = value.equals("0") ? 0 : Durations.parse(value);
                if (millis < 0) {
                    msg().error(player, "invalid-duration", "value", value);
                    return;
                }
                kits.yaml().set(kit + ".cooldown", millis / 1000);
            }
            case "cost" -> {
                try {
                    kits.yaml().set(kit + ".cost", Math.max(0, Double.parseDouble(value)));
                } catch (NumberFormatException e) {
                    msg().error(player, "invalid-number", "value", value);
                    return;
                }
            }
            case "onetime" -> kits.yaml().set(kit + ".one-time", !kits.yaml().getBoolean(kit + ".one-time"));
            case "firstjoin" -> kits.yaml().set(kit + ".first-join", !kits.yaml().getBoolean(kit + ".first-join"));
            case "icon" -> {
                Material hand = player.getInventory().getItemInMainHand().getType();
                if (hand.isAir()) {
                    msg().error(player, "icon-empty-hand");
                    return;
                }
                kits.yaml().set(kit + ".icon", hand.name());
            }
            default -> {
                usage(player, "editkit");
                return;
            }
        }
        kits.save();
        msg().info(player, "kit-updated", "name", kit);
    }

    @EventHandler
    public void onEditorClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Editor editor)) return;
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : event.getInventory().getContents()) if (item != null && !item.getType().isAir()) items.add(item.clone());
        kits.yaml().set(editor.kit + ".items", items);
        kits.save();
        msg().info(event.getPlayer(), "kit-updated", "name", editor.kit);
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "delkit");
            return;
        }
        String kit = args[0].toLowerCase(Locale.ROOT);
        if (!kits.yaml().contains(kit)) {
            msg().error(sender, "kit-unknown", "name", kit);
            return;
        }
        Runnable remove = () -> {
            kits.yaml().set(kit, null);
            kits.save();
            for (String id : usage.yaml().getKeys(false)) usage.yaml().set(id + "." + kit, null);
            usage.save();
            msg().info(sender, "kit-deleted", "name", kit);
        };
        if (sender instanceof Player player) {
            Menu.confirm(this, player, msg().title(player, "delete-title", "name", kit), () -> {
                player.closeInventory();
                remove.run();
            }, player::closeInventory);
        } else {
            remove.run();
        }
    }
}

package de.creativedm.ultra.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/** Chest GUI in the Ultra layout: black frame, content in the middle, navigation in the bottom row. */
public final class Menu implements InventoryHolder {

    public record Entry(ItemStack item, Consumer<InventoryClickEvent> click) {}

    /** Content slots of a six row menu. */
    public static final int PAGE_SIZE = 28;

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public Menu(Component title, int rows) {
        inventory = Bukkit.createInventory(this, rows * 9, Messages.legacy(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Menu frame() {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of());
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            if (slot < 9 || slot >= size - 9 || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, pane);
        }
        return this;
    }

    public Menu set(int slot, ItemStack item, Consumer<InventoryClickEvent> click) {
        inventory.setItem(slot, item);
        if (click == null) actions.remove(slot);
        else actions.put(slot, click);
        return this;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public static ItemStack item(Material material, Component name, List<Component> lore) {
        return decorate(new ItemStack(material), name, lore);
    }

    /** Gives any item the Ultra name and lore and hides its vanilla tooltip lines. */
    public static ItemStack decorate(ItemStack item, Component name, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        String text = Messages.legacy(name);
        meta.setDisplayName(text.isEmpty() ? " " : text);
        List<String> lines = new java.util.ArrayList<>();
        for (Component line : lore) lines.add(Messages.legacy(line));
        meta.setLore(lines);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack head(OfflinePlayer owner, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(owner);
            item.setItemMeta(meta);
        }
        return decorate(item, name, lore);
    }

    public static Material material(String name, Material def) {
        Material material = name == null ? null : Material.matchMaterial(name);
        return material == null || !material.isItem() || material.isAir() ? def : material;
    }

    /**
     * A six row list with paging. Slot 1 of the bottom row goes back (or to the previous page),
     * slot 5 closes, slot 9 opens the next page.
     */
    public static Menu paged(UltraPlugin plugin, Player player, Component title, List<Entry> entries, int page,
                             IntConsumer openPage, Runnable back) {
        Messages msg = plugin.msg();
        Menu menu = new Menu(title, 6).frame();
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = current * PAGE_SIZE + i;
            if (index >= entries.size()) break;
            Entry entry = entries.get(index);
            menu.set(10 + (i / 7) * 9 + (i % 7), entry.item(), entry.click());
        }
        if (current > 0) {
            menu.set(45, item(Material.ARROW, msg.text(player, "gui-previous"), List.of()), e -> openPage.accept(current - 1));
        } else if (back != null) {
            menu.set(45, item(Material.ARROW, msg.text(player, "gui-back"), List.of()), e -> back.run());
        }
        menu.set(49, item(Material.BARRIER, msg.text(player, "gui-close"), List.of()), e -> player.closeInventory());
        if (current < pages - 1) {
            menu.set(53, item(Material.ARROW, msg.text(player, "gui-next"), List.of()), e -> openPage.accept(current + 1));
        }
        return menu;
    }

    /** Confirmation for anything that cannot be undone: confirm on the left, cancel on the right. */
    public static void confirm(UltraPlugin plugin, Player player, Component title, Runnable yes, Runnable no) {
        Messages msg = plugin.msg();
        new Menu(title, 3).frame()
                .set(11, item(Material.LIME_CONCRETE, msg.text(player, "gui-confirm"), List.of()), e -> yes.run())
                .set(15, item(Material.RED_CONCRETE, msg.text(player, "gui-cancel"), List.of()), e -> no.run())
                .open(player);
    }

    /** Registered once per plugin by UltraPlugin. */
    static final class Clicks implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof Menu menu)) return;
            event.setCancelled(true);
            if (event.getClickedInventory() != menu.inventory) return;
            Consumer<InventoryClickEvent> action = menu.actions.get(event.getSlot());
            if (action == null || !(event.getWhoClicked() instanceof Player player)) return;
            if (org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(Menu.class) instanceof UltraPlugin plugin) plugin.msg().sound(player, "click");
            action.accept(event);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof Menu) event.setCancelled(true);
        }

    }
}

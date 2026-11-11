package de.creativedm.ultra.ranks;

import de.creativedm.ultra.core.Messages;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Asks a player for a line of text.
 * Minecraft 1.21.1+: an anvil opens and the player types into its name field.
 * Older versions have no API to open an anvil, so the player types into the chat instead.
 */
final class Input implements Listener {

    private record Session(Inventory anvil, String initial, Consumer<String> done, Runnable cancel) {}

    private final UltraRanks plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Object anvilType;
    private final Method createView;

    Input(UltraRanks plugin) {
        this.plugin = plugin;
        Object type = null;
        Method create = null;
        try {
            Class<?> menuType = Class.forName("org.bukkit.inventory.MenuType");
            Field field = menuType.getField("ANVIL");
            type = field.get(null);
            create = Class.forName("org.bukkit.inventory.MenuType$Typed").getMethod("create", HumanEntity.class, String.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().info("Anvil input needs Minecraft 1.21.1+, text input uses the chat on this server.");
        }
        this.anvilType = type;
        this.createView = create;
    }

    /** @param initial text the field starts with; cancel runs when the player closes the anvil or types "cancel" */
    void ask(Player player, String promptKey, String initial, Consumer<String> done, Runnable cancel) {
        sessions.remove(player.getUniqueId());
        if (anvilType != null) {
            try {
                String title = Messages.legacy(plugin.msg().text(player, promptKey));
                InventoryView view = (InventoryView) createView.invoke(anvilType, player, title);
                ItemStack paper = new ItemStack(Material.PAPER);
                ItemMeta meta = paper.getItemMeta();
                meta.setDisplayName(initial == null || initial.isEmpty() ? " " : initial);
                paper.setItemMeta(meta);
                player.openInventory(view);
                view.getTopInventory().setItem(0, paper);
                sessions.put(player.getUniqueId(), new Session(view.getTopInventory(), initial, done, cancel));
                return;
            } catch (ReflectiveOperationException | RuntimeException e) {
                plugin.getLogger().warning("Could not open anvil input, falling back to chat: " + e.getMessage());
            }
        }
        player.closeInventory();
        sessions.put(player.getUniqueId(), new Session(null, initial, done, cancel));
        plugin.msg().chat(player, "input-chat", "prompt", plugin.msg().raw(player, promptKey));
    }

    @EventHandler
    public void onPrepare(PrepareAnvilEvent event) {
        Session session = session(event.getView().getPlayer(), event.getInventory());
        if (session == null) return;
        String text = event.getInventory().getRenameText();
        ItemStack result = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(text == null || text.isBlank() ? " " : text);
        result.setItemMeta(meta);
        event.setResult(result);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Session session = session(event.getWhoClicked(), event.getView().getTopInventory());
        if (session == null) return;
        event.setCancelled(true);
        if (event.getRawSlot() != 2) return;
        String text = ((AnvilInventory) session.anvil()).getRenameText();
        if (text == null || text.isBlank()) text = session.initial();
        finish(event.getWhoClicked(), text == null ? "" : text.trim());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Session session = session(event.getPlayer(), event.getInventory());
        if (session == null) return;
        event.getInventory().clear(); // never hand the paper to the player
        sessions.remove(event.getPlayer().getUniqueId());
        if (session.cancel() != null) plugin.getServer().getScheduler().runTask(plugin, session.cancel());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || session.anvil() != null) return;
        event.setCancelled(true);
        String text = event.getMessage().trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancel") || text.equalsIgnoreCase("abbrechen")) {
                sessions.remove(event.getPlayer().getUniqueId());
                if (session.cancel() != null) session.cancel().run();
            } else {
                finish(event.getPlayer(), text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private Session session(HumanEntity player, Inventory inventory) {
        Session session = sessions.get(player.getUniqueId());
        return session != null && session.anvil() != null && session.anvil().equals(inventory) ? session : null;
    }

    private void finish(HumanEntity who, String text) {
        Session session = sessions.remove(who.getUniqueId());
        if (session == null) return;
        if (session.anvil() != null) {
            session.anvil().clear();
            who.closeInventory();
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> session.done().accept(text));
    }
}

package de.creativedm.ultra.chat;

import de.creativedm.ultra.core.Durations;
import de.creativedm.ultra.core.UltraPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class UltraChat extends UltraPlugin implements Listener {

    private static final Pattern ADVERTISING = Pattern.compile(
            "(\\d{1,3}\\.){3}\\d{1,3}|([a-z0-9-]+\\.)+(com|net|org|de|eu|io|gg|me|fr|nl|it|co|uk|tv|xyz|info|biz)\\b", Pattern.CASE_INSENSITIVE);

    // Chat runs off the main thread, so everything it touches is concurrent.
    private final Map<UUID, String> channels = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastChat = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();
    private volatile boolean locked;

    @Override
    public String module() {
        return "Chat";
    }

    @Override
    protected void enable() {
        getServer().getPluginManager().registerEvents(this, this);
        Completer none = (sender, args) -> List.of();
        command("msg", "ultrachat.msg", false, this::message, (s, a) -> a.length == 1 ? null : List.of());
        command("r", "ultrachat.msg", true, (s, a) -> reply((Player) s, a), none);
        command("channel", "ultrachat.channel", true, (s, a) -> channel((Player) s, a), (s, a) -> a.length == 1 ? channelNames(s) : List.of());
        command("chatclear", "ultrachat.clear", false, (s, a) -> clear(s), none);
        command("mutechat", "ultrachat.mutechat", false, (s, a) -> lock(s), none);
        command("slowmode", "ultrachat.slowmode", false, this::slowmode, (s, a) -> a.length == 1 ? List.of("0", "3", "5", "10") : List.of());
        command("socialspy", "ultrachat.socialspy", true, (s, a) -> spy((Player) s), none);
    }

    // ---- chat -----------------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        String text = event.getMessage();

        if (locked && !player.hasPermission("ultrachat.bypass.mutechat")) {
            event.setCancelled(true);
            msg().error(player, "chat-muted");
            return;
        }
        long now = System.currentTimeMillis();
        long wait = lastChat.getOrDefault(id, 0L) + getConfig().getInt("slowmode") * 1000L - now;
        if (wait > 0 && !player.hasPermission("ultrachat.bypass.slowmode")) {
            event.setCancelled(true);
            msg().error(player, "slowmode", "time", Durations.format(wait));
            return;
        }
        if (!player.hasPermission("ultrachat.bypass.filter")) {
            if (getConfig().getBoolean("filter.advertising.enabled") && advertises(text)) {
                event.setCancelled(true);
                msg().error(player, "filter-advertising");
                return;
            }
            if (getConfig().getBoolean("filter.spam.enabled") && text.equalsIgnoreCase(lastMessage.get(id))
                    && now - lastChat.getOrDefault(id, 0L) < getConfig().getInt("filter.spam.repeat-seconds", 30) * 1000L) {
                event.setCancelled(true);
                msg().error(player, "filter-spam");
                return;
            }
            text = filter(text);
        }
        lastChat.put(id, now);
        lastMessage.put(id, text);

        Component message = player.hasPermission("ultrachat.color")
                ? LegacyComponentSerializer.legacyAmpersand().deserialize(text) : Component.text(text);
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (getConfig().getBoolean("show-item") && text.contains("[item]") && !hand.getType().isAir()) {
            message = message.replaceText(TextReplacementConfig.builder().matchLiteral("[item]").replacement(itemName(hand)).once().build());
        }
        if (getConfig().getBoolean("mentions")) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other == player || !player.canSee(other) || !text.toLowerCase(Locale.ROOT).contains("@" + other.getName().toLowerCase(Locale.ROOT))) continue;
                message = message.replaceText(TextReplacementConfig.builder()
                        .match(Pattern.compile("@" + Pattern.quote(other.getName()), Pattern.CASE_INSENSITIVE))
                        .replacement(msg().parse("<accent>@<name></accent>", "name", other.getName())).build());
                getServer().getScheduler().runTask(this, () -> msg().info(other, "mentioned", "player", player.getName()));
            }
        }

        String channel = channels.getOrDefault(id, getConfig().getString("default-channel", "global"));
        ConfigurationSection settings = getConfig().getConfigurationSection("channels." + channel);
        String permission = settings == null ? "" : settings.getString("permission", "");
        int radius = settings == null ? 0 : settings.getInt("radius");
        List<Player> receivers = new ArrayList<>();
        for (Player other : event.getRecipients()) {
            boolean blocked = other != player && ((!permission.isEmpty() && !other.hasPermission(permission))
                    || (radius > 0 && (other.getWorld() != player.getWorld()
                    || other.getLocation().distanceSquared(player.getLocation()) > (double) radius * radius)));
            if (!blocked) receivers.add(other);
        }

        String format = (settings == null ? "" : settings.getString("prefix", "")) + format(player);
        Component line = msg().parse(format, "player", de.creativedm.ultra.core.Messages.fromLegacy(player.getDisplayName()),
                "name", player.getName(), "world", player.getWorld().getName(), "message", message,
                "prefix", rankText(player, "prefix"), "suffix", rankText(player, "suffix"), "rank", rankText(player, "rank"));
        // Spigot's chat cannot carry hover/click text, so we deliver the formatted line ourselves.
        // The event stays alive (with no recipients) so the console log and other plugins still see it.
        for (Player receiver : receivers) msg().send(receiver, line);
        event.getRecipients().clear();
        event.setMessage(text);
        event.setFormat("[" + channel + "] %1$s: %2$s");
    }

    /**
     * Prefix, suffix or rank name: from UltraRanks (player metadata, MiniMessage) or any other rank plugin through Vault.
     */
    private Component rankText(Player player, String type) {
        for (var value : player.getMetadata("ultraranks_" + type)) return msg().parse(value.asString());
        if (type.equals("rank") || getServer().getPluginManager().getPlugin("Vault") == null) return Component.empty();
        return VaultChat.text(player, type);
    }

    /** Only loaded when Vault is installed. */
    private static final class VaultChat {
        static Component text(Player player, String type) {
            var provider = org.bukkit.Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
            if (provider == null) return Component.empty();
            String value = type.equals("prefix") ? provider.getProvider().getPlayerPrefix(player) : provider.getProvider().getPlayerSuffix(player);
            return de.creativedm.ultra.core.Messages.fromLegacy(value == null ? "" : value.replace('&', '§'));
        }
    }

    /** "[Name ×amount]" with the item tooltip on hover. */
    private Component itemName(ItemStack item) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        Component name = meta != null && meta.hasDisplayName()
                ? de.creativedm.ultra.core.Messages.fromLegacy(meta.getDisplayName())
                : Component.translatable(item.getType().getTranslationKey());
        Component shown = Component.text("[").append(name).append(Component.text(item.getAmount() > 1 ? " ×" + item.getAmount() + "]" : "]"));
        return shown.color(net.kyori.adventure.text.format.TextColor.fromHexString(getConfig().getString("theme.accent", "#22D3EE")))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showItem(
                        net.kyori.adventure.key.Key.key(item.getType().getKey().toString()), item.getAmount()));
    }

    /** First format the player has ultrachat.format.&lt;name&gt; for, otherwise "default". */
    private String format(Player player) {
        ConfigurationSection formats = getConfig().getConfigurationSection("formats");
        if (formats != null) {
            for (String key : formats.getKeys(false)) {
                String permission = "ultrachat.format." + key;
                if (!key.equals("default") && player.isPermissionSet(permission) && player.hasPermission(permission)) return formats.getString(key);
            }
        }
        return getConfig().getString("formats.default", "<muted><player></muted> <sep>»</sep> <white><message>");
    }

    private boolean advertises(String text) {
        Matcher matcher = ADVERTISING.matcher(text);
        List<String> allowed = getConfig().getStringList("filter.advertising.allowed");
        while (matcher.find()) {
            String hit = matcher.group().toLowerCase(Locale.ROOT);
            if (allowed.stream().noneMatch(entry -> hit.endsWith(entry.toLowerCase(Locale.ROOT)))) return true;
        }
        return false;
    }

    private String filter(String text) {
        if (getConfig().getBoolean("filter.caps.enabled") && text.length() >= getConfig().getInt("filter.caps.min-length", 8)) {
            long letters = text.chars().filter(Character::isLetter).count();
            long upper = text.chars().filter(Character::isUpperCase).count();
            if (letters > 0 && upper * 100 / letters > getConfig().getInt("filter.caps.max-percent", 60)) text = text.toLowerCase(Locale.ROOT);
        }
        String replacement = getConfig().getString("filter.words.replacement", "***");
        for (String word : getConfig().getStringList("filter.words.blocked")) {
            if (!word.isEmpty()) text = text.replaceAll("(?i)" + Pattern.quote(word), Matcher.quoteReplacement(replacement));
        }
        return text;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastChat.remove(id);
        lastMessage.remove(id);
        lastPartner.remove(id);
    }

    // ---- private messages -----------------------------------------------------------------------------------

    private void message(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "msg");
            return;
        }
        Player target = player(sender, args[0]);
        if (target != null) deliver(sender, target, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
    }

    private void reply(Player player, String[] args) {
        if (args.length == 0) {
            usage(player, "r");
            return;
        }
        UUID partner = lastPartner.get(player.getUniqueId());
        Player target = partner == null ? null : Bukkit.getPlayer(partner);
        if (target == null) {
            msg().error(player, "reply-none");
            return;
        }
        deliver(player, target, String.join(" ", args));
    }

    /** Private messages are chat content, so they are shown in the chat. */
    private void deliver(CommandSender sender, Player target, String text) {
        if (sender == target) {
            msg().error(sender, "pm-self");
            return;
        }
        if (sender instanceof Player player && !player.hasPermission("ultrachat.bypass.filter")) {
            if (getConfig().getBoolean("filter.advertising.enabled") && advertises(text)) {
                msg().error(sender, "filter-advertising");
                return;
            }
            text = filter(text);
        }
        msg().send(sender, msg().text(sender, "pm-out", "player", target.getName(), "message", text));
        msg().send(target, msg().text(target, "pm-in", "player", sender.getName(), "message", text));
        msg().sound(target, "success");
        if (sender instanceof Player player) {
            lastPartner.put(player.getUniqueId(), target.getUniqueId());
            lastPartner.put(target.getUniqueId(), player.getUniqueId());
        }
        for (UUID id : spies) {
            Player spy = Bukkit.getPlayer(id);
            if (spy != null && spy != sender && spy != target) {
                msg().send(spy, msg().text(spy, "pm-spy", "from", sender.getName(), "to", target.getName(), "message", text));
            }
        }
    }

    private void spy(Player player) {
        boolean on = spies.add(player.getUniqueId());
        if (!on) spies.remove(player.getUniqueId());
        msg().info(player, on ? "spy-on" : "spy-off");
    }

    // ---- channels and moderation ----------------------------------------------------------------------------

    private List<String> channelNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        ConfigurationSection section = getConfig().getConfigurationSection("channels");
        if (section == null) return names;
        for (String key : section.getKeys(false)) {
            String permission = section.getString(key + ".permission", "");
            if (permission.isEmpty() || sender.hasPermission(permission)) names.add(key);
        }
        return names;
    }

    private void channel(Player player, String[] args) {
        if (args.length == 0) {
            msg().info(player, "channel-current", "channel", channels.getOrDefault(player.getUniqueId(), getConfig().getString("default-channel", "global")));
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        if (!channelNames(player).contains(name)) {
            msg().error(player, "channel-unknown", "channel", name);
            return;
        }
        channels.put(player.getUniqueId(), name);
        msg().info(player, "channel-switched", "channel", name);
    }

    private void clear(CommandSender sender) {
        String blank = " ";
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("ultrachat.bypass.clear")) for (int i = 0; i < 100; i++) player.sendMessage(blank);
            msg().status(player, "chat-cleared", "player", sender.getName());
        }
        if (!(sender instanceof Player)) msg().info(sender, "chat-cleared", "player", sender.getName());
    }

    private void lock(CommandSender sender) {
        locked = !locked;
        for (Player player : Bukkit.getOnlinePlayers()) msg().status(player, locked ? "chat-locked" : "chat-unlocked", "player", sender.getName());
        if (!(sender instanceof Player)) msg().info(sender, locked ? "chat-locked" : "chat-unlocked", "player", sender.getName());
    }

    private void slowmode(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "slowmode");
            return;
        }
        int seconds;
        try {
            seconds = Math.max(0, Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
            msg().error(sender, "invalid-number", "value", args[0]);
            return;
        }
        getConfig().set("slowmode", seconds);
        saveConfig();
        msg().info(sender, seconds == 0 ? "slowmode-off" : "slowmode-set", "time", seconds + "s");
    }
}

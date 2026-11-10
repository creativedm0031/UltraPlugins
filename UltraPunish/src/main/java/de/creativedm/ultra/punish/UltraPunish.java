package de.creativedm.ultra.punish;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Durations;
import de.creativedm.ultra.core.Menu;
import de.creativedm.ultra.core.UltraPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UltraPunish extends UltraPlugin implements Listener {

    enum Type { BAN, MUTE, KICK, WARN }

    /** One entry of a player's history. until = 0 means permanent. */
    static final class Entry {
        int id;
        UUID player;
        String name;
        Type type;
        String reason;
        String staff;
        long time;
        long until;
        boolean ip;
        volatile boolean active;

        boolean running() {
            return active && (until == 0 || until > System.currentTimeMillis());
        }
    }

    private record Target(UUID id, String name) {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private DataFile data;

    @Override
    public String module() {
        return "Punish";
    }

    @Override
    protected void enable() {
        data = new DataFile(this, "punishments.yml");
        ConfigurationSection records = data.yaml().getConfigurationSection("records");
        if (records != null) {
            for (String key : records.getKeys(false)) {
                ConfigurationSection s = records.getConfigurationSection(key);
                Entry e = new Entry();
                e.id = Integer.parseInt(key);
                e.player = UUID.fromString(s.getString("player"));
                e.name = s.getString("name");
                e.type = Type.valueOf(s.getString("type"));
                e.reason = s.getString("reason", "");
                e.staff = s.getString("staff");
                e.time = s.getLong("time");
                e.until = s.getLong("until");
                e.ip = s.getBoolean("ip");
                e.active = s.getBoolean("active");
                entries.add(e);
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
        Completer players = (sender, args) -> args.length == 1 ? null
                : args.length == 2 ? List.of("30m", "1h", "1d", "7d", "30d") : List.of();
        Completer playersOnly = (sender, args) -> args.length == 1 ? null : List.of();
        for (String name : List.of("ban", "tempban", "banip", "mute", "tempmute", "kick", "warn")) {
            String permission = "ultrapunish." + (name.contains("ban") ? "ban" : name.contains("mute") ? "mute" : name);
            command(name, permission, false, (s, a) -> punishCommand(name, s, a), name.startsWith("temp") ? players : playersOnly);
        }
        command("unban", "ultrapunish.revoke", false, (s, a) -> revoke(s, a, Type.BAN), playersOnly);
        command("unmute", "ultrapunish.revoke", false, (s, a) -> revoke(s, a, Type.MUTE), playersOnly);
        command("history", "ultrapunish.history", false, this::history, playersOnly);
        command("punish", "ultrapunish.gui", true, this::punishMenu, playersOnly);
    }

    // ---- storage --------------------------------------------------------------------------------------------

    private void store(Entry e) {
        String path = "records." + e.id + ".";
        data.yaml().set(path + "player", e.player.toString());
        data.yaml().set(path + "name", e.name);
        data.yaml().set(path + "type", e.type.name());
        data.yaml().set(path + "reason", e.reason);
        data.yaml().set(path + "staff", e.staff);
        data.yaml().set(path + "time", e.time);
        data.yaml().set(path + "until", e.until);
        data.yaml().set(path + "ip", e.ip);
        data.yaml().set(path + "active", e.active);
    }

    private Target target(CommandSender sender, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return new Target(online.getUniqueId(), online.getName());
        String id = data.yaml().getString("names." + name.toLowerCase(Locale.ROOT));
        if (id == null) {
            msg().error(sender, "player-never-joined", "player", name);
            return null;
        }
        return new Target(UUID.fromString(id), data.yaml().getString("players." + id + ".name", name));
    }

    private Entry running(UUID player, Type type) {
        for (Entry e : entries) if (e.type == type && e.player.equals(player) && e.running()) return e;
        return null;
    }

    // ---- commands -------------------------------------------------------------------------------------------

    private void punishCommand(String command, CommandSender sender, String[] args) {
        boolean temporary = command.startsWith("temp");
        if (args.length < (temporary ? 2 : 1)) {
            usage(sender, command);
            return;
        }
        Target target = target(sender, args[0]);
        if (target == null) return;
        long duration = 0;
        if (temporary) {
            duration = Durations.parse(args[1]);
            if (duration < 0) {
                msg().error(sender, "invalid-duration", "value", args[1]);
                return;
            }
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, temporary ? 2 : 1, args.length));
        Type type = command.contains("ban") ? Type.BAN : command.contains("mute") ? Type.MUTE : command.equals("kick") ? Type.KICK : Type.WARN;
        apply(sender, target, type, duration, reason, command.equals("banip"));
    }

    private void apply(CommandSender staff, Target target, Type type, long duration, String reason, boolean ip) {
        Player online = Bukkit.getPlayer(target.id());
        if (online != null && staff instanceof Player && online.hasPermission("ultrapunish.exempt")) {
            msg().error(staff, "target-exempt", "player", target.name());
            return;
        }
        if (type == Type.KICK && online == null) {
            msg().error(staff, "player-not-found", "player", target.name());
            return;
        }
        if (type == Type.BAN || type == Type.MUTE) {
            Entry old = running(target.id(), type);
            if (old != null) {
                old.active = false;
                store(old);
            }
        }
        Entry e = new Entry();
        e.id = entries.stream().mapToInt(x -> x.id).max().orElse(0) + 1;
        e.player = target.id();
        e.name = target.name();
        e.type = type;
        e.reason = reason;
        e.staff = staff.getName();
        e.time = System.currentTimeMillis();
        e.until = duration > 0 ? e.time + duration : 0;
        e.ip = ip;
        e.active = type != Type.KICK;
        entries.add(e);
        store(e);
        data.save();

        notifyStaff(staff, "notify-" + type.name().toLowerCase(Locale.ROOT), e);
        switch (type) {
            case BAN -> {
                String address = data.yaml().getString("players." + target.id() + ".ip");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean sameAddress = ip && address != null && player.getAddress() != null
                            && address.equals(player.getAddress().getAddress().getHostAddress());
                    if (player.getUniqueId().equals(target.id()) || sameAddress) player.kick(screen(player, "ban-screen", e));
                }
            }
            case KICK -> online.kick(screen(online, "kick-screen", e));
            case MUTE -> {
                if (online != null) msg().error(online, "you-were-muted", "duration", duration(online, e), "reason", reason(online, e));
            }
            case WARN -> {
                long warnings = entries.stream().filter(x -> x.type == Type.WARN && x.active && x.player.equals(target.id())).count();
                if (online != null) msg().error(online, "you-were-warned", "reason", reason(online, e), "count", warnings);
                String action = getConfig().getString("warn-actions." + warnings);
                if (action != null) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.replace("%player%", target.name()));
            }
        }
    }

    private void revoke(CommandSender sender, String[] args, Type type) {
        if (args.length == 0) {
            usage(sender, type == Type.BAN ? "unban" : "unmute");
            return;
        }
        Target target = target(sender, args[0]);
        if (target == null) return;
        Entry e = running(target.id(), type);
        if (e == null) {
            msg().error(sender, type == Type.BAN ? "not-banned" : "not-muted", "player", target.name());
            return;
        }
        e.active = false;
        store(e);
        data.save();
        notifyStaff(sender, type == Type.BAN ? "notify-unban" : "notify-unmute", e);
        Player online = Bukkit.getPlayer(target.id());
        if (type == Type.MUTE && online != null && online != sender) msg().info(online, "you-were-unmuted");
    }

    /** Tells the acting staff member and everyone with ultrapunish.notify, each in their own language. */
    private void notifyStaff(CommandSender actor, String key, Entry e) {
        Set<CommandSender> receivers = new LinkedHashSet<>();
        receivers.add(actor);
        receivers.add(Bukkit.getConsoleSender());
        for (Player player : Bukkit.getOnlinePlayers()) if (player.hasPermission("ultrapunish.notify")) receivers.add(player);
        for (CommandSender receiver : receivers) {
            Object[] values = {"player", e.name, "staff", actor.getName(), "duration", duration(receiver, e), "reason", reason(receiver, e)};
            if (receiver == actor) msg().info(receiver, key, values);
            else msg().status(receiver, key, values);
        }
    }

    private String reason(CommandSender viewer, Entry e) {
        return e.reason == null || e.reason.isEmpty() ? msg().raw(viewer, "no-reason") : e.reason;
    }

    private String duration(CommandSender viewer, Entry e) {
        return e.until == 0 ? msg().raw(viewer, "permanent") : Durations.format(e.until - e.time);
    }

    private Component screen(CommandSender viewer, String key, Entry e) {
        String left = e.until == 0 ? msg().raw(viewer, "permanent") : Durations.format(e.until - System.currentTimeMillis());
        return msg().text(viewer, key, "reason", reason(viewer, e), "duration", left, "staff", e.staff, "id", e.id);
    }

    // ---- history and GUI ------------------------------------------------------------------------------------

    private void history(CommandSender sender, String[] args) {
        if (args.length == 0) {
            usage(sender, "history");
            return;
        }
        Target target = target(sender, args[0]);
        if (target == null) return;
        if (sender instanceof Player player) {
            openHistory(player, target, 0);
            return;
        }
        for (Entry e : entries) {
            if (!e.player.equals(target.id())) continue;
            sender.sendMessage("#" + e.id + " " + e.type + " " + DATE.format(Instant.ofEpochMilli(e.time)) + " by " + e.staff + " ("
                    + duration(sender, e) + ", " + (e.running() ? "active" : "inactive") + "): " + reason(sender, e));
        }
    }

    private void openHistory(Player viewer, Target target, int page) {
        List<Menu.Entry> items = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            if (!e.player.equals(target.id())) continue;
            Material icon = switch (e.type) {
                case BAN -> Material.BARRIER;
                case MUTE -> Material.WRITABLE_BOOK;
                case KICK -> Material.LEATHER_BOOTS;
                case WARN -> Material.PAPER;
            };
            String type = msg().raw(viewer, "type-" + e.type.name().toLowerCase(Locale.ROOT));
            items.add(new Menu.Entry(Menu.item(icon, msg().parse("<brand>#<id> <type></brand>", "id", e.id, "type", type),
                    msg().lore(viewer, "history-lore", "reason", reason(viewer, e), "staff", e.staff,
                            "date", DATE.format(Instant.ofEpochMilli(e.time)), "duration", e.type == Type.BAN || e.type == Type.MUTE ? duration(viewer, e) : "–",
                            "status", msg().raw(viewer, e.running() ? "status-active" : "status-inactive"))), null));
        }
        if (items.isEmpty()) {
            msg().error(viewer, "history-empty", "player", target.name());
            return;
        }
        Menu.paged(this, viewer, msg().title(viewer, "history-title", "player", target.name()), items, page,
                next -> openHistory(viewer, target, next), null).open(viewer);
    }

    private void punishMenu(CommandSender sender, String[] args) {
        Player viewer = (Player) sender;
        if (args.length == 0) {
            usage(sender, "punish");
            return;
        }
        Target target = target(sender, args[0]);
        if (target == null) return;
        Menu menu = new Menu(msg().title(viewer, "punish-title", "player", target.name()), 6).frame();
        menu.set(4, Menu.head(Bukkit.getOfflinePlayer(target.id()), msg().parse("<brand><player></brand>", "player", target.name()), List.of()), null);
        ConfigurationSection templates = getConfig().getConfigurationSection("templates");
        int index = 0;
        if (templates != null) {
            for (String key : templates.getKeys(false)) {
                if (index >= Menu.PAGE_SIZE) break;
                ConfigurationSection t = templates.getConfigurationSection(key);
                Type type;
                try {
                    type = Type.valueOf(t.getString("type", "WARN").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Template '" + key + "' has an unknown type");
                    continue;
                }
                String reason = t.getString("reason", key);
                List<String> steps = t.getStringList("durations");
                long previous = entries.stream().filter(x -> x.player.equals(target.id()) && x.type == type && reason.equals(x.reason)).count();
                String step = steps.isEmpty() ? "permanent" : steps.get((int) Math.min(previous, steps.size() - 1));
                long millis = Durations.parse(step);
                long duration = millis < 0 ? 0 : millis;
                String shown = type == Type.KICK || type == Type.WARN ? "–" : duration == 0 ? msg().raw(viewer, "permanent") : Durations.format(duration);
                menu.set(10 + (index / 7) * 9 + (index % 7), Menu.item(Menu.material(t.getString("icon"), Material.PAPER),
                        msg().parse("<brand><reason></brand>", "reason", reason),
                        msg().lore(viewer, "template-lore", "type", msg().raw(viewer, "type-" + type.name().toLowerCase(Locale.ROOT)), "duration", shown)),
                        click -> {
                            viewer.closeInventory();
                            if (!viewer.hasPermission("ultrapunish." + type.name().toLowerCase(Locale.ROOT))) {
                                msg().error(viewer, "no-permission");
                                return;
                            }
                            apply(viewer, target, type, duration, reason, false);
                        });
                index++;
            }
        }
        menu.set(45, Menu.item(Material.BOOK, msg().text(viewer, "history-button"), List.of()), click -> {
            if (viewer.hasPermission("ultrapunish.history")) openHistory(viewer, target, 0);
            else msg().error(viewer, "no-permission");
        });
        menu.set(49, Menu.item(Material.BARRIER, msg().text(viewer, "gui-close"), List.of()), click -> viewer.closeInventory());
        menu.open(viewer);
    }

    // ---- enforcement ----------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        String address = event.getAddress().getHostAddress();
        for (Entry e : entries) {
            if (e.type != Type.BAN || !e.running()) continue;
            boolean sameAddress = e.ip && address.equals(data.yaml().getString("players." + e.player + ".ip"));
            if (e.player.equals(event.getUniqueId()) || sameAddress) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, screen(Bukkit.getConsoleSender(), "ban-screen", e));
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String id = player.getUniqueId().toString();
        String address = player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress();
        data.yaml().set("players." + id + ".name", player.getName());
        data.yaml().set("players." + id + ".ip", address);
        data.yaml().set("names." + player.getName().toLowerCase(Locale.ROOT), id);
        data.save();
        if (address == null || !getConfig().getBoolean("alt-detection", true)) return;
        for (Entry e : entries) {
            if (e.type != Type.BAN || !e.running() || e.player.equals(player.getUniqueId())) continue;
            if (!address.equals(data.yaml().getString("players." + e.player + ".ip"))) continue;
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("ultrapunish.notify")) msg().chat(staff, "notify-alt", "player", player.getName(), "alt", e.name);
            }
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Entry mute = running(event.getPlayer().getUniqueId(), Type.MUTE);
        if (mute == null) return;
        event.setCancelled(true);
        muted(event.getPlayer(), mute);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Entry mute = running(event.getPlayer().getUniqueId(), Type.MUTE);
        if (mute == null) return;
        String label = event.getMessage().substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        if (label.contains(":")) label = label.substring(label.indexOf(':') + 1);
        if (!getConfig().getStringList("muted-commands").contains(label)) return;
        event.setCancelled(true);
        muted(event.getPlayer(), mute);
    }

    private void muted(Player player, Entry mute) {
        String left = mute.until == 0 ? msg().raw(player, "permanent") : Durations.format(mute.until - System.currentTimeMillis());
        msg().error(player, "you-are-muted", "duration", left, "reason", reason(player, mute));
    }
}

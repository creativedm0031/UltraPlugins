package de.creativedm.ultra.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Base class of every Ultra plugin: shared messages, commands, admin command and reload. */
public abstract class UltraPlugin extends JavaPlugin {

    public interface Handler {
        void run(CommandSender sender, String[] args);
    }

    public interface Completer {
        /** Return null to complete online player names. */
        List<String> complete(CommandSender sender, String[] args);
    }

    private Messages messages;
    private net.kyori.adventure.platform.bukkit.BukkitAudiences audiences;
    private Teleporter teleporter;
    /** Checked by us instead of Bukkit, so a missing permission is answered in the Ultra layout. */
    private final java.util.Map<String, String> permissions = new java.util.HashMap<>();

    /** Name without "Ultra", e.g. "Spawn". */
    public abstract String module();

    protected abstract void enable();

    protected void disable() {}

    /** Called after config.yml and the language files were reloaded. */
    protected void reloaded() {}

    /** Extra sub commands of /ultra&lt;name&gt;. Return true if handled. */
    protected boolean adminCommand(CommandSender sender, String[] args) {
        return false;
    }

    protected List<String> adminSubcommands() {
        return List.of();
    }

    public final String id() {
        return "ultra" + module().toLowerCase(Locale.ROOT);
    }

    public final Messages msg() {
        return messages;
    }

    public final Teleporter teleporter() {
        return teleporter;
    }

    @Override
    public final void onEnable() {
        saveDefaultConfig();
        audiences = net.kyori.adventure.platform.bukkit.BukkitAudiences.create(this);
        messages = new Messages(this, audiences);
        teleporter = new Teleporter(this);
        getServer().getPluginManager().registerEvents(new Menu.Clicks(), this);
        command(id(), id() + ".admin", false, this::admin, (sender, args) -> {
            if (args.length == 1) {
                List<String> subs = new ArrayList<>(List.of("help", "reload", "version", "language"));
                subs.addAll(adminSubcommands());
                return subs;
            }
            return args.length == 2 && args[0].equalsIgnoreCase("language") ? Messages.LANGUAGES : List.of();
        });
        enable();
    }

    @Override
    public final void onDisable() {
        disable();
        if (audiences != null) audiences.close();
    }

    private void admin(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                reloadConfig();
                messages.load();
                reloaded();
                messages.info(sender, "reloaded");
            }
            case "version" -> messages.info(sender, "version", "version", getDescription().getVersion());
            case "language" -> {
                if (args.length < 2) {
                    messages.info(sender, "language-current", "language", messages.defaultLanguage());
                } else if (!Messages.LANGUAGES.contains(args[1].toLowerCase(Locale.ROOT))) {
                    messages.error(sender, "language-unknown", "languages", String.join(", ", Messages.LANGUAGES));
                } else {
                    getConfig().set("language", args[1].toLowerCase(Locale.ROOT));
                    saveConfig();
                    messages.load();
                    messages.info(sender, "language-set", "language", args[1].toLowerCase(Locale.ROOT));
                }
            }
            case "help" -> help(sender);
            default -> {
                if (!adminCommand(sender, args)) help(sender);
            }
        }
    }

    /** Help is a list, so it is the one piece of feedback that is printed to the chat. */
    private void help(CommandSender sender) {
        messages.send(sender, messages.parse("<brand><bold>Ultra" + module() + "</bold></brand> <sep>»</sep> <muted>v" + getDescription().getVersion()));
        for (String name : getDescription().getCommands().keySet()) {
            PluginCommand command = getCommand(name);
            String permission = permissions.get(name);
            if (command == null || (permission != null && !sender.hasPermission(permission))) continue;
            messages.send(sender, messages.parse("<sep>▸</sep> <accent><usage></accent> <sep>–</sep> <muted>" + messages.raw(sender, "help-" + name),
                    "usage", command.getUsage().replace("<command>", name)));
        }
    }

    /** Registers a command that is declared in plugin.yml. Permission and player checks answer in the Ultra layout. */
    public final void command(String name, String permission, boolean playerOnly, Handler handler, Completer completer) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing in plugin.yml");
            return;
        }
        if (permission != null) permissions.put(name, permission);
        command.setExecutor((sender, cmd, label, args) -> {
            if (permission != null && !sender.hasPermission(permission)) {
                messages.error(sender, "no-permission");
            } else if (playerOnly && !(sender instanceof Player)) {
                messages.error(sender, "player-only");
            } else {
                handler.run(sender, args);
            }
            return true;
        });
        command.setTabCompleter((sender, cmd, label, args) -> {
            if (permission != null && !sender.hasPermission(permission)) return List.of();
            List<String> options = completer == null ? List.of() : completer.complete(sender, args);
            if (options == null) return null;
            String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String option : options) if (option.toLowerCase(Locale.ROOT).startsWith(typed)) matches.add(option);
            return matches;
        });
    }

    /** Sends the usage of a command as an error. */
    public final void usage(CommandSender sender, String command) {
        PluginCommand cmd = getCommand(command);
        messages.error(sender, "usage", "usage", cmd == null ? "/" + command : cmd.getUsage().replace("<command>", command));
    }

    /** Finds an online player the sender is able to see, or sends "player-not-found". */
    public final Player player(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || (sender instanceof Player viewer && !viewer.canSee(target))) {
            messages.error(sender, "player-not-found", "player", name);
            return null;
        }
        return target;
    }

    /** Highest number granted through permissions like "ultrahomes.limit.5", or the default. */
    public final int permissionNumber(Player player, String prefix, int def) {
        int best = -1;
        for (var info : player.getEffectivePermissions()) {
            if (!info.getValue() || !info.getPermission().startsWith(prefix)) continue;
            try {
                best = Math.max(best, Integer.parseInt(info.getPermission().substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // not a number, e.g. "prefix.*"
            }
        }
        return best < 0 ? def : best;
    }
}

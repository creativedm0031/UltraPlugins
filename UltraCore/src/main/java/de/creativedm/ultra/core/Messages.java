package de.creativedm.ultra.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * The one place every Ultra plugin sends text through.
 * Feedback goes to the action bar, errors go to the chat, in the language of the receiver.
 */
public final class Messages {

    public static final List<String> LANGUAGES = List.of("en", "de", "fr", "nl", "it");

    /** Section-sign text with hex colours, for item names, lore, inventory titles and kick screens (Spigot needs Strings there). */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR).hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public static String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    public static Component fromLegacy(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    private final UltraPlugin plugin;
    private final BukkitAudiences audiences;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private MiniMessage mini = MiniMessage.miniMessage();
    private String fallback = "en";
    private boolean clientLanguage = true;
    private String gradient = "<gradient:#8B5CF6:#22D3EE>";

    Messages(UltraPlugin plugin, BukkitAudiences audiences) {
        this.plugin = plugin;
        this.audiences = audiences;
        load();
    }

    void load() {
        FileConfiguration config = plugin.getConfig();
        fallback = config.getString("language", "en").toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(fallback)) {
            plugin.getLogger().warning("Unknown language '" + fallback + "', using en. Available: " + LANGUAGES);
            fallback = "en";
        }
        clientLanguage = config.getBoolean("use-client-language", true);
        String primary = config.getString("theme.primary", "#8B5CF6");
        String accent = config.getString("theme.accent", "#22D3EE");
        gradient = "<gradient:" + primary + ":" + accent + ">";
        mini = MiniMessage.builder().tags(TagResolver.resolver(
                TagResolver.standard(),
                color("primary", primary, "#8B5CF6"),
                color("accent", accent, "#22D3EE"),
                color("muted", config.getString("theme.muted"), "#94A3B8"),
                color("sep", config.getString("theme.separator"), "#64748B"),
                color("error", config.getString("theme.error"), "#F87171"),
                color("errortext", config.getString("theme.error-text"), "#FCA5A5"))).build();

        languages.clear();
        for (String code : LANGUAGES) {
            File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
            YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
            boolean changed = !file.exists();
            for (String resource : new String[] {"core-lang/" + code + ".yml", "lang/" + code + ".yml"}) {
                try (InputStream in = plugin.getResource(resource)) {
                    if (in == null) continue;
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                    for (String key : defaults.getKeys(false)) {
                        if (!yaml.contains(key)) {
                            yaml.set(key, defaults.get(key));
                            changed = true;
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not read " + resource + ": " + e.getMessage());
                }
            }
            if (changed) {
                try {
                    yaml.save(file);
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not save " + file + ": " + e.getMessage());
                }
            }
            languages.put(code, yaml);
        }
    }

    private static TagResolver color(String name, String hex, String def) {
        TextColor color = hex == null ? null : TextColor.fromHexString(hex);
        if (color == null) color = TextColor.fromHexString(def);
        return TagResolver.resolver(name, Tag.styling(color));
    }

    /** Language code used for this receiver. */
    public String language(CommandSender sender) {
        if (clientLanguage && sender instanceof Player player) {
            String locale = player.getLocale();
            String code = locale == null ? "" : locale.toLowerCase(Locale.ROOT).split("_")[0];
            if (LANGUAGES.contains(code)) return code;
        }
        return fallback;
    }

    public String defaultLanguage() {
        return fallback;
    }

    /** The untranslated MiniMessage string behind a key. */
    public String raw(CommandSender sender, String key) {
        String text = languages.get(language(sender)).getString(key);
        if (text == null) text = languages.get("en").getString(key);
        return text == null ? key : text;
    }

    private TagResolver placeholders(Object... pairs) {
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String name = String.valueOf(pairs[i]);
            builder.resolver(pairs[i + 1] instanceof Component component
                    ? Placeholder.component(name, component)
                    : Placeholder.unparsed(name, String.valueOf(pairs[i + 1])));
        }
        return builder.build();
    }

    /** Parses a MiniMessage string with the Ultra theme tags. */
    public Component parse(String miniMessage, Object... pairs) {
        return mini.deserialize(miniMessage.replace("<brand>", gradient).replace("</brand>", "</gradient>"), placeholders(pairs))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** A translated line without prefix, for GUIs and custom layouts. */
    public Component text(CommandSender sender, String key, Object... pairs) {
        return parse(raw(sender, key), pairs);
    }

    /** Translated lore: one line per "\n" in the language file. */
    public List<Component> lore(CommandSender sender, String key, Object... pairs) {
        List<Component> lines = new ArrayList<>();
        for (String line : raw(sender, key).split("\n")) lines.add(parse("<muted>" + line, pairs));
        return lines;
    }

    /** GUI title: "Ultra » Page". */
    public Component title(CommandSender sender, String key, Object... pairs) {
        return parse("<primary><bold>Ultra</bold></primary> <dark_gray>» " + raw(sender, key), pairs);
    }

    private Component infoLine(CommandSender sender, String key, Object... pairs) {
        return parse("<brand>✦ " + plugin.module() + "</brand> <sep>»</sep> <white>" + raw(sender, key), pairs);
    }

    /** Feedback: action bar plus success sound. The console gets a normal line. */
    public void info(CommandSender sender, String key, Object... pairs) {
        status(sender, key, pairs);
        if (sender instanceof Player player) sound(player, "success");
    }

    /** Silent action bar line, for countdowns and permanent status. */
    public void status(CommandSender sender, String key, Object... pairs) {
        Component line = infoLine(sender, key, pairs);
        if (sender instanceof Player player) audiences.player(player).sendActionBar(line);
        else send(sender, line);
    }

    /** A line in the chat with the normal prefix. Only for content that has to be clickable or re-readable. */
    public void chat(CommandSender sender, String key, Object... pairs) {
        send(sender, infoLine(sender, key, pairs));
    }

    /** Errors are the only plugin feedback that goes to the chat. */
    public void error(CommandSender sender, String key, Object... pairs) {
        send(sender, parse("<error>✖ <bold>Ultra" + plugin.module() + "</bold></error> <sep>»</sep> <errortext>" + raw(sender, key), pairs));
        if (sender instanceof Player player) sound(player, "error");
    }

    /** Plays one of the sounds from config.yml (success, error, tick, teleport, click). */
    public void sound(Player player, String type) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("sounds.enabled", true)) return;
        String key = config.getString("sounds." + type + ".key", "");
        if (key.isEmpty()) return;
        player.playSound(player.getLocation(), key, SoundCategory.MASTER, 1f, (float) config.getDouble("sounds." + type + ".pitch", 1.0));
    }

    /** Sends any component to a player or the console. */
    public void send(CommandSender sender, Component component) {
        audiences.sender(sender).sendMessage(component);
    }

    /** Sends a component to everyone online and the console. */
    public void broadcast(Component component) {
        audiences.all().sendMessage(component);
    }
}

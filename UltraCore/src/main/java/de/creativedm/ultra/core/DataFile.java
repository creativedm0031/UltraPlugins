package de.creativedm.ultra.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/** A YAML data file that is written off the main thread. */
public final class DataFile {

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Object lock = new Object();

    public DataFile(Plugin plugin, String name) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), name);
        this.yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    public YamlConfiguration yaml() {
        return yaml;
    }

    public void setLocation(String path, org.bukkit.Location location) {
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".x", location.getX());
        yaml.set(path + ".y", location.getY());
        yaml.set(path + ".z", location.getZ());
        yaml.set(path + ".yaw", (double) location.getYaw());
        yaml.set(path + ".pitch", (double) location.getPitch());
    }

    /** @return null if nothing is stored or the world is not loaded */
    public org.bukkit.Location getLocation(String path) {
        String name = yaml.getString(path + ".world");
        org.bukkit.World world = name == null ? null : org.bukkit.Bukkit.getWorld(name);
        if (world == null) return null;
        return new org.bukkit.Location(world, yaml.getDouble(path + ".x"), yaml.getDouble(path + ".y"), yaml.getDouble(path + ".z"),
                (float) yaml.getDouble(path + ".yaw"), (float) yaml.getDouble(path + ".pitch"));
    }

    public void save() {
        String content = yaml.saveToString();
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(content));
        else write(content);
    }

    private void write(String content) {
        synchronized (lock) {
            try {
                Path target = file.toPath();
                Files.createDirectories(target.getParent());
                Path temp = target.resolveSibling(file.getName() + ".tmp");
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
            }
        }
    }
}

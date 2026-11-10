package de.creativedm.ultra.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Optional costs through Vault. Without Vault and an economy plugin everything is free. */
public final class Eco {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private Eco() {}

    public static boolean available() {
        return Bukkit.getPluginManager().getPlugin("Vault") != null && Hook.economy() != null;
    }

    public static boolean has(Player player, double amount) {
        return amount <= 0 || !available() || Hook.economy().has(player, amount);
    }

    /** @return false if the player cannot pay */
    public static boolean charge(Player player, double amount) {
        if (amount <= 0 || !available()) return true;
        return Hook.economy().withdrawPlayer(player, amount).transactionSuccess();
    }

    public static String format(double amount) {
        return available() ? Hook.economy().format(amount) : FORMAT.format(amount);
    }

    /** Only loaded when Vault is installed. */
    private static final class Hook {
        static Economy economy() {
            RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
            return provider == null ? null : provider.getProvider();
        }
    }
}

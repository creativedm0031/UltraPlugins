package de.creativedm.ultra.economy;

import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

/** Makes the default currency available to every plugin that uses Vault. Only loaded when Vault is installed. */
final class VaultHook extends AbstractEconomy {

    private final UltraEconomy plugin;

    private VaultHook(UltraEconomy plugin) {
        this.plugin = plugin;
    }

    static void register(UltraEconomy plugin) {
        Bukkit.getServicesManager().register(Economy.class, new VaultHook(plugin), plugin, ServicePriority.Highest);
    }

    private String currency() {
        return plugin.defaultCurrency();
    }

    @SuppressWarnings("deprecation")
    private static UUID id(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    private EconomyResponse change(UUID player, String name, double amount, boolean withdraw) {
        double balance = plugin.balance(player, currency());
        if (amount < 0) return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Negative amount");
        if (withdraw && balance < amount) return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        double result = withdraw ? balance - amount : balance + amount;
        plugin.set(player, currency(), result);
        plugin.log(withdraw ? "VAULT_WITHDRAW" : "VAULT_DEPOSIT", withdraw ? name : "Vault", withdraw ? "Vault" : name, currency(), amount);
        return new EconomyResponse(amount, plugin.balance(player, currency()), EconomyResponse.ResponseType.SUCCESS, null);
    }

    private static EconomyResponse noBanks() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "UltraEconomy has no banks");
    }

    @Override public boolean isEnabled() { return plugin.isEnabled(); }
    @Override public String getName() { return "UltraEconomy"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return plugin.decimals(currency()); }
    @Override public String format(double amount) { return plugin.format(currency(), amount); }
    @Override public String currencyNamePlural() { return currency(); }
    @Override public String currencyNameSingular() { return currency(); }

    @Override public boolean hasAccount(OfflinePlayer player) { return plugin.hasAccount(player.getUniqueId()); }
    @Override public boolean hasAccount(String name) { return plugin.hasAccount(id(name)); }
    @Override public boolean hasAccount(String name, String world) { return hasAccount(name); }

    @Override public double getBalance(OfflinePlayer player) { return plugin.balance(player.getUniqueId(), currency()); }
    @Override public double getBalance(String name) { return plugin.balance(id(name), currency()); }
    @Override public double getBalance(String name, String world) { return getBalance(name); }

    @Override public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
    @Override public boolean has(String name, double amount) { return getBalance(name) >= amount; }
    @Override public boolean has(String name, String world, double amount) { return has(name, amount); }

    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return change(player.getUniqueId(), String.valueOf(player.getName()), amount, true);
    }
    @Override public EconomyResponse withdrawPlayer(String name, double amount) { return change(id(name), name, amount, true); }
    @Override public EconomyResponse withdrawPlayer(String name, String world, double amount) { return withdrawPlayer(name, amount); }

    @Override public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return change(player.getUniqueId(), String.valueOf(player.getName()), amount, false);
    }
    @Override public EconomyResponse depositPlayer(String name, double amount) { return change(id(name), name, amount, false); }
    @Override public EconomyResponse depositPlayer(String name, String world, double amount) { return depositPlayer(name, amount); }

    @Override public boolean createPlayerAccount(OfflinePlayer player) {
        if (plugin.hasAccount(player.getUniqueId())) return false;
        plugin.set(player.getUniqueId(), currency(), plugin.balance(player.getUniqueId(), currency()));
        return true;
    }
    @SuppressWarnings("deprecation")
    @Override public boolean createPlayerAccount(String name) { return createPlayerAccount(Bukkit.getOfflinePlayer(name)); }
    @Override public boolean createPlayerAccount(String name, String world) { return createPlayerAccount(name); }

    @Override public EconomyResponse createBank(String name, String player) { return noBanks(); }
    @Override public EconomyResponse deleteBank(String name) { return noBanks(); }
    @Override public EconomyResponse bankBalance(String name) { return noBanks(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return noBanks(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return noBanks(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return noBanks(); }
    @Override public EconomyResponse isBankOwner(String name, String player) { return noBanks(); }
    @Override public EconomyResponse isBankMember(String name, String player) { return noBanks(); }
    @Override public List<String> getBanks() { return List.of(); }
}

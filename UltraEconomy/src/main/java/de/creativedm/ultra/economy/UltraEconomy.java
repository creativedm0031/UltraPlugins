package de.creativedm.ultra.economy;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Menu;
import de.creativedm.ultra.core.UltraPlugin;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UltraEconomy extends UltraPlugin implements Listener {

    private record Payment(UUID target, String currency, double amount, long expires) {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final Map<UUID, Payment> pending = new HashMap<>();
    private final Object logLock = new Object();
    private DataFile data;

    @Override
    public String module() {
        return "Economy";
    }

    @Override
    protected void enable() {
        data = new DataFile(this, "balances.yml");
        getServer().getPluginManager().registerEvents(this, this);
        Completer amounts = (s, a) -> a.length == 1 ? null : a.length == 3 ? currencies() : List.of();
        command("balance", "ultraeconomy.balance", false, this::balanceCommand, (s, a) -> a.length == 1 ? null : List.of());
        command("pay", "ultraeconomy.pay", true, (s, a) -> pay((Player) s, a), amounts);
        command("baltop", "ultraeconomy.baltop", true, (s, a) -> openTop((Player) s, defaultCurrency(), 0), (s, a) -> List.of());
        command("eco", "ultraeconomy.eco", false, this::eco,
                (s, a) -> a.length == 1 ? List.of("give", "take", "set") : a.length == 2 ? null : a.length == 4 ? currencies() : List.of());
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            VaultHook.register(this);
            getLogger().info("Registered as Vault economy (currency: " + defaultCurrency() + ")");
        }
    }

    @Override
    protected List<String> adminSubcommands() {
        return List.of("log");
    }

    @Override
    protected boolean adminCommand(CommandSender sender, String[] args) {
        if (!args[0].equalsIgnoreCase("log")) return false;
        if (!sender.hasPermission("ultraeconomy.log")) msg().error(sender, "no-permission");
        else if (args.length < 2 || !(sender instanceof Player player)) msg().error(sender, "usage", "usage", "/ultraeconomy log <player>");
        else openLog(player, args[1], 0);
        return true;
    }

    // ---- accounts -------------------------------------------------------------------------------------------

    List<String> currencies() {
        ConfigurationSection section = getConfig().getConfigurationSection("currencies");
        return section == null || section.getKeys(false).isEmpty() ? List.of("coins") : new ArrayList<>(section.getKeys(false));
    }

    /** The first currency in config.yml. It is the one Vault and other plugins use. */
    String defaultCurrency() {
        return currencies().get(0);
    }

    int decimals(String currency) {
        return Math.max(0, Math.min(4, getConfig().getInt("currencies." + currency + ".decimals", 2)));
    }

    double round(String currency, double amount) {
        return BigDecimal.valueOf(amount).setScale(decimals(currency), RoundingMode.HALF_UP).doubleValue();
    }

    String format(String currency, double amount) {
        int decimals = decimals(currency);
        DecimalFormat format = new DecimalFormat(decimals == 0 ? "#,##0" : "#,##0." + "0".repeat(decimals), DecimalFormatSymbols.getInstance(Locale.US));
        String symbol = getConfig().getString("currencies." + currency + ".symbol", "$");
        return getConfig().getBoolean("currencies." + currency + ".symbol-before", true) ? symbol + format.format(amount) : format.format(amount) + symbol;
    }

    boolean hasAccount(UUID player) {
        return data.yaml().contains(player.toString());
    }

    double balance(UUID player, String currency) {
        return data.yaml().getDouble(player + "." + currency, getConfig().getDouble("currencies." + currency + ".start"));
    }

    void set(UUID player, String currency, double amount) {
        data.yaml().set(player + "." + currency, round(currency, Math.max(0, amount)));
        data.save();
    }

    /** One line per booking in transactions.log: time, type, from, to, currency, amount. */
    void log(String type, String from, String to, String currency, double amount) {
        String line = System.currentTimeMillis() + "|" + type + "|" + from + "|" + to + "|" + currency + "|" + amount + "\n";
        Runnable write = () -> {
            synchronized (logLock) {
                try {
                    Files.createDirectories(getDataFolder().toPath());
                    Files.writeString(new File(getDataFolder(), "transactions.log").toPath(), line, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    getLogger().warning("Could not write transactions.log: " + e.getMessage());
                }
            }
        };
        if (isEnabled()) getServer().getScheduler().runTaskAsynchronously(this, write);
        else write.run();
    }

    private UUID find(CommandSender sender, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        for (String key : data.yaml().getKeys(false)) {
            if (name.equalsIgnoreCase(data.yaml().getString(key + ".name"))) return UUID.fromString(key);
        }
        msg().error(sender, "player-unknown", "player", name);
        return null;
    }

    String name(UUID player) {
        return data.yaml().getString(player + ".name", player.toString().substring(0, 8));
    }

    private String currency(CommandSender sender, String[] args, int index) {
        if (args.length <= index) return defaultCurrency();
        String currency = args[index].toLowerCase(Locale.ROOT);
        if (currencies().contains(currency)) return currency;
        msg().error(sender, "currency-unknown", "currency", currency);
        return null;
    }

    private double amount(CommandSender sender, String text, String currency) {
        try {
            double amount = round(currency, Double.parseDouble(text.replace(",", ".")));
            if (amount > 0 && !Double.isInfinite(amount) && !Double.isNaN(amount)) return amount;
        } catch (NumberFormatException ignored) {
            // reported below
        }
        msg().error(sender, "invalid-number", "value", text);
        return -1;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String id = event.getPlayer().getUniqueId().toString();
        if (event.getPlayer().getName().equals(data.yaml().getString(id + ".name"))) return;
        boolean created = !data.yaml().contains(id);
        data.yaml().set(id + ".name", event.getPlayer().getName());
        if (created) for (String currency : currencies()) data.yaml().set(id + "." + currency, getConfig().getDouble("currencies." + currency + ".start"));
        data.save();
    }

    // ---- commands -------------------------------------------------------------------------------------------

    private void balanceCommand(CommandSender sender, String[] args) {
        UUID target;
        if (args.length > 0 && sender.hasPermission("ultraeconomy.balance.others")) target = find(sender, args[0]);
        else if (sender instanceof Player player) target = player.getUniqueId();
        else {
            usage(sender, "balance");
            return;
        }
        if (target == null) return;
        List<String> parts = new ArrayList<>();
        for (String currency : currencies()) parts.add(format(currency, balance(target, currency)));
        boolean own = sender instanceof Player player && player.getUniqueId().equals(target);
        msg().info(sender, own ? "balance" : "balance-other", "player", name(target), "balance", String.join(" · ", parts));
    }

    private void pay(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            Payment payment = pending.remove(player.getUniqueId());
            if (payment == null || payment.expires() < System.currentTimeMillis()) msg().error(player, "pay-nothing-to-confirm");
            else transfer(player, payment.target(), payment.currency(), payment.amount());
            return;
        }
        if (args.length < 2) {
            usage(player, "pay");
            return;
        }
        UUID target = find(player, args[0]);
        String currency = target == null ? null : currency(player, args, 2);
        if (currency == null) return;
        double amount = amount(player, args[1], currency);
        if (amount < 0) return;
        if (target.equals(player.getUniqueId())) {
            msg().error(player, "pay-self");
            return;
        }
        if (amount < getConfig().getDouble("pay.minimum", 1)) {
            msg().error(player, "pay-minimum", "amount", format(currency, getConfig().getDouble("pay.minimum", 1)));
            return;
        }
        double threshold = getConfig().getDouble("pay.confirm-above");
        if (threshold > 0 && amount >= threshold) {
            pending.put(player.getUniqueId(), new Payment(target, currency, amount, System.currentTimeMillis() + 30_000));
            // Needs a click, so this one line goes to the chat.
            msg().chat(player, "pay-confirm", "amount", format(currency, amount), "player", name(target));
            return;
        }
        transfer(player, target, currency, amount);
    }

    private void transfer(Player player, UUID target, String currency, double amount) {
        double balance = balance(player.getUniqueId(), currency);
        if (balance < amount) {
            msg().error(player, "insufficient-funds", "amount", format(currency, amount - balance));
            return;
        }
        set(player.getUniqueId(), currency, balance - amount);
        set(target, currency, balance(target, currency) + amount);
        log("PAY", player.getName(), name(target), currency, amount);
        msg().info(player, "pay-sent", "amount", format(currency, amount), "player", name(target));
        Player receiver = Bukkit.getPlayer(target);
        if (receiver != null) msg().info(receiver, "pay-received", "amount", format(currency, amount), "player", player.getName());
    }

    private void eco(CommandSender sender, String[] args) {
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (args.length < 3 || !List.of("give", "take", "set").contains(action)) {
            usage(sender, "eco");
            return;
        }
        UUID target = find(sender, args[1]);
        String currency = target == null ? null : currency(sender, args, 3);
        if (currency == null) return;
        double amount = action.equals("set") && args[2].equals("0") ? 0 : amount(sender, args[2], currency);
        if (amount < 0) return;
        double balance = balance(target, currency);
        double result = action.equals("give") ? balance + amount : action.equals("take") ? Math.max(0, balance - amount) : amount;
        set(target, currency, result);
        log(action.toUpperCase(Locale.ROOT), sender.getName(), name(target), currency, amount);
        msg().info(sender, "eco-" + action, "player", name(target), "amount", format(currency, amount), "balance", format(currency, result));
    }

    // ---- GUIs -----------------------------------------------------------------------------------------------

    private void openTop(Player viewer, String currency, int page) {
        List<String> ids = new ArrayList<>(data.yaml().getKeys(false));
        ids.sort(Comparator.comparingDouble((String id) -> balance(UUID.fromString(id), currency)).reversed());
        List<Menu.Entry> entries = new ArrayList<>();
        int limit = Math.min(ids.size(), getConfig().getInt("baltop-size", 56));
        for (int i = 0; i < limit; i++) {
            UUID id = UUID.fromString(ids.get(i));
            entries.add(new Menu.Entry(Menu.head(Bukkit.getOfflinePlayer(id), msg().parse("<brand>#<rank> <player></brand>", "rank", i + 1, "player", name(id)),
                    msg().lore(viewer, "baltop-lore", "balance", format(currency, balance(id, currency)))), null));
        }
        Menu menu = Menu.paged(this, viewer, msg().title(viewer, "baltop-title", "currency", currency), entries, page,
                next -> openTop(viewer, currency, next), null);
        List<String> currencies = currencies();
        if (currencies.size() > 1) {
            menu.set(47, Menu.item(Material.GOLD_INGOT, msg().text(viewer, "currency-button"), msg().lore(viewer, "currency-lore", "currency", currency)),
                    click -> openTop(viewer, currencies.get((currencies.indexOf(currency) + 1) % currencies.size()), 0));
        }
        menu.open(viewer);
    }

    private void openLog(Player viewer, String playerName, int page) {
        List<String> lines;
        try {
            File file = new File(getDataFolder(), "transactions.log");
            lines = file.exists() ? Files.readAllLines(file.toPath(), StandardCharsets.UTF_8) : List.of();
        } catch (IOException e) {
            lines = List.of();
        }
        List<Menu.Entry> entries = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0 && entries.size() < 280; i--) {
            String[] p = lines.get(i).split("\\|");
            if (p.length < 6 || !(p[2].equalsIgnoreCase(playerName) || p[3].equalsIgnoreCase(playerName))) continue;
            entries.add(new Menu.Entry(Menu.item(Material.PAPER, msg().parse("<brand><type></brand>", "type", p[1]),
                    msg().lore(viewer, "log-lore", "from", p[2], "to", p[3], "amount", format(p[4], Double.parseDouble(p[5])),
                            "date", DATE.format(Instant.ofEpochMilli(Long.parseLong(p[0]))))), null));
        }
        if (entries.isEmpty()) {
            msg().error(viewer, "log-empty", "player", playerName);
            return;
        }
        Menu.paged(this, viewer, msg().title(viewer, "log-title", "player", playerName), entries, page, next -> openLog(viewer, playerName, next), null).open(viewer);
    }
}

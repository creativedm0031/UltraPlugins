package de.creativedm.ultra.ranks;

import de.creativedm.ultra.core.Durations;
import de.creativedm.ultra.core.Eco;
import de.creativedm.ultra.core.Menu;
import de.creativedm.ultra.core.Messages;
import de.creativedm.ultra.core.UltraPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

public final class UltraRanks extends UltraPlugin implements Listener {

    private RankManager ranks;
    private Gui gui;
    private Input input;
    private NamespacedKey voucherRank;
    private NamespacedKey voucherDuration;
    private BukkitTask expiryTask;

    @Override
    public String module() {
        return "Ranks";
    }

    RankManager ranks() {
        return ranks;
    }

    Input input() {
        return input;
    }

    @Override
    protected void enable() {
        ranks = new RankManager(this);
        gui = new Gui(this, ranks);
        input = new Input(this);
        voucherRank = new NamespacedKey(this, "voucher_rank");
        voucherDuration = new NamespacedKey(this, "voucher_duration");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(input, this);

        command("ranks", "ultraranks.use", true, (s, a) -> gui.openUser((Player) s, 0), (s, a) -> List.of());
        command("rankadmin", "ultraranks.manage", true, (s, a) -> gui.openAdmin((Player) s, 0), (s, a) -> List.of());
        command("rank", "ultraranks.manage", false, this::rankCommand, this::rankComplete);

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            VaultHook.register(this);
            getLogger().info("Registered permissions and prefixes in Vault");
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            getLogger().info("Registered PlaceholderAPI placeholders %ultraranks_...%");
        }
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            getLogger().warning("LuckPerms is installed as well. Import it with /ultraranks import, then remove LuckPerms – two permission plugins fight each other.");
        }
        ranks.refreshAll();
        startExpiryTask();
    }

    @Override
    protected void reloaded() {
        ranks.refreshAll();
        startExpiryTask();
    }

    @Override
    protected void disable() {
        if (ranks != null) ranks.cleanup();
    }

    private void startExpiryTask() {
        if (expiryTask != null) expiryTask.cancel();
        long ticks = Math.max(5, getConfig().getInt("expiry-check-seconds", 30)) * 20L;
        expiryTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) checkExpiry(player);
        }, ticks, ticks);
    }

    private void checkExpiry(Player player) {
        Rank back = ranks.expire(player.getUniqueId());
        if (back == null) return;
        ranks.refresh(player);
        ranks.log("Server", "expire", player.getName(), back.id);
        msg().error(player, "rank-expired", "rank", msg().parse(back.display));
    }

    @Override
    protected List<String> adminSubcommands() {
        return List.of("import");
    }

    @Override
    protected boolean adminCommand(CommandSender sender, String[] args) {
        if (!args[0].equalsIgnoreCase("import")) return false;
        if (!getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            msg().error(sender, "import-no-luckperms");
            return true;
        }
        msg().info(sender, "import-started");
        LuckPermsImport.run(this, sender);
        return true;
    }

    // ---- /rank ----------------------------------------------------------------------------------------------

    private void rankCommand(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set" -> setCommand(sender, args);
            case "info" -> infoCommand(sender, args);
            case "list" -> {
                List<String> names = new ArrayList<>();
                for (Rank rank : ranks.sorted()) names.add(rank.id + " (" + rank.weight + ")");
                msg().info(sender, "rank-list", "ranks", String.join(", ", names));
            }
            case "create" -> {
                if (args.length < 2) {
                    usage(sender, "rank");
                    return;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (!id.matches("[a-z0-9_-]{1,24}")) msg().error(sender, "rank-invalid-id");
                else if (ranks.rank(id) != null) msg().error(sender, "rank-exists", "rank", id);
                else {
                    ranks.create(id);
                    ranks.log(sender.getName(), "create", id, "");
                    msg().info(sender, "rank-created", "rank", id);
                }
            }
            case "delete" -> {
                Rank rank = args.length < 2 ? null : rankArg(sender, args[1]);
                if (rank == null) return;
                if (rank == ranks.defaultRank()) {
                    msg().error(sender, "rank-delete-default");
                    return;
                }
                ranks.delete(rank);
                ranks.log(sender.getName(), "delete", rank.id, "");
                msg().info(sender, "rank-deleted", "rank", rank.id);
            }
            case "perm" -> permCommand(sender, args);
            case "parent" -> parentCommand(sender, args);
            case "voucher" -> voucherCommand(sender, args);
            default -> usage(sender, "rank");
        }
    }

    private Rank rankArg(CommandSender sender, String id) {
        Rank rank = ranks.rank(id);
        if (rank == null) msg().error(sender, "rank-unknown", "rank", id);
        return rank;
    }

    /** /rank set <player> <rank> [duration] */
    private void setCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "rank");
            return;
        }
        UUID id = ranks.find(args[1]);
        if (id == null) {
            msg().error(sender, "player-never-joined", "player", args[1]);
            return;
        }
        Rank rank = rankArg(sender, args[2]);
        if (rank == null) return;
        long until = 0;
        if (args.length > 3 && !args[3].equalsIgnoreCase("permanent")) {
            long millis = Durations.parse(args[3]);
            if (millis < 0) {
                msg().error(sender, "invalid-duration", "value", args[3]);
                return;
            }
            until = System.currentTimeMillis() + millis;
        }
        String name = ranks.name(id);
        ranks.set(id, name, rank, until);
        String shown = until == 0 ? msg().raw(sender, "permanent") : args[3];
        ranks.log(sender.getName(), "set", name, rank.id + (until == 0 ? "" : " (" + args[3] + ")"));
        msg().info(sender, "rank-set", "player", name, "rank", msg().parse(rank.display), "duration", shown);
        Player online = Bukkit.getPlayer(id);
        if (online != null && online != sender) msg().info(online, "rank-received", "rank", msg().parse(rank.display), "duration", until == 0 ? msg().raw(online, "permanent") : args[3]);
    }

    private void infoCommand(CommandSender sender, String[] args) {
        UUID id = args.length > 1 ? ranks.find(args[1]) : sender instanceof Player p ? p.getUniqueId() : null;
        if (id == null) {
            msg().error(sender, "player-never-joined", "player", args.length > 1 ? args[1] : "?");
            return;
        }
        RankManager.Assignment a = ranks.assignment(id);
        Rank rank = ranks.rank(a.rank());
        msg().info(sender, "rank-info", "player", ranks.name(id), "rank", rank == null ? Component.text("–") : msg().parse(rank.display),
                "duration", a.until() == 0 ? msg().raw(sender, "permanent") : Durations.format(a.until() - System.currentTimeMillis()));
    }

    /** /rank perm <rank> <permission> [true|false|remove] */
    private void permCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "rank");
            return;
        }
        Rank rank = rankArg(sender, args[1]);
        if (rank == null) return;
        String perm = args[2].toLowerCase(Locale.ROOT);
        String mode = args.length > 3 ? args[3].toLowerCase(Locale.ROOT) : "true";
        switch (mode) {
            case "remove" -> {
                rank.permissions.remove(perm);
                ranks.log(sender.getName(), "perm-remove", rank.id, perm);
                msg().info(sender, "perm-removed", "perm", perm, "rank", msg().parse(rank.display));
            }
            case "false" -> {
                rank.permissions.put(perm, false);
                ranks.log(sender.getName(), "perm-negate", rank.id, perm);
                msg().info(sender, "perm-negated", "perm", perm, "rank", msg().parse(rank.display));
            }
            default -> {
                rank.permissions.put(perm, true);
                ranks.log(sender.getName(), "perm-add", rank.id, perm);
                msg().info(sender, "perm-added", "perm", perm, "rank", msg().parse(rank.display));
            }
        }
        ranks.save();
        ranks.refreshAll();
    }

    /** /rank parent <rank> <add|remove> <parent> */
    private void parentCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender, "rank");
            return;
        }
        Rank rank = rankArg(sender, args[1]);
        Rank parent = rank == null ? null : rankArg(sender, args[3]);
        if (parent == null) return;
        if (args[2].equalsIgnoreCase("remove")) {
            rank.parents.remove(parent.id);
            ranks.log(sender.getName(), "parent-remove", rank.id, parent.id);
        } else if (rank == parent || ranks.wouldLoop(rank, parent)) {
            msg().error(sender, "parent-loop", "rank", parent.id);
            return;
        } else if (!rank.parents.contains(parent.id)) {
            rank.parents.add(parent.id);
            ranks.log(sender.getName(), "parent-add", rank.id, parent.id);
        }
        ranks.save();
        ranks.refreshAll();
        msg().info(sender, "rank-updated", "rank", msg().parse(rank.display));
    }

    /** /rank voucher <rank> [duration|permanent] [player] [amount] */
    private void voucherCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "rank");
            return;
        }
        Rank rank = rankArg(sender, args[1]);
        if (rank == null) return;
        long duration = 0;
        if (args.length > 2 && !args[2].equalsIgnoreCase("permanent")) {
            duration = Durations.parse(args[2]);
            if (duration < 0) {
                msg().error(sender, "invalid-duration", "value", args[2]);
                return;
            }
        }
        Player target = args.length > 3 ? player(sender, args[3]) : sender instanceof Player p ? p : null;
        if (target == null) {
            if (args.length <= 3) usage(sender, "rank");
            return;
        }
        int amount = 1;
        if (args.length > 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[4])));
            } catch (NumberFormatException e) {
                msg().error(sender, "invalid-number", "value", args[4]);
                return;
            }
        }
        ItemStack item = voucher(target, rank, duration);
        item.setAmount(amount);
        target.getInventory().addItem(item).values().forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        String shown = duration == 0 ? msg().raw(sender, "permanent") : Durations.format(duration);
        ranks.log(sender.getName(), "voucher", rank.id, shown + " → " + target.getName());
        msg().info(sender, "voucher-given", "rank", msg().parse(rank.display), "player", target.getName());
    }

    private List<String> rankComplete(CommandSender sender, String[] args) {
        List<String> ids = new ArrayList<>();
        for (Rank rank : ranks.sorted()) ids.add(rank.id);
        if (args.length == 1) return List.of("set", "info", "list", "create", "delete", "perm", "parent", "voucher");
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "set" -> args.length == 2 ? null : args.length == 3 ? ids : args.length == 4 ? List.of("permanent", "1d", "7d", "30d") : List.of();
            case "info" -> args.length == 2 ? null : List.of();
            case "delete" -> args.length == 2 ? ids : List.of();
            case "perm" -> args.length == 2 ? ids : args.length == 3 ? knownPermissions(args[2])
                    : args.length == 4 ? List.of("true", "false", "remove") : List.of();
            case "parent" -> args.length == 2 || args.length == 4 ? ids : args.length == 3 ? List.of("add", "remove") : List.of();
            case "voucher" -> args.length == 2 ? ids : args.length == 3 ? List.of("permanent", "7d", "30d") : args.length == 4 ? null : List.of();
            default -> List.of();
        };
    }

    private List<String> knownPermissions(String typed) {
        List<String> out = new ArrayList<>();
        for (var perm : Bukkit.getPluginManager().getPermissions()) {
            if (perm.getName().startsWith(typed.toLowerCase(Locale.ROOT)) && out.size() < 80) out.add(perm.getName());
        }
        return out;
    }

    // ---- vouchers -------------------------------------------------------------------------------------------

    ItemStack voucher(Player viewer, Rank rank, long duration) {
        String shown = duration == 0 ? msg().raw(viewer, "permanent") : Durations.format(duration);
        ItemStack item = Menu.item(Menu.material(getConfig().getString("voucher.material"), Material.PAPER),
                msg().text(viewer, "voucher-name", "rank", msg().parse(rank.display)), msg().lore(viewer, "voucher-lore", "duration", shown));
        var meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(voucherRank, PersistentDataType.STRING, rank.id);
        data.set(voucherDuration, PersistentDataType.LONG, duration);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onVoucher(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String id = data.get(voucherRank, PersistentDataType.STRING);
        if (id == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Rank rank = ranks.rank(id);
        if (rank == null) {
            msg().error(player, "rank-unknown", "rank", id);
            return;
        }
        Long duration = data.get(voucherDuration, PersistentDataType.LONG);
        if (!grant(player, rank, duration == null ? 0 : duration)) return;
        item.setAmount(item.getAmount() - 1);
        ranks.log(player.getName(), "redeem", rank.id, duration == null || duration == 0 ? msg().raw(player, "permanent") : Durations.format(duration));
    }

    /**
     * Gives a rank from a voucher or purchase. A lower rank is refused; the same temporary rank is extended.
     * @return false if the player keeps their current rank
     */
    private boolean grant(Player player, Rank rank, long duration) {
        RankManager.Assignment mine = ranks.assignment(player.getUniqueId());
        Rank current = ranks.rank(mine.rank());
        if (current != null && current != rank && current.weight >= rank.weight && mine.until() == 0) {
            msg().error(player, "rank-already-higher", "rank", msg().parse(current.display));
            return false;
        }
        if (current == rank && mine.until() == 0) {
            msg().error(player, "rank-already-have", "rank", msg().parse(rank.display));
            return false;
        }
        long until = 0;
        if (duration > 0) {
            long start = current == rank && mine.until() > System.currentTimeMillis() ? mine.until() : System.currentTimeMillis();
            until = start + duration;
        }
        ranks.set(player.getUniqueId(), player.getName(), rank, until);
        msg().info(player, "rank-received", "rank", msg().parse(rank.display), "duration", until == 0 ? msg().raw(player, "permanent") : Durations.format(until - System.currentTimeMillis()));
        return true;
    }

    // ---- buying ---------------------------------------------------------------------------------------------

    boolean canBuy(Player player, Rank rank) {
        if (rank.price <= 0 || !getConfig().getBoolean("purchase.enabled", true) || !Eco.available()) return false;
        RankManager.Assignment mine = ranks.assignment(player.getUniqueId());
        Rank current = ranks.rank(mine.rank());
        if (current == null) return true;
        if (current == rank) return mine.until() > 0; // extend a temporary rank
        return rank.weight > current.weight || mine.until() > 0;
    }

    void buy(Player player, Rank rank) {
        if (!canBuy(player, rank)) {
            msg().error(player, "rank-not-buyable");
            return;
        }
        if (!Eco.charge(player, rank.price)) {
            msg().error(player, "not-enough-money", "amount", Eco.format(rank.price));
            return;
        }
        long duration = rank.purchaseDuration.equalsIgnoreCase("permanent") ? 0 : Math.max(0, Durations.parse(rank.purchaseDuration));
        if (grant(player, rank, duration)) ranks.log(player.getName(), "buy", rank.id, Eco.format(rank.price));
    }

    // ---- players --------------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ranks.remember(player);
        checkExpiry(player);
        ranks.refresh(player);
        String header = getConfig().getString("tablist.header", "");
        String footer = getConfig().getString("tablist.footer", "");
        if (!header.isEmpty() || !footer.isEmpty()) updateHeaders();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ranks.forget(event.getPlayer());
        getServer().getScheduler().runTask(this, this::updateHeaders);
    }

    /** Optional tab list header and footer from config.yml. */
    private void updateHeaders() {
        String header = getConfig().getString("tablist.header", "");
        String footer = getConfig().getString("tablist.footer", "");
        if (header.isEmpty() && footer.isEmpty()) return;
        String online = String.valueOf(Bukkit.getOnlinePlayers().size());
        String max = String.valueOf(Bukkit.getMaxPlayers());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(
                    Messages.legacy(msg().parse(header.replace("\\n", "\n"), "online", online, "max", max, "player", player.getName())),
                    Messages.legacy(msg().parse(footer.replace("\\n", "\n"), "online", online, "max", max, "player", player.getName())));
        }
    }

    /** Chat format – only when UltraChat is not installed; UltraChat then shows the rank itself. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!getConfig().getBoolean("chat.enabled", true) || getServer().getPluginManager().isPluginEnabled("UltraChat")) return;
        Player player = event.getPlayer();
        String text = event.getMessage();
        Component message = player.hasPermission("ultraranks.chat.color")
                ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(text) : Component.text(text);
        Component line = msg().parse(getConfig().getString("chat.format", "<prefix><white><player></white><suffix> <sep>»</sep> <white><message>"),
                "prefix", msg().parse(meta(player, "ultraranks_prefix")), "suffix", msg().parse(meta(player, "ultraranks_suffix")),
                "rank", msg().parse(meta(player, "ultraranks_rank")), "player", player.getName(), "message", message);
        for (Player receiver : event.getRecipients()) msg().send(receiver, line);
        event.getRecipients().clear();
        event.setFormat("%1$s: %2$s");
    }

    /** Rank texts set on the player when the rank changes – safe to read from the chat thread. */
    static String meta(Player player, String key) {
        for (var value : player.getMetadata(key)) return value.asString();
        return "";
    }

    /** Used by Vault and PlaceholderAPI. */
    Map<String, Boolean> effectivePermissions(UUID player) {
        Rank rank = ranks.rankOf(player);
        return rank == null ? Map.of() : ranks.permissions(rank);
    }
}

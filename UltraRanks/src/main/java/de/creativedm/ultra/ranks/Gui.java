package de.creativedm.ultra.ranks;

import de.creativedm.ultra.core.Durations;
import de.creativedm.ultra.core.Eco;
import de.creativedm.ultra.core.Menu;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** All menus: the admin GUI (/rankadmin) and the player GUI (/ranks). */
final class Gui {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final List<ChatColor> COLORS = List.of(ChatColor.WHITE, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.BLACK,
            ChatColor.RED, ChatColor.DARK_RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.DARK_GREEN,
            ChatColor.AQUA, ChatColor.DARK_AQUA, ChatColor.BLUE, ChatColor.DARK_BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE);

    private final UltraRanks plugin;
    private final RankManager ranks;

    Gui(UltraRanks plugin, RankManager ranks) {
        this.plugin = plugin;
        this.ranks = ranks;
    }

    private Component parse(String miniMessage) {
        return plugin.msg().parse(miniMessage == null ? "" : miniMessage);
    }

    private ItemStack button(Player player, Material material, String nameKey, String loreKey, Object... pairs) {
        return Menu.item(material, plugin.msg().text(player, nameKey), loreKey == null ? List.of() : plugin.msg().lore(player, loreKey, pairs));
    }

    private String duration(Player viewer, String value) {
        return value == null || value.equalsIgnoreCase("permanent") ? plugin.msg().raw(viewer, "permanent") : value;
    }

    private String expiry(Player viewer, long until) {
        return until == 0 ? plugin.msg().raw(viewer, "permanent") : Durations.format(until - System.currentTimeMillis());
    }

    private static Material wool(String color) {
        return switch (color) {
            case "GRAY" -> Material.LIGHT_GRAY_WOOL;
            case "DARK_GRAY" -> Material.GRAY_WOOL;
            case "BLACK" -> Material.BLACK_WOOL;
            case "RED" -> Material.RED_WOOL;
            case "DARK_RED" -> Material.BROWN_WOOL;
            case "GOLD" -> Material.ORANGE_WOOL;
            case "YELLOW" -> Material.YELLOW_WOOL;
            case "GREEN" -> Material.LIME_WOOL;
            case "DARK_GREEN" -> Material.GREEN_WOOL;
            case "AQUA" -> Material.LIGHT_BLUE_WOOL;
            case "DARK_AQUA" -> Material.CYAN_WOOL;
            case "BLUE" -> Material.BLUE_WOOL;
            case "DARK_BLUE" -> Material.BLUE_WOOL;
            case "LIGHT_PURPLE" -> Material.MAGENTA_WOOL;
            case "DARK_PURPLE" -> Material.PURPLE_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private String parentNames(Rank rank) {
        if (rank.parents.isEmpty()) return "–";
        List<String> names = new ArrayList<>();
        for (String id : rank.parents) names.add(id);
        return String.join(", ", names);
    }

    // =========================================================================================================
    // Admin
    // =========================================================================================================

    void openAdmin(Player player, int page) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (Rank rank : ranks.sorted()) {
            entries.add(new Menu.Entry(Menu.item(Menu.material(rank.icon, Material.NAME_TAG), parse(rank.display),
                    plugin.msg().lore(player, "admin-rank-lore", "id", rank.id, "weight", rank.weight,
                            "prefix", parse(rank.prefix), "suffix", parse(rank.suffix),
                            "members", ranks.members(rank).size(), "perms", rank.permissions.size(), "parents", parentNames(rank),
                            "default", plugin.msg().raw(player, rank == ranks.defaultRank() ? "yes" : "no"))),
                    click -> openEdit(player, rank)));
        }
        Menu menu = Menu.paged(plugin, player, plugin.msg().title(player, "admin-title"), entries, page, next -> openAdmin(player, next), null);
        menu.set(47, button(player, Material.EMERALD, "btn-create", "btn-create-lore"), click -> createRank(player));
        menu.set(51, button(player, Material.BOOK, "btn-log", "btn-log-lore"), click -> openLog(player, 0));
        menu.open(player);
    }

    private void createRank(Player player) {
        plugin.input().ask(player, "prompt-rank-id", "", text -> {
            String id = text.toLowerCase(Locale.ROOT).replace(' ', '_');
            if (!id.matches("[a-z0-9_-]{1,24}")) {
                plugin.msg().error(player, "rank-invalid-id");
                openAdmin(player, 0);
            } else if (ranks.rank(id) != null) {
                plugin.msg().error(player, "rank-exists", "rank", id);
                openAdmin(player, 0);
            } else {
                Rank rank = ranks.create(id);
                ranks.log(player.getName(), "create", id, "");
                plugin.msg().info(player, "rank-created", "rank", id);
                openEdit(player, rank);
            }
        }, () -> openAdmin(player, 0));
    }

    void openEdit(Player player, Rank rank) {
        if (ranks.rank(rank.id) == null) {
            openAdmin(player, 0);
            return;
        }
        Menu menu = new Menu(plugin.msg().title(player, "edit-title", "rank", parse(rank.display)), 6).frame();
        Component preview = plugin.msg().parse("<prefix><white><player></white><suffix>", "prefix", parse(rank.prefix), "suffix", parse(rank.suffix),
                "player", player.getName());
        menu.set(4, Menu.item(Menu.material(rank.icon, Material.NAME_TAG), parse(rank.display), plugin.msg().lore(player, "edit-preview-lore",
                "preview", preview, "id", rank.id)), null);

        menu.set(10, button(player, Material.NAME_TAG, "btn-display", "btn-text-lore", "value", parse(rank.display)),
                click -> edit(player, rank, "prompt-display", rank.display, value -> rank.display = value, "display"));
        menu.set(11, button(player, Material.OAK_SIGN, "btn-prefix", "btn-text-clear-lore", "value", parse(rank.prefix)), click -> {
            if (click.isRightClick()) set(player, rank, () -> rank.prefix = "", "prefix", "");
            else edit(player, rank, "prompt-prefix", rank.prefix, value -> rank.prefix = value, "prefix");
        });
        menu.set(12, button(player, Material.BIRCH_SIGN, "btn-suffix", "btn-text-clear-lore", "value", parse(rank.suffix)), click -> {
            if (click.isRightClick()) set(player, rank, () -> rank.suffix = "", "suffix", "");
            else edit(player, rank, "prompt-suffix", rank.suffix, value -> rank.suffix = value, "suffix");
        });
        menu.set(13, button(player, Material.EXPERIENCE_BOTTLE, "btn-weight", "btn-weight-lore", "value", rank.weight), click -> {
            int step = click.isShiftClick() ? 10 : 1;
            int value = rank.weight + (click.isRightClick() ? -step : step);
            set(player, rank, () -> rank.weight = value, "weight", String.valueOf(value));
        });
        menu.set(14, button(player, wool(rank.nameColor), "btn-color", "btn-color-lore", "value", rank.nameColor.toLowerCase(Locale.ROOT)), click -> {
            int index = Math.max(0, COLORS.indexOf(ChatColor.valueOf(safeColor(rank.nameColor))));
            int next = (index + (click.isRightClick() ? COLORS.size() - 1 : 1)) % COLORS.size();
            set(player, rank, () -> rank.nameColor = COLORS.get(next).name(), "name-color", COLORS.get(next).name());
        });
        menu.set(15, button(player, Menu.material(rank.icon, Material.NAME_TAG), "btn-icon", "btn-icon-lore"), click -> {
            Material hand = player.getInventory().getItemInMainHand().getType();
            if (hand.isAir()) {
                plugin.msg().error(player, "icon-empty-hand");
                return;
            }
            set(player, rank, () -> rank.icon = hand.name(), "icon", hand.name());
        });
        menu.set(16, button(player, Material.BOOK, "btn-description", "btn-text-lore", "value", parse(rank.description)),
                click -> edit(player, rank, "prompt-description", rank.description, value -> rank.description = value, "description"));

        menu.set(28, button(player, Material.WRITABLE_BOOK, "btn-perms", "btn-perms-lore", "count", rank.permissions.size()), click -> openPerms(player, rank, 0));
        menu.set(29, button(player, Material.CHAIN, "btn-parents", "btn-parents-lore", "value", parentNames(rank)), click -> openParents(player, rank));
        menu.set(30, button(player, Material.PLAYER_HEAD, "btn-members", "btn-members-lore", "count", ranks.members(rank).size()), click -> openMembers(player, rank, 0));
        menu.set(31, button(player, Material.GOLD_INGOT, "btn-price", "btn-price-lore", "value", rank.price > 0 ? Eco.format(rank.price) : plugin.msg().raw(player, "not-for-sale")), click -> {
            if (click.isRightClick()) set(player, rank, () -> rank.price = 0, "price", "0");
            else edit(player, rank, "prompt-price", rank.price > 0 ? String.valueOf(rank.price) : "", value -> {
                try {
                    rank.price = Math.max(0, Double.parseDouble(value.replace(",", ".")));
                } catch (NumberFormatException e) {
                    plugin.msg().error(player, "invalid-number", "value", value);
                }
            }, "price");
        });
        menu.set(32, button(player, Material.CLOCK, "btn-duration", "btn-duration-lore", "value", duration(player, rank.purchaseDuration)),
                click -> edit(player, rank, "prompt-duration", rank.purchaseDuration, value -> {
                    if (value.equalsIgnoreCase("permanent") || value.equals("0") || Durations.parse(value) > 0) rank.purchaseDuration = value.equals("0") ? "permanent" : value.toLowerCase(Locale.ROOT);
                    else plugin.msg().error(player, "invalid-duration", "value", value);
                }, "purchase-duration"));
        boolean isDefault = rank == ranks.defaultRank();
        menu.set(33, button(player, Material.NETHER_STAR, "btn-default", isDefault ? "btn-default-yes" : "btn-default-no"), click -> {
            if (isDefault) return;
            plugin.getConfig().set("default-rank", rank.id);
            plugin.saveConfig();
            ranks.log(player.getName(), "default", rank.id, "");
            ranks.refreshAll();
            plugin.msg().info(player, "rank-default-set", "rank", parse(rank.display));
            openEdit(player, rank);
        });
        menu.set(34, button(player, Material.PAPER, "btn-voucher", "btn-voucher-lore"), click -> {
            player.getInventory().addItem(plugin.voucher(player, rank, 0)).values()
                    .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            ranks.log(player.getName(), "voucher", rank.id, plugin.msg().raw(player, "permanent"));
            plugin.msg().info(player, "voucher-given", "rank", parse(rank.display), "player", player.getName());
        });

        menu.set(45, Menu.item(Material.ARROW, plugin.msg().text(player, "gui-back"), List.of()), click -> openAdmin(player, 0));
        menu.set(49, Menu.item(Material.BARRIER, plugin.msg().text(player, "gui-close"), List.of()), click -> player.closeInventory());
        if (!isDefault) {
            menu.set(53, button(player, Material.TNT, "btn-delete", "btn-delete-lore"), click ->
                    Menu.confirm(plugin, player, plugin.msg().title(player, "delete-title", "rank", parse(rank.display)), () -> {
                        ranks.delete(rank);
                        ranks.log(player.getName(), "delete", rank.id, "");
                        plugin.msg().info(player, "rank-deleted", "rank", rank.id);
                        openAdmin(player, 0);
                    }, () -> openEdit(player, rank)));
        }
        menu.open(player);
    }

    private static String safeColor(String name) {
        try {
            ChatColor.valueOf(name);
            return name;
        } catch (IllegalArgumentException e) {
            return "WHITE";
        }
    }

    private void edit(Player player, Rank rank, String prompt, String current, java.util.function.Consumer<String> apply, String field) {
        plugin.input().ask(player, prompt, current, value -> {
            apply.accept(value);
            ranks.save();
            ranks.refreshAll();
            ranks.log(player.getName(), "edit", rank.id, field + " = " + value);
            plugin.msg().info(player, "rank-updated", "rank", parse(rank.display));
            openEdit(player, rank);
        }, () -> openEdit(player, rank));
    }

    private void set(Player player, Rank rank, Runnable change, String field, String value) {
        change.run();
        ranks.save();
        ranks.refreshAll();
        ranks.log(player.getName(), "edit", rank.id, field + " = " + value);
        openEdit(player, rank);
    }

    // ---- permissions ----------------------------------------------------------------------------------------

    void openPerms(Player player, Rank rank, int page) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : new ArrayList<>(rank.permissions.entrySet())) {
            String perm = e.getKey();
            boolean on = e.getValue();
            entries.add(new Menu.Entry(Menu.item(on ? Material.LIME_DYE : Material.GRAY_DYE,
                    plugin.msg().parse(on ? "<green><perm>" : "<red><perm>", "perm", perm),
                    plugin.msg().lore(player, "perm-lore", "state", plugin.msg().raw(player, on ? "perm-on" : "perm-off"))), click -> {
                if (click.isShiftClick() && click.isRightClick()) {
                    rank.permissions.remove(perm);
                    ranks.log(player.getName(), "perm-remove", rank.id, perm);
                    plugin.msg().info(player, "perm-removed", "perm", perm, "rank", parse(rank.display));
                } else {
                    rank.permissions.put(perm, !on);
                    ranks.log(player.getName(), on ? "perm-off" : "perm-on", rank.id, perm);
                }
                ranks.save();
                ranks.refreshAll();
                openPerms(player, rank, page);
            }));
        }
        for (Map.Entry<String, Boolean> e : ranks.permissions(rank).entrySet()) {
            if (rank.permissions.containsKey(e.getKey())) continue;
            entries.add(new Menu.Entry(Menu.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    plugin.msg().parse(e.getValue() ? "<muted><perm>" : "<muted><st><perm></st>", "perm", e.getKey()),
                    plugin.msg().lore(player, "perm-inherited-lore", "rank", String.valueOf(ranks.source(rank, e.getKey())),
                            "state", plugin.msg().raw(player, e.getValue() ? "perm-on" : "perm-off"))), null));
        }
        Menu menu = Menu.paged(plugin, player, plugin.msg().title(player, "perms-title", "rank", parse(rank.display)), entries, page,
                next -> openPerms(player, rank, next), () -> openEdit(player, rank));
        menu.set(47, button(player, Material.LIME_CONCRETE, "btn-add-perm", "btn-add-perm-lore"), click ->
                plugin.input().ask(player, "prompt-permission", "", text -> {
                    String perm = text.replace(" ", "");
                    boolean on = !perm.startsWith("-");
                    if (!on) perm = perm.substring(1);
                    if (perm.isEmpty()) {
                        openPerms(player, rank, page);
                        return;
                    }
                    rank.permissions.put(perm.toLowerCase(Locale.ROOT), on);
                    ranks.save();
                    ranks.refreshAll();
                    ranks.log(player.getName(), on ? "perm-add" : "perm-negate", rank.id, perm);
                    plugin.msg().info(player, on ? "perm-added" : "perm-negated", "perm", perm, "rank", parse(rank.display));
                    openPerms(player, rank, page);
                }, () -> openPerms(player, rank, page)));
        menu.open(player);
    }

    // ---- parents --------------------------------------------------------------------------------------------

    private void openParents(Player player, Rank rank) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (Rank other : ranks.sorted()) {
            if (other == rank) continue;
            boolean on = rank.parents.contains(other.id);
            entries.add(new Menu.Entry(Menu.item(on ? Material.LIME_STAINED_GLASS : Menu.material(other.icon, Material.NAME_TAG), parse(other.display),
                    plugin.msg().lore(player, "parent-lore", "state", plugin.msg().raw(player, on ? "yes" : "no"), "weight", other.weight)), click -> {
                if (on) {
                    rank.parents.remove(other.id);
                    ranks.log(player.getName(), "parent-remove", rank.id, other.id);
                } else if (ranks.wouldLoop(rank, other)) {
                    plugin.msg().error(player, "parent-loop", "rank", other.id);
                    return;
                } else {
                    rank.parents.add(other.id);
                    ranks.log(player.getName(), "parent-add", rank.id, other.id);
                }
                ranks.save();
                ranks.refreshAll();
                openParents(player, rank);
            }));
        }
        Menu.paged(plugin, player, plugin.msg().title(player, "parents-title", "rank", parse(rank.display)), entries, 0,
                next -> openParents(player, rank), () -> openEdit(player, rank)).open(player);
    }

    // ---- members --------------------------------------------------------------------------------------------

    private void openMembers(Player player, Rank rank, int page) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (Map.Entry<UUID, String> member : ranks.members(rank)) {
            RankManager.Assignment a = ranks.assignment(member.getKey());
            entries.add(new Menu.Entry(Menu.head(Bukkit.getOfflinePlayer(member.getKey()), plugin.msg().parse("<brand><name></brand>", "name", member.getValue()),
                    plugin.msg().lore(player, "member-lore", "expires", expiry(player, a.until()),
                            "online", plugin.msg().raw(player, Bukkit.getPlayer(member.getKey()) != null ? "online" : "offline"))), click -> {
                Rank fallback = ranks.defaultRank();
                if (fallback == null || fallback == rank) return;
                Menu.confirm(plugin, player, plugin.msg().title(player, "remove-member-title", "player", member.getValue()), () -> {
                    ranks.set(member.getKey(), member.getValue(), fallback, 0);
                    ranks.log(player.getName(), "set", member.getValue(), fallback.id);
                    plugin.msg().info(player, "rank-set", "player", member.getValue(), "rank", parse(fallback.display), "duration", plugin.msg().raw(player, "permanent"));
                    openMembers(player, rank, page);
                }, () -> openMembers(player, rank, page));
            }));
        }
        Menu menu = Menu.paged(plugin, player, plugin.msg().title(player, "members-title", "rank", parse(rank.display)), entries, page,
                next -> openMembers(player, rank, next), () -> openEdit(player, rank));
        menu.set(47, button(player, Material.LIME_CONCRETE, "btn-add-member", "btn-add-member-lore"), click ->
                plugin.input().ask(player, "prompt-player", "", name -> {
                    UUID id = ranks.find(name);
                    if (id == null) {
                        plugin.msg().error(player, "player-never-joined", "player", name);
                    } else {
                        ranks.set(id, ranks.name(id), rank, 0);
                        ranks.log(player.getName(), "set", ranks.name(id), rank.id);
                        plugin.msg().info(player, "rank-set", "player", ranks.name(id), "rank", parse(rank.display), "duration", plugin.msg().raw(player, "permanent"));
                    }
                    openMembers(player, rank, page);
                }, () -> openMembers(player, rank, page)));
        menu.open(player);
    }

    // ---- log ------------------------------------------------------------------------------------------------

    private void openLog(Player player, int page) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (String[] line : ranks.readLog(280)) {
            String text = plugin.msg().raw(player, "log-" + line[2]);
            entries.add(new Menu.Entry(Menu.item(Material.PAPER, plugin.msg().parse("<brand><text></brand>", "text", text.replace("<a>", line[3]).replace("<b>", line[4])),
                    plugin.msg().lore(player, "log-lore", "actor", line[1], "date", DATE.format(Instant.ofEpochMilli(Long.parseLong(line[0]))))), null));
        }
        if (entries.isEmpty()) {
            plugin.msg().error(player, "log-empty");
            return;
        }
        Menu.paged(plugin, player, plugin.msg().title(player, "log-title"), entries, page, next -> openLog(player, next), () -> openAdmin(player, 0)).open(player);
    }

    // =========================================================================================================
    // Players
    // =========================================================================================================

    void openUser(Player player, int page) {
        RankManager.Assignment mine = ranks.assignment(player.getUniqueId());
        Rank current = ranks.rank(mine.rank());
        List<Menu.Entry> entries = new ArrayList<>();
        for (Rank rank : ranks.sorted()) {
            String status;
            if (rank == current) status = plugin.msg().raw(player, mine.until() == 0 ? "status-yours" : "status-yours-temp").replace("<time>", expiry(player, mine.until()));
            else if (plugin.canBuy(player, rank)) status = plugin.msg().raw(player, "status-buy").replace("<price>", Eco.format(rank.price)).replace("<duration>", duration(player, rank.purchaseDuration));
            else status = "";
            List<Component> lore = new ArrayList<>();
            for (String line : rank.description.isEmpty() ? new String[0] : rank.description.split("\\|")) lore.add(plugin.msg().parse("<white>" + line.trim()));
            lore.addAll(plugin.msg().lore(player, "user-rank-lore", "prefix", parse(rank.prefix), "weight", rank.weight,
                    "perms", ranks.permissions(rank).size(), "parents", parentNames(rank)));
            if (!status.isEmpty()) lore.add(plugin.msg().parse("<accent>" + status));
            lore.addAll(plugin.msg().lore(player, plugin.canBuy(player, rank) ? "user-rank-actions-buy" : "user-rank-actions"));
            ItemStack item = Menu.item(Menu.material(rank.icon, Material.NAME_TAG), parse(rank.display), lore);
            entries.add(new Menu.Entry(item, click -> {
                if (click.isRightClick() && plugin.canBuy(player, rank)) confirmBuy(player, rank);
                else openRankPerms(player, rank, 0);
            }));
        }
        Menu menu = Menu.paged(plugin, player, plugin.msg().title(player, "user-title"), entries, page, next -> openUser(player, next), null);
        menu.set(4, Menu.head(player, plugin.msg().text(player, "user-head", "rank", current == null ? Component.text("–") : parse(current.display)),
                plugin.msg().lore(player, "user-head-lore", "expires", expiry(player, mine.until()), "perms", current == null ? 0 : ranks.permissions(current).size())), null);
        menu.open(player);
    }

    private void openRankPerms(Player player, Rank rank, int page) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : ranks.permissions(rank).entrySet()) {
            entries.add(new Menu.Entry(Menu.item(e.getValue() ? Material.LIME_DYE : Material.GRAY_DYE,
                    plugin.msg().parse(e.getValue() ? "<green><perm>" : "<red><st><perm></st>", "perm", e.getKey()),
                    plugin.msg().lore(player, "user-perm-lore", "rank", String.valueOf(ranks.source(rank, e.getKey())),
                            "state", plugin.msg().raw(player, e.getValue() ? "perm-on" : "perm-off"))), null));
        }
        if (entries.isEmpty()) {
            plugin.msg().error(player, "rank-no-perms", "rank", parse(rank.display));
            return;
        }
        Menu.paged(plugin, player, plugin.msg().title(player, "user-perms-title", "rank", parse(rank.display)), entries, page,
                next -> openRankPerms(player, rank, next), () -> openUser(player, 0)).open(player);
    }

    private void confirmBuy(Player player, Rank rank) {
        Menu.confirm(plugin, player, plugin.msg().title(player, "buy-title", "rank", parse(rank.display), "price", Eco.format(rank.price)), () -> {
            player.closeInventory();
            plugin.buy(player, rank);
        }, () -> openUser(player, 0));
    }
}

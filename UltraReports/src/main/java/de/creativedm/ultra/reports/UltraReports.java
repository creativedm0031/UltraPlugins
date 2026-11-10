package de.creativedm.ultra.reports;

import de.creativedm.ultra.core.DataFile;
import de.creativedm.ultra.core.Durations;
import de.creativedm.ultra.core.Menu;
import de.creativedm.ultra.core.UltraPlugin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class UltraReports extends UltraPlugin {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final List<String> STATES = List.of("open", "claimed", "closed");

    private final Map<UUID, Long> lastReport = new HashMap<>();
    private final HttpClient http = HttpClient.newHttpClient();
    private DataFile data;

    @Override
    public String module() {
        return "Reports";
    }

    @Override
    protected void enable() {
        data = new DataFile(this, "reports.yml");
        command("report", "ultrareports.report", true, (s, a) -> report((Player) s, a), (s, a) -> a.length == 1 ? null : a.length == 2 ? reasons() : List.of());
        command("reports", "ultrareports.staff", true, (s, a) -> reports((Player) s, a), (s, a) -> List.of());
    }

    private List<String> reasons() {
        ConfigurationSection section = getConfig().getConfigurationSection("reasons");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    private String reasonName(String reason) {
        return getConfig().getString("reasons." + reason + ".name", reason);
    }

    private List<String> ids() {
        List<String> ids = new ArrayList<>(data.yaml().getKeys(false));
        ids.sort((a, b) -> Integer.compare(Integer.parseInt(b), Integer.parseInt(a)));
        return ids;
    }

    // ---- reporting ------------------------------------------------------------------------------------------

    private void report(Player player, String[] args) {
        if (args.length == 0) {
            usage(player, "report");
            return;
        }
        Player target = player(player, args[0]);
        if (target == null) return;
        if (target == player) {
            msg().error(player, "report-self");
            return;
        }
        if (target.hasPermission("ultrareports.exempt")) {
            msg().error(player, "report-exempt", "player", target.getName());
            return;
        }
        long wait = lastReport.getOrDefault(player.getUniqueId(), 0L) + getConfig().getInt("cooldown", 60) * 1000L - System.currentTimeMillis();
        if (wait > 0 && !player.hasPermission("ultrareports.bypass.cooldown")) {
            msg().error(player, "cooldown", "time", Durations.format(wait));
            return;
        }
        for (String id : ids()) {
            if (player.getUniqueId().toString().equals(data.yaml().getString(id + ".reporter"))
                    && target.getUniqueId().toString().equals(data.yaml().getString(id + ".target"))
                    && !"closed".equals(data.yaml().getString(id + ".state"))) {
                msg().error(player, "already-reported", "player", target.getName());
                return;
            }
        }
        if (args.length > 1) {
            submit(player, target, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            return;
        }
        // No reason given: let the player pick one.
        Menu menu = new Menu(msg().title(player, "reason-title", "player", target.getName()), 3).frame();
        int slot = 10;
        for (String reason : reasons()) {
            if (slot > 16) break;
            menu.set(slot++, Menu.item(Menu.material(getConfig().getString("reasons." + reason + ".icon"), Material.PAPER),
                    msg().parse("<brand><reason></brand>", "reason", reasonName(reason)), msg().lore(player, "reason-lore")), click -> {
                player.closeInventory();
                submit(player, target, reasonName(reason));
            });
        }
        menu.open(player);
    }

    private void submit(Player reporter, Player target, String reason) {
        int id = ids().stream().mapToInt(Integer::parseInt).max().orElse(0) + 1;
        String path = id + ".";
        data.yaml().set(path + "reporter", reporter.getUniqueId().toString());
        data.yaml().set(path + "reporter-name", reporter.getName());
        data.yaml().set(path + "target", target.getUniqueId().toString());
        data.yaml().set(path + "target-name", target.getName());
        data.yaml().set(path + "reason", reason);
        data.yaml().set(path + "time", System.currentTimeMillis());
        data.yaml().set(path + "state", "open");
        data.yaml().set(path + "world", target.getWorld().getName());
        data.save();
        lastReport.put(reporter.getUniqueId(), System.currentTimeMillis());
        msg().info(reporter, "report-sent", "player", target.getName());
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission("ultrareports.notify")) continue;
            // Clickable, so the staff notification is one chat line.
            msg().chat(staff, "notify-new", "id", id, "player", target.getName(), "reporter", reporter.getName(), "reason", reason);
            msg().sound(staff, "success");
        }
        webhook(id, reporter.getName(), target.getName(), reason);
    }

    private void webhook(int id, String reporter, String target, String reason) {
        String url = getConfig().getString("discord-webhook", "");
        if (url.isEmpty()) return;
        String json = "{\"embeds\":[{\"title\":\"Report #" + id + "\",\"color\":9133302,\"fields\":["
                + field("Player", target) + "," + field("Reporter", reporter) + "," + field("Reason", reason) + "]}]}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        http.sendAsync(request, HttpResponse.BodyHandlers.discarding()).exceptionally(error -> {
            getLogger().warning("Discord webhook failed: " + error.getMessage());
            return null;
        });
    }

    private static String field(String name, String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"name\":\"" + name + "\",\"value\":\"" + escaped + "\",\"inline\":true}";
    }

    // ---- staff inbox ----------------------------------------------------------------------------------------

    private void openInbox(Player staff, String state, int page) {
        List<Menu.Entry> entries = new ArrayList<>();
        for (String id : ids()) {
            if (!state.equals(data.yaml().getString(id + ".state"))) continue;
            String path = id + ".";
            String targetName = data.yaml().getString(path + "target-name");
            UUID target = UUID.fromString(data.yaml().getString(path + "target"));
            String handler = data.yaml().getString(path + "handler", "–");
            entries.add(new Menu.Entry(Menu.head(Bukkit.getOfflinePlayer(target),
                    msg().parse("<brand>#<id> <player></brand>", "id", id, "player", targetName),
                    msg().lore(staff, "report-lore", "reason", data.yaml().getString(path + "reason"),
                            "reporter", data.yaml().getString(path + "reporter-name"),
                            "date", DATE.format(Instant.ofEpochMilli(data.yaml().getLong(path + "time"))),
                            "handler", handler, "online", msg().raw(staff, Bukkit.getPlayer(target) != null ? "online" : "offline"))),
                    click -> openReport(staff, id)));
        }
        Menu menu = Menu.paged(this, staff, msg().title(staff, "inbox-title", "state", msg().raw(staff, "state-" + state)), entries, page,
                next -> openInbox(staff, state, next), null);
        menu.set(47, Menu.item(Material.HOPPER, msg().text(staff, "filter-button"), msg().lore(staff, "filter-lore", "state", msg().raw(staff, "state-" + state))),
                click -> openInbox(staff, STATES.get((STATES.indexOf(state) + 1) % STATES.size()), 0));
        menu.open(staff);
    }

    private void openReport(Player staff, String id) {
        String path = id + ".";
        UUID targetId = UUID.fromString(data.yaml().getString(path + "target"));
        String targetName = data.yaml().getString(path + "target-name");
        String state = data.yaml().getString(path + "state");
        Menu menu = new Menu(msg().title(staff, "report-title", "id", id), 3).frame();
        menu.set(4, Menu.head(Bukkit.getOfflinePlayer(targetId), msg().parse("<brand><player></brand>", "player", targetName),
                msg().lore(staff, "report-detail", "reason", data.yaml().getString(path + "reason"), "reporter", data.yaml().getString(path + "reporter-name"))), null);
        if ("open".equals(state)) {
            menu.set(10, Menu.item(Material.NAME_TAG, msg().text(staff, "action-claim"), msg().lore(staff, "action-claim-lore")), click -> {
                data.yaml().set(path + "state", "claimed");
                data.yaml().set(path + "handler", staff.getName());
                data.save();
                msg().info(staff, "report-claimed", "id", id);
                Player target = Bukkit.getPlayer(targetId);
                if (target != null && getConfig().getBoolean("teleport-on-claim")) staff.teleport(target.getLocation());
                openReport(staff, id);
            });
        }
        menu.set(12, Menu.item(Material.ENDER_PEARL, msg().text(staff, "action-teleport"), List.of()), click -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) {
                msg().error(staff, "player-not-found", "player", targetName);
                return;
            }
            staff.closeInventory();
            staff.teleport(target.getLocation());
            msg().info(staff, "teleported", "player", targetName);
        });
        if (Bukkit.getPluginManager().isPluginEnabled("UltraPunish")) {
            menu.set(14, Menu.item(Material.IRON_SWORD, msg().text(staff, "action-punish"), msg().lore(staff, "action-punish-lore")), click -> {
                staff.closeInventory();
                staff.performCommand("punish " + targetName);
            });
        }
        if (!"closed".equals(state)) {
            menu.set(16, Menu.item(Material.LIME_DYE, msg().text(staff, "action-close"), List.of()), click -> {
                data.yaml().set(path + "state", "closed");
                data.yaml().set(path + "handler", staff.getName());
                data.save();
                msg().info(staff, "report-closed", "id", id);
                Player reporter = Bukkit.getPlayer(UUID.fromString(data.yaml().getString(path + "reporter")));
                if (reporter != null) msg().info(reporter, "report-handled", "player", targetName);
                openInbox(staff, "open", 0);
            });
        }
        menu.set(22, Menu.item(Material.ARROW, msg().text(staff, "gui-back"), List.of()), click -> openInbox(staff, state, 0));
        menu.open(staff);
    }

    /** /reports opens the inbox, /reports &lt;id&gt; one report (used by the clickable notification). */
    private void reports(Player staff, String[] args) {
        if (args.length == 0) openInbox(staff, "open", 0);
        else if (!data.yaml().contains(args[0].replace("#", ""))) msg().error(staff, "report-unknown");
        else openReport(staff, args[0].replace("#", ""));
    }
}

package in.potenfyr.statfyr.commands;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.LeaderboardEntry;
import in.potenfyr.statfyr.analytics.Metrics;
import in.potenfyr.statfyr.analytics.Period;
import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.analytics.ServerStats;
import in.potenfyr.statfyr.compat.ServerVersion;
import in.potenfyr.statfyr.util.Text;
import in.potenfyr.statfyr.util.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /statfyr} (alias {@code /sf}) command executor and tab completer.
 *
 * <p>Subcommands: {@code stats}, {@code top}, {@code help}, {@code reload},
 * {@code status}, {@code database}, {@code purge}, {@code purge-history},
 * {@code debug}.
 */
public final class StatfyrCommand
        implements CommandExecutor, TabCompleter {

    private final Statfyr plugin;

    private final MessageService messages;

    private final List<HelpEntry> helpEntries =
            new ArrayList<>();

    public StatfyrCommand(Statfyr plugin, MessageService messages) {

        this.plugin = plugin;
        this.messages = messages;

        helpEntries.add(
                new HelpEntry(
                        "/statfyr stats [player]",
                        "View player statistics.",
                        "statfyr.stats"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr top [stat] [period]",
                        "View leaderboards.",
                        "statfyr.top"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr help",
                        "Show StatFYR commands.",
                        "statfyr.help"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr reload",
                        "Reload configuration.",
                        "statfyr.reload"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr status",
                        "View StatFYR status.",
                        "statfyr.status"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr database",
                        "View storage statistics.",
                        "statfyr.admin"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr purge <player>",
                        "Delete a player's analytics data.",
                        "statfyr.admin"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr purge-history",
                        "Delete all historical snapshots.",
                        "statfyr.admin"
                )
        );

        helpEntries.add(
                new HelpEntry(
                        "/statfyr debug",
                        "Dump analytics diagnostics.",
                        "statfyr.admin"
                )
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            if (can(sender, "statfyr.help")) {
                sendHelp(sender);

            } else {
                messages.send(sender, "no-permission");
            }

            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "stats":
                handleStats(sender, args);
                return true;

            case "top":
                handleTop(sender, args);
                return true;

            case "help":
                if (require(sender, "statfyr.help")) {
                    sendHelp(sender);
                }
                return true;

            case "reload":
                handleReload(sender);
                return true;

            case "status":
                handleStatus(sender);
                return true;

            case "database":
                handleDatabase(sender);
                return true;

            case "purge":
                handlePurge(sender, args);
                return true;

            case "purge-history":
                handlePurgeHistory(sender);
                return true;

            case "debug":
                handleDebug(sender);
                return true;

            default:
                messages.send(sender, "unknown-subcommand");
                return true;
        }
    }

    // -------------------------------------------------------------------------
    // stats
    // -------------------------------------------------------------------------

    private void handleStats(
            CommandSender sender,
            String[] args
    ) {

        UUID targetUuid;
        String targetName;

        if (args.length >= 2) {

            if (!require(sender, "statfyr.stats.others")) {
                return;
            }

            targetName = args[1];

            UUID resolved =
                    plugin.getPlayerResolver()
                            .resolveUuid(targetName);

            if (resolved == null) {
                messages.send(
                        sender,
                        "player-not-found",
                        "player",
                        targetName
                );
                return;
            }

            targetUuid = resolved;

        } else {

            if (!(sender instanceof Player)) {
                messages.send(sender, "players-only");
                return;
            }

            if (!require(sender, "statfyr.stats.self")) {
                return;
            }

            targetUuid = ((Player) sender).getUniqueId();
            targetName = sender.getName();
        }

        // Refresh name if possible.
        String name =
                plugin.getPlayerResolver()
                        .resolvePlayerName(targetUuid);

        if (!Text.isBlank(name) && !"unknown".equals(name)) {
            targetName = name;
        }

        PlayerProfile profile =
                plugin.getAnalytics()
                        .profileSnapshot(targetUuid);

        if (profile == null) {
            plugin.getAnalytics().profile(targetUuid, targetName);
            profile = plugin.getAnalytics().profileSnapshot(targetUuid);
        }

        if (profile == null
                || (profile.firstSeen <= 0L
                && profile.totalSessions == 0
                && profile.allTime.isEmpty())) {

            messages.send(
                    sender,
                    "stats-none",
                    "player",
                    targetName
            );

            return;
        }

        long kills =
                profile.total(Metrics.KILLS);

        long deaths =
                profile.total(Metrics.DEATHS);

        double kdr =
                deaths == 0L
                        ? kills
                        : (double) kills / (double) deaths;

        messages.sendRaw(
                sender,
                "stats-header",
                "player",
                targetName
        );

        messages.sendRaw(sender, "stats-combat");
        statLine(sender, "stats-kills", TimeFormat.number(kills));
        statLine(sender, "stats-deaths", TimeFormat.number(deaths));
        statLine(sender, "stats-kdr", String.format(Locale.ROOT, "%.2f", kdr));

        messages.sendRaw(sender, "stats-activity");
        statLine(
                sender,
                "stats-playtime",
                TimeFormat.duration(profile.total(Metrics.PLAYTIME))
        );
        statLine(
                sender,
                "stats-active-time",
                TimeFormat.duration(profile.total(Metrics.ACTIVE_TIME))
        );
        statLine(
                sender,
                "stats-afk-time",
                TimeFormat.duration(profile.total(Metrics.AFK_TIME))
        );
        statLine(
                sender,
                "stats-sessions",
                TimeFormat.number(profile.total(Metrics.SESSIONS))
        );

        messages.sendRaw(sender, "stats-mining");
        statLine(
                sender,
                "stats-blocks-mined",
                TimeFormat.number(profile.total(Metrics.BLOCKS_MINED))
        );
        statLine(
                sender,
                "stats-crafted",
                TimeFormat.number(profile.total(Metrics.ITEMS_CRAFTED))
        );

        statLine(
                sender,
                "stats-first-seen",
                TimeFormat.relative(profile.firstSeen)
        );
        statLine(
                sender,
                "stats-last-seen",
                TimeFormat.relative(profile.lastSeen)
        );
        statLine(
                sender,
                "stats-segment",
                plugin.getAnalytics().segment(profile)
        );
    }

    private void statLine(
            CommandSender sender,
            String labelKey,
            String value
    ) {

        messages.sendRaw(
                sender,
                "stats-line",
                "key",
                messages.get(labelKey),
                "value",
                value
        );
    }

    // -------------------------------------------------------------------------
    // top
    // -------------------------------------------------------------------------

    private void handleTop(
            CommandSender sender,
            String[] args
    ) {

        String stat =
                args.length >= 2 ? args[1] : "kills";

        String metric =
                Metrics.canonical(stat);

        if (!CommandSupport.isKnownMetric(metric)) {

            messages.send(
                    sender,
                    "invalid-stat",
                    "stat",
                    stat
            );

            return;
        }

        if (!can(sender, "statfyr.top")
                && !can(sender, "statfyr.top." + metric)
                && !can(sender, "statfyr.admin")) {

            messages.send(sender, "no-permission");
            return;
        }

        Period period =
                args.length >= 3
                        ? Period.from(args[2])
                        : Period.from(
                        plugin.getConfig()
                                .getString(
                                        "leaderboards.default-period",
                                        "all_time"
                                )
                );

        int limit =
                Math.max(
                        1,
                        plugin.getConfig()
                                .getInt(
                                        "pagination.default-limit",
                                        25
                                )
                );

        List<LeaderboardEntry> entries =
                plugin.getAnalytics()
                        .leaderboard(metric, period, limit);

        messages.sendRaw(
                sender,
                "top-header",
                "stat",
                metric,
                "period",
                period.token()
        );

        if (entries.isEmpty()) {

            messages.sendRaw(
                    sender,
                    "top-empty",
                    "stat",
                    metric
            );

            return;
        }

        int rank =
                1;

        for (LeaderboardEntry entry : entries) {

            messages.sendRaw(
                    sender,
                    "top-line",
                    "rank",
                    String.valueOf(rank),
                    "player",
                    entry.name,
                    "value",
                    formatValue(metric, entry)
            );

            rank++;
        }
    }

    private String formatValue(
            String metric,
            LeaderboardEntry entry
    ) {

        switch (metric) {

            case Metrics.PLAYTIME:
            case Metrics.ACTIVE_TIME:
            case Metrics.AFK_TIME:
                return TimeFormat.duration(entry.value);

            case Metrics.KDR:
                return String.format(
                        Locale.ROOT,
                        "%.2f",
                        entry.decimalValue
                );

            case Metrics.DISTANCE_TRAVELED:
            case Metrics.DISTANCE_WALKED:
            case Metrics.DISTANCE_SPRINTED:
            case Metrics.DISTANCE_SWUM:
            case Metrics.DISTANCE_FLOWN:
                return TimeFormat.number(entry.value / 100L) + "m";

            default:
                return TimeFormat.number(entry.value);
        }
    }

    // -------------------------------------------------------------------------
    // help
    // -------------------------------------------------------------------------

    private void sendHelp(CommandSender sender) {

        messages.sendRaw(sender, "help-header");

        for (HelpEntry entry : helpEntries) {

            if (entry.permission == null
                    || can(sender, entry.permission)
                    || can(sender, "statfyr.admin")) {

                messages.sendRaw(
                        sender,
                        "help-line",
                        "usage",
                        entry.usage,
                        "description",
                        entry.description
                );
            }
        }

        messages.sendRaw(sender, "help-footer");
    }

    // -------------------------------------------------------------------------
    // reload
    // -------------------------------------------------------------------------

    private void handleReload(CommandSender sender) {

        if (!require(sender, "statfyr.reload")) {
            return;
        }

        try {

            plugin.reloadStatfyr();

            messages.send(sender, "reloaded");

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "Reload failed: " + exception.getMessage()
            );

            messages.send(sender, "reload-failed");
        }
    }

    // -------------------------------------------------------------------------
    // status
    // -------------------------------------------------------------------------

    private void handleStatus(CommandSender sender) {

        if (!require(sender, "statfyr.status")) {
            return;
        }

        messages.sendRaw(sender, "status-header");

        statusLine(
                sender,
                "HTTP",
                plugin.getHttpServer() != null
                        && plugin.getHttpServer().isRunning()
                        ? messages.get("status-running")
                        : messages.get("status-stopped")
        );

        statusLine(
                sender,
                "Port",
                String.valueOf(plugin.getConfigManager().getPort())
        );

        statusLine(
                sender,
                "Platform",
                ServerVersion.getPlatformLabel()
        );

        statusLine(
                sender,
                "Profiles",
                String.valueOf(plugin.getAnalytics().profileCount())
        );

        statusLine(
                sender,
                "Online",
                String.valueOf(Bukkit.getOnlinePlayers().size())
        );
    }

    private void statusLine(
            CommandSender sender,
            String key,
            String value
    ) {

        messages.sendRaw(
                sender,
                "status-line",
                "key",
                key,
                "value",
                value
        );
    }

    // -------------------------------------------------------------------------
    // database
    // -------------------------------------------------------------------------

    private void handleDatabase(CommandSender sender) {

        if (!require(sender, "statfyr.admin")) {
            return;
        }

        messages.sendRaw(sender, "database-header");

        for (Map.Entry<String, Object> entry
                : plugin.getAnalytics()
                .debugSnapshot()
                .entrySet()) {

            statusLine(
                    sender,
                    entry.getKey(),
                    String.valueOf(entry.getValue())
            );
        }
    }

    // -------------------------------------------------------------------------
    // purge
    // -------------------------------------------------------------------------

    private void handlePurge(
            CommandSender sender,
            String[] args
    ) {

        if (!require(sender, "statfyr.admin")) {
            return;
        }

        if (args.length < 2) {

            messages.sendMessage(
                    sender,
                    "&cUsage: /statfyr purge <player>"
            );

            return;
        }

        String name =
                args[1];

        UUID uuid =
                plugin.getPlayerResolver()
                        .resolveUuid(name);

        if (uuid == null) {

            messages.send(
                    sender,
                    "player-not-found",
                    "player",
                    name
            );

            return;
        }

        plugin.getAnalytics().deleteProfile(uuid);

        messages.send(
                sender,
                "purge-done",
                "player",
                name
        );
    }

    private void handlePurgeHistory(CommandSender sender) {

        if (!require(sender, "statfyr.admin")) {
            return;
        }

        plugin.getAnalytics().purgeHistory();

        messages.send(sender, "purge-history-done");
    }

    // -------------------------------------------------------------------------
    // debug
    // -------------------------------------------------------------------------

    private void handleDebug(CommandSender sender) {

        if (!require(sender, "statfyr.admin")) {
            return;
        }

        messages.sendRaw(sender, "debug-header");

        for (Map.Entry<String, Object> entry
                : plugin.getAnalytics()
                .debugSnapshot()
                .entrySet()) {

            statusLine(
                    sender,
                    entry.getKey(),
                    String.valueOf(entry.getValue())
            );
        }

        statusLine(
                sender,
                "placeholderapi",
                String.valueOf(plugin.isPlaceholderApiPresent())
        );

        statusLine(
                sender,
                "vault",
                String.valueOf(plugin.isVaultPresent())
        );

        statusLine(
                sender,
                "prometheus",
                String.valueOf(
                        plugin.getConfig()
                                .getBoolean(
                                        "integrations.prometheus.enabled",
                                        false
                                )
                )
        );
    }

    // -------------------------------------------------------------------------
    // Permissions
    // -------------------------------------------------------------------------

    private boolean can(
            CommandSender sender,
            String permission
    ) {

        return sender.hasPermission(permission)
                || sender.hasPermission("statfyr.*");
    }

    private boolean require(
            CommandSender sender,
            String permission
    ) {

        if (can(sender, permission)
                || can(sender, "statfyr.admin")) {
            return true;
        }

        messages.send(sender, "no-permission");
        return false;
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (args.length == 1) {

            List<String> options =
                    new ArrayList<>();

            String[] subs = {
                    "stats", "top", "help", "reload", "status",
                    "database", "purge", "purge-history", "debug"
            };

            for (String sub : subs) {

                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))
                        && canRun(sender, sub)) {

                    options.add(sub);
                }
            }

            Collections.sort(options);
            return options;
        }

        if (args.length == 2) {

            String sub =
                    args[0].toLowerCase(Locale.ROOT);

            if ("top".equals(sub)) {

                return CommandSupport.filter(
                        Arrays.asList(Metrics.leaderboardKeys()),
                        args[1]
                );
            }

            if ("stats".equals(sub) || "purge".equals(sub)) {

                return CommandSupport.filter(
                        playerNames(),
                        args[1]
                );
            }
        }

        if (args.length == 3
                && "top".equals(args[0].toLowerCase(Locale.ROOT))) {

            return CommandSupport.filter(
                    Arrays.asList(
                            "daily",
                            "weekly",
                            "monthly",
                            "alltime"
                    ),
                    args[2]
            );
        }

        return Collections.emptyList();
    }

    private boolean canRun(
            CommandSender sender,
            String sub
    ) {

        return CommandSupport.canRun(
                permission -> can(sender, permission),
                sub
        );
    }

    private List<String> playerNames() {

        List<String> names =
                new ArrayList<>();

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {

            if (player.getName() != null) {
                names.add(player.getName());
            }
        }

        Collections.sort(names);
        return names;
    }

    // -------------------------------------------------------------------------
    // Inner
    // -------------------------------------------------------------------------

    private static final class HelpEntry {

        private final String usage;
        private final String description;
        private final String permission;

        private HelpEntry(
                String usage,
                String description,
                String permission
        ) {

            this.usage = usage;
            this.description = description;
            this.permission = permission;
        }
    }
}

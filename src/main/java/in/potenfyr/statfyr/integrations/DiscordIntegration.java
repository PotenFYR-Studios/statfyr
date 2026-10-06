package in.potenfyr.statfyr.integrations;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.LeaderboardEntry;
import in.potenfyr.statfyr.analytics.Metrics;
import in.potenfyr.statfyr.analytics.Period;
import in.potenfyr.statfyr.analytics.ServerStats;
import in.potenfyr.statfyr.compat.SchedulerCompat;
import in.potenfyr.statfyr.storage.JsonIO;
import in.potenfyr.statfyr.util.Text;
import in.potenfyr.statfyr.util.TimeFormat;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Optional Discord webhook notifications.
 *
 * <p>Posts are made asynchronously over plain {@link HttpURLConnection} — no
 * external HTTP dependency is bundled. Weekly/monthly summaries are announced
 * shortly after their configured reset boundary.
 */
public final class DiscordIntegration {

    private static final int MAX_CONTENT = 1900;

    private final Statfyr plugin;

    private volatile boolean enabled;
    private volatile String webhookUrl = "";
    private volatile boolean weeklyEnabled;
    private volatile boolean monthlyEnabled;
    private volatile boolean serverStatusEnabled;

    private volatile String lastWeekKey;
    private volatile String lastMonthKey;
    private volatile long lastStatusAt;

    private volatile boolean taskStarted;

    public DiscordIntegration(Statfyr plugin) {

        this.plugin = plugin;
    }

    /**
     * Re-reads configuration. Safe to call on every reload.
     */
    public void reload() {

        this.enabled =
                plugin.getConfig().getBoolean(
                        "integrations.discord.enabled",
                        false
                );

        String url =
                plugin.getConfig().getString(
                        "integrations.discord.webhook-url",
                        ""
                );

        this.webhookUrl =
                url == null ? "" : url.trim();

        this.weeklyEnabled =
                plugin.getConfig().getBoolean(
                        "integrations.discord.events.weekly-leaderboard",
                        true
                );

        this.monthlyEnabled =
                plugin.getConfig().getBoolean(
                        "integrations.discord.events.monthly-leaderboard",
                        true
                );

        this.serverStatusEnabled =
                plugin.getConfig().getBoolean(
                        "integrations.discord.events.server-status",
                        false
                );
    }

    /**
     * Starts the boundary watcher. Call once, after the analytics engine.
     */
    public void start() {

        reload();

        lastWeekKey =
                plugin.getAnalytics().currentWeekKey();

        lastMonthKey =
                plugin.getAnalytics().currentMonthKey();

        lastStatusAt =
                System.currentTimeMillis();

        if (taskStarted) {
            return;
        }

        taskStarted = true;

        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::checkBoundaries,
                20L * 300L,
                20L * 300L
        );
    }

    private void checkBoundaries() {

        if (!enabled) {
            return;
        }

        try {

            String week =
                    plugin.getAnalytics().currentWeekKey();

            String month =
                    plugin.getAnalytics().currentMonthKey();

            if (weeklyEnabled
                    && lastWeekKey != null
                    && !week.equals(lastWeekKey)) {

                lastWeekKey = week;
                announceWeekly();
            }

            if (monthlyEnabled
                    && lastMonthKey != null
                    && !month.equals(lastMonthKey)) {

                lastMonthKey = month;
                announceMonthly();
            }

            long now =
                    System.currentTimeMillis();

            if (serverStatusEnabled
                    && now - lastStatusAt >= 60L * 60L * 1000L) {

                lastStatusAt = now;
                announceStatus();
            }

        } catch (Throwable ignored) {
        }
    }

    // -------------------------------------------------------------------------
    // Announcements
    // -------------------------------------------------------------------------

    public void announceWeekly() {

        send(buildSummary("Weekly", Period.WEEKLY));
    }

    public void announceMonthly() {

        send(buildSummary("Monthly", Period.MONTHLY));
    }

    /**
     * Posts a short periodic server status summary.
     */
    public void announceStatus() {

        if (!enabled) {
            return;
        }

        ServerStats stats =
                plugin.getAnalytics().serverStats();

        StringBuilder builder =
                new StringBuilder();

        builder.append("**StatFYR Server Status**\n\n");
        builder.append("Online: **").append(stats.online).append("**\n");
        builder.append("Peak today: **").append(stats.peakToday).append("**\n");
        builder.append("Unique today: **").append(stats.uniqueToday).append("**\n");
        builder.append("Total players: **").append(stats.totalPlayers).append("**");

        send(builder.toString());
    }

    /**
     * Sends an arbitrary milestone message.
     *
     * @param message message body
     */
    public void announceMilestone(String message) {

        if (!enabled) {
            return;
        }

        send("**StatFYR Milestone**\n" + message);
    }

    private String buildSummary(String label, Period period) {

        StringBuilder builder =
                new StringBuilder();

        builder.append("**StatFYR ")
                .append(label)
                .append(" Statistics**\n\n");

        List<LeaderboardEntry> playtime =
                plugin.getAnalytics()
                        .leaderboard(Metrics.PLAYTIME, period, 1);

        List<LeaderboardEntry> kills =
                plugin.getAnalytics()
                        .leaderboard(Metrics.KILLS, period, 1);

        List<LeaderboardEntry> mined =
                plugin.getAnalytics()
                        .leaderboard(Metrics.BLOCKS_MINED, period, 1);

        if (!playtime.isEmpty()) {

            LeaderboardEntry entry =
                    playtime.get(0);

            builder.append("Top Player: **")
                    .append(entry.name)
                    .append("** - ")
                    .append(TimeFormat.duration(entry.value))
                    .append('\n');
        }

        if (!kills.isEmpty()) {

            LeaderboardEntry entry =
                    kills.get(0);

            builder.append("Most Kills: **")
                    .append(entry.name)
                    .append("** - ")
                    .append(TimeFormat.number(entry.value))
                    .append('\n');
        }

        if (!mined.isEmpty()) {

            LeaderboardEntry entry =
                    mined.get(0);

            builder.append("Most Mining: **")
                    .append(entry.name)
                    .append("** - ")
                    .append(TimeFormat.number(entry.value))
                    .append(" blocks\n");
        }

        ServerStats stats =
                plugin.getAnalytics().serverStats();

        builder.append("Server Peak: **")
                .append(
                        period == Period.MONTHLY
                                ? stats.peakMonth
                                : stats.peakWeek
                )
                .append(" players**");

        return builder.toString();
    }

    // -------------------------------------------------------------------------
    // Transport
    // -------------------------------------------------------------------------

    private void send(String content) {

        if (!enabled || Text.isBlank(webhookUrl)) {
            return;
        }

        final String body =
                JsonIO.toJson(
                        Collections.singletonMap(
                                "content",
                                truncate(content)
                        )
                );

        final String url = webhookUrl;

        try {

            plugin.getExecutorService().execute(
                    () -> post(url, body)
            );

        } catch (Throwable ignored) {
            // Executor unavailable (shutdown) — skip.
        }
    }

    private void post(String url, String body) {

        HttpURLConnection connection = null;

        try {

            connection =
                    (HttpURLConnection) new URL(url).openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );
            connection.setRequestProperty(
                    "User-Agent",
                    "StatFYR"
            );

            try (OutputStream output =
                         connection.getOutputStream()) {

                output.write(
                        body.getBytes(StandardCharsets.UTF_8)
                );
            }

            int status =
                    connection.getResponseCode();

            if (status >= 300) {

                plugin.getLogger().warning(
                        "Discord webhook responded with HTTP " + status
                );
            }

        } catch (Throwable throwable) {

            plugin.getLogger().warning(
                    "Failed to post to Discord webhook: "
                            + throwable.getMessage()
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String truncate(String content) {

        if (content == null) {
            return "";
        }

        return content.length() <= MAX_CONTENT
                ? content
                : content.substring(0, MAX_CONTENT);
    }
}

package in.potenfyr.statfyr.integrations;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.Metrics;
import in.potenfyr.statfyr.analytics.Period;
import in.potenfyr.statfyr.util.TimeFormat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * PlaceholderAPI expansion.
 *
 * <p>Self placeholders (no player suffix) resolve against the requesting
 * player; parameterised variants append a player name, for example
 * {@code %statfyr_kills_Steve%}.
 *
 * <p>This class references PlaceholderAPI types and is therefore only loaded
 * by {@link PlaceholderIntegration} when PlaceholderAPI is present.
 */
public final class StatfyrPlaceholderExpansion extends PlaceholderExpansion {

    private final Statfyr plugin;

    private final List<String> keys =
            new ArrayList<>();

    public StatfyrPlaceholderExpansion(Statfyr plugin) {

        this.plugin = plugin;

        keys.addAll(
                Arrays.asList(Metrics.leaderboardKeys())
        );

        keys.add(Metrics.AFK_TIME);
        keys.add("first_seen");
        keys.add("last_seen");
        keys.add("segment");
        keys.add("afk");
        keys.add("rank");

        // Longest first so "distance_traveled" wins over "distance".
        keys.sort(
                (left, right) ->
                        Integer.compare(right.length(), left.length())
        );
    }

    @Override
    public String getIdentifier() {
        return "statfyr";
    }

    @Override
    public String getAuthor() {
        return "PotenFYR Studios";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(
            OfflinePlayer player,
            String params
    ) {

        if (params == null || params.isEmpty()) {
            return "";
        }

        String query =
                params.toLowerCase(Locale.ROOT);

        if (query.startsWith("server_")) {
            return serverPlaceholder(query);
        }

        PlaceholderParser.Result parsed =
                PlaceholderParser.parse(query, keys);

        if (parsed == null) {
            return "";
        }

        if (parsed.isSelf()) {

            return playerPlaceholder(
                    player == null ? null : player.getUniqueId(),
                    parsed.key
            );
        }

        UUID uuid =
                plugin.getPlayerResolver()
                        .resolveUuid(parsed.playerName);

        return playerPlaceholder(uuid, parsed.key);
    }

    // -------------------------------------------------------------------------
    // Player placeholders
    // -------------------------------------------------------------------------

    private String playerPlaceholder(UUID uuid, String key) {

        if (uuid == null) {
            return "";
        }

        switch (key) {

            case "first_seen": {

                in.potenfyr.statfyr.analytics.PlayerProfile profile =
                        plugin.getAnalytics().profileIfPresent(uuid);

                return profile == null
                        ? ""
                        : TimeFormat.relative(profile.firstSeen);
            }

            case "last_seen": {

                in.potenfyr.statfyr.analytics.PlayerProfile profile =
                        plugin.getAnalytics().profileIfPresent(uuid);

                return profile == null
                        ? ""
                        : TimeFormat.relative(profile.lastSeen);
            }

            case "segment": {

                in.potenfyr.statfyr.analytics.PlayerProfile profile =
                        plugin.getAnalytics().profileIfPresent(uuid);

                return profile == null
                        ? ""
                        : plugin.getAnalytics().segment(profile);
            }

            case "afk":
                return plugin.getAnalytics().isAfk(uuid)
                        ? "yes"
                        : "no";

            case "rank": {

                int rank =
                        plugin.getAnalytics()
                                .rank(uuid, Metrics.KILLS, Period.ALL_TIME);

                return rank <= 0 ? "unranked" : "#" + rank;
            }

            default:
                return formatMetric(uuid, key);
        }
    }

    private String formatMetric(UUID uuid, String metric) {

        if (Metrics.KDR.equals(metric)) {

            return String.format(
                    Locale.ROOT,
                    "%.2f",
                    plugin.getAnalytics().kdr(uuid)
            );
        }

        if (Metrics.PLAYTIME.equals(metric)
                || Metrics.ACTIVE_TIME.equals(metric)
                || Metrics.AFK_TIME.equals(metric)) {

            return TimeFormat.duration(
                    plugin.getAnalytics().metric(uuid, metric)
            );
        }

        if (metric.startsWith("distance")) {

            return TimeFormat.number(
                    plugin.getAnalytics().metric(uuid, metric) / 100L
            ) + "m";
        }

        return String.valueOf(
                plugin.getAnalytics().metric(uuid, metric)
        );
    }

    // -------------------------------------------------------------------------
    // Server placeholders
    // -------------------------------------------------------------------------

    private String serverPlaceholder(String query) {

        in.potenfyr.statfyr.analytics.ServerStats stats =
                plugin.getAnalytics().serverStats();

        switch (query) {

            case "server_online":
                return String.valueOf(stats.online);

            case "server_peak_today":
                return String.valueOf(stats.peakToday);

            case "server_peak_week":
                return String.valueOf(stats.peakWeek);

            case "server_peak_month":
                return String.valueOf(stats.peakMonth);

            case "server_peak_all_time":
                return String.valueOf(stats.peakAllTime);

            case "server_unique_today":
                return String.valueOf(stats.uniqueToday);

            case "server_unique_week":
                return String.valueOf(stats.uniqueWeek);

            case "server_unique_month":
                return String.valueOf(stats.uniqueMonth);

            case "server_total_players":
                return String.valueOf(stats.totalPlayers);

            case "server_sessions":
                return String.valueOf(stats.sessionsTotal);

            case "server_playtime":
                return TimeFormat.duration(stats.totalPlaytimeSeconds);

            default:
                return "";
        }
    }
}

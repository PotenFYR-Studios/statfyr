package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.LeaderboardEntry;
import in.potenfyr.statfyr.analytics.Metrics;
import in.potenfyr.statfyr.analytics.Period;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.http.HttpQuery;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;
import in.potenfyr.statfyr.util.TimeFormat;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code GET /api/leaderboard/{stat}}
 *
 * <p>Query parameters:
 * <ul>
 *     <li>{@code period} — daily, weekly, monthly, all_time</li>
 *     <li>{@code limit} / {@code page} — pagination</li>
 *     <li>{@code order} — asc/desc (desc default)</li>
 *     <li>{@code online_only} — only currently online players</li>
 * </ul>
 */
public final class LeaderboardHandler implements HttpHandler {

    private final Statfyr plugin;

    public LeaderboardHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        try {

            String statName =
                    extractStatName(
                            exchange.getRequestURI().getPath()
                    );

            if (statName == null || statName.trim().isEmpty()) {

                ResponseUtil.sendBadRequest(
                        exchange,
                        "Stat name is required"
                );

                return;
            }

            ConfigManager config =
                    plugin.getConfigManager();

            Map<String, String> params =
                    HttpQuery.parse(exchange.getRequestURI());

            int limit =
                    HttpQuery.parseInt(
                            params,
                            "limit",
                            config.getDefaultPageLimit(),
                            1,
                            config.getMaxPageLimit()
                    );

            int page =
                    HttpQuery.parseInt(
                            params,
                            "page",
                            1,
                            1,
                            Integer.MAX_VALUE
                    );

            boolean ascending =
                    params.getOrDefault("order", "desc")
                            .equalsIgnoreCase("asc");

            boolean onlineOnly =
                    Boolean.parseBoolean(
                            params.getOrDefault("online_only", "false")
                    );

            String defaultPeriod =
                    plugin.getConfig().getString(
                            "leaderboards.default-period",
                            "all_time"
                    );

            Period period =
                    Period.from(
                            params.getOrDefault(
                                    "period",
                                    defaultPeriod
                            )
                    );

            String metric =
                    Metrics.canonical(statName);

            List<LeaderboardEntry> all =
                    plugin.getAnalytics()
                            .leaderboard(metric, period, 0);

            if (onlineOnly) {

                List<LeaderboardEntry> filtered =
                        new ArrayList<>();

                for (LeaderboardEntry entry : all) {

                    if (entry.online) {
                        filtered.add(entry);
                    }
                }

                all = filtered;
            }

            if (ascending) {

                List<LeaderboardEntry> reversed =
                        new ArrayList<>(all);

                java.util.Collections.reverse(reversed);

                all = reversed;
            }

            int total =
                    all.size();

            int offset =
                    (page - 1) * limit;

            int fromIndex =
                    Math.min(offset, total);

            int toIndex =
                    Math.min(fromIndex + limit, total);

            List<Object> entryList =
                    new ArrayList<>();

            int rank =
                    fromIndex + 1;

            for (LeaderboardEntry entry
                    : all.subList(fromIndex, toIndex)) {

                Map<String, Object> map =
                        new LinkedHashMap<>();

                map.put("rank", rank);
                map.put("uuid", entry.uuid);
                map.put("name", entry.name);
                map.put("online", entry.online);
                map.put("value", entry.value);
                map.put("formatted", format(metric, entry));

                entryList.add(map);

                rank++;
            }

            Map<String, Object> metadata =
                    new LinkedHashMap<>();

            metadata.put("period", period.token());
            metadata.put("ascending", ascending);
            metadata.put("online_only", onlineOnly);
            metadata.put(
                    "generated_at",
                    java.time.Instant.now().toString()
            );

            JsonBuilder builder =
                    new JsonBuilder()
                            .add("stat", metric)
                            .add("period", period.token())
                            .add("total", total)
                            .add("limit", limit)
                            .add("page", page)
                            .add("offset", offset)
                            .add("entries", entryList)
                            .add("metadata", metadata);

            ResponseUtil.sendOk(exchange, builder.build());

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "Leaderboard endpoint failure: " + exception.getMessage()
            );

            ResponseUtil.sendInternalError(
                    exchange,
                    "Failed to generate leaderboard"
            );
        }
    }

    // -------------------------------------------------------------------------

    private String format(
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

    private String extractStatName(String path) {

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash =
                path.lastIndexOf('/');

        if (lastSlash < 0) {
            return null;
        }

        return path.substring(lastSlash + 1);
    }

    /**
     * @param uuid player UUID
     * @return whether the player is online
     */
    static boolean isOnline(UUID uuid) {

        return uuid != null
                && Bukkit.getPlayer(uuid) != null;
    }

    /**
     * @param uri request URI (kept for API compatibility)
     * @return parsed query params
     */
    static Map<String, String> parseQueryParams(URI uri) {

        return HttpQuery.parse(uri);
    }
}

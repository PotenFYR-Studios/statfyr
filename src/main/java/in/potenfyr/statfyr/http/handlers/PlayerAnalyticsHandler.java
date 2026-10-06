package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.http.HttpQuery;
import in.potenfyr.statfyr.storage.ActivityEvent;
import in.potenfyr.statfyr.storage.Snapshot;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player analytics endpoints:
 *
 * <ul>
 *     <li>{@code GET /api/player/{player}/history}</li>
 *     <li>{@code GET /api/player/{player}/activity}</li>
 *     <li>{@code GET /api/player/{player}/sessions}</li>
 *     <li>{@code GET /api/player/{player}/retention}</li>
 * </ul>
 */
public final class PlayerAnalyticsHandler {

    private final Statfyr plugin;

    public PlayerAnalyticsHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    /**
     * @return {@code true} when the sub-path was handled
     */
    public boolean handle(
            HttpExchange exchange,
            UUID uuid,
            String name,
            String subPath
    ) throws IOException {

        switch (subPath) {

            case "history":
                sendHistory(exchange, uuid, name);
                return true;

            case "activity":
                sendActivity(exchange, uuid, name);
                return true;

            case "sessions":
                sendSessions(exchange, uuid, name);
                return true;

            case "retention":
                sendRetention(exchange, uuid, name);
                return true;

            default:
                return false;
        }
    }

    private void sendHistory(
            HttpExchange exchange,
            UUID uuid,
            String name
    ) throws IOException {

        Map<String, String> params =
                HttpQuery.parse(exchange.getRequestURI());

        long to =
                HttpQuery.parseTime(
                        params,
                        "to",
                        System.currentTimeMillis()
                );

        long from =
                HttpQuery.parseTime(
                        params,
                        "from",
                        to - 30L * 24L * 60L * 60L * 1000L
                );

        int limit =
                HttpQuery.parseInt(params, "limit", 500, 1, 5000);

        List<Snapshot> snapshots =
                plugin.getAnalytics()
                        .playerHistory(uuid, from, to, limit);

        List<Object> list =
                new ArrayList<>();

        for (Snapshot snapshot : snapshots) {

            Map<String, Object> entry =
                    new LinkedHashMap<>();

            entry.put("timestamp", snapshot.ts);
            entry.put("metrics", snapshot.metrics);

            list.add(entry);
        }

        JsonBuilder builder =
                new JsonBuilder()
                        .add("uuid", uuid.toString())
                        .add("name", name)
                        .add("from", from)
                        .add("to", to)
                        .add("count", list.size())
                        .add("history", list);

        ResponseUtil.sendOk(exchange, builder.build());
    }

    private void sendActivity(
            HttpExchange exchange,
            UUID uuid,
            String name
    ) throws IOException {

        Map<String, String> params =
                HttpQuery.parse(exchange.getRequestURI());

        long to =
                HttpQuery.parseTime(
                        params,
                        "to",
                        System.currentTimeMillis()
                );

        long from =
                HttpQuery.parseTime(
                        params,
                        "from",
                        to - 7L * 24L * 60L * 60L * 1000L
                );

        int limit =
                HttpQuery.parseInt(params, "limit", 200, 1, 2000);

        List<ActivityEvent> events =
                plugin.getAnalytics()
                        .activity(uuid, from, to, limit);

        List<Object> list =
                new ArrayList<>();

        for (ActivityEvent event : events) {

            Map<String, Object> entry =
                    new LinkedHashMap<>();

            entry.put("timestamp", event.ts);
            entry.put("type", event.type);

            if (event.detail != null) {
                entry.put("detail", event.detail);
            }

            entry.put("value", event.value);

            list.add(entry);
        }

        JsonBuilder builder =
                new JsonBuilder()
                        .add("uuid", uuid.toString())
                        .add("name", name)
                        .add("from", from)
                        .add("to", to)
                        .add("count", list.size())
                        .add("activity", list);

        ResponseUtil.sendOk(exchange, builder.build());
    }

    private void sendSessions(
            HttpExchange exchange,
            UUID uuid,
            String name
    ) throws IOException {

        PlayerProfile profile =
                plugin.getAnalytics().profileIfPresent(uuid);

        long now =
                System.currentTimeMillis();

        long currentSession =
                profile == null || profile.currentSessionStart <= 0L
                        ? 0L
                        : (now - profile.currentSessionStart) / 1000L;

        JsonBuilder builder =
                new JsonBuilder()
                        .add("player", name)
                        .add("uuid", uuid.toString());

        if (profile == null) {

            builder.add("sessions", 0)
                    .add("total_playtime", 0)
                    .add("average_session", 0)
                    .add("longest_session", 0)
                    .add("current_session", 0)
                    .add("first_seen", 0)
                    .add("last_seen", 0)
                    .add("online", plugin.getPlayerResolver()
                            .isOnline(uuid));

            ResponseUtil.sendOk(exchange, builder.build());
            return;
        }

        long sessions =
                profile.totalSessions;

        long totalPlaytime =
                profile.totalPlaytimeSeconds;

        builder.add("sessions", sessions)
                .add("total_playtime", totalPlaytime)
                .add("active_time", profile.activeSeconds)
                .add("afk_time", profile.afkSeconds)
                .add(
                        "average_session",
                        sessions == 0L
                                ? 0L
                                : totalPlaytime / sessions
                )
                .add("longest_session", profile.longestSessionSeconds)
                .add("current_session", currentSession)
                .add("first_seen", profile.firstSeen)
                .add("last_seen", profile.lastSeen)
                .add(
                        "online",
                        plugin.getPlayerResolver().isOnline(uuid)
                );

        ResponseUtil.sendOk(exchange, builder.build());
    }

    private void sendRetention(
            HttpExchange exchange,
            UUID uuid,
            String name
    ) throws IOException {

        PlayerProfile profile =
                plugin.getAnalytics().profileIfPresent(uuid);

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("uuid", uuid.toString());
        map.put("name", name);

        if (profile != null) {

            map.put("first_seen", profile.firstSeen);
            map.put("last_seen", profile.lastSeen);
            map.put("segment", plugin.getAnalytics().segment(profile));
            map.put(
                    "sessions",
                    profile.totalSessions
            );
            map.put(
                    "playtime",
                    profile.totalPlaytimeSeconds
            );
        }

        map.put("server_retention", plugin.getAnalytics().retention());
        map.put("generated_at", Instant.now().toString());

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(map)
        );
    }

    /**
     * @return the identifier portion of a player route
     */
    public static String extractIdentifier(String path) {

        String rest =
                path.substring("/api/player/".length());

        int slash =
                rest.indexOf('/');

        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }

        return rest;
    }

    /**
     * @return the sub-path after the identifier, or an empty string
     */
    public static String extractSubPath(String path) {

        String rest =
                path.substring("/api/player/".length());

        int slash =
                rest.indexOf('/');

        if (slash < 0) {
            return "";
        }

        return rest.substring(slash + 1);
    }
}

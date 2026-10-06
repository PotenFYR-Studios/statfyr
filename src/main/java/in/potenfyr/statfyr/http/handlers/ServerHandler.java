package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.analytics.ServerStats;
import in.potenfyr.statfyr.http.HttpQuery;
import in.potenfyr.statfyr.storage.ServerSnapshot;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-level analytics endpoints:
 *
 * <ul>
 *     <li>{@code GET /api/server}</li>
 *     <li>{@code GET /api/server/summary}</li>
 *     <li>{@code GET /api/server/history}</li>
 *     <li>{@code GET /api/server/activity}</li>
 *     <li>{@code GET /api/server/retention}</li>
 *     <li>{@code GET /api/server/segments}</li>
 * </ul>
 */
public final class ServerHandler implements HttpHandler {

    private final Statfyr plugin;

    public ServerHandler(Statfyr plugin) {

        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        String path =
                exchange.getRequestURI().getPath();

        String suffix =
                path.length() > "/api/server".length()
                        ? path.substring("/api/server".length())
                        : "";

        Map<String, String> params =
                HttpQuery.parse(exchange.getRequestURI());

        try {

            switch (suffix) {

                case "":
                case "/":
                case "/summary":
                    sendSummary(exchange);
                    return;

                case "/history":
                    sendHistory(exchange, params);
                    return;

                case "/activity":
                case "/heatmap":
                    sendActivity(exchange);
                    return;

                case "/retention":
                    sendRetention(exchange);
                    return;

                case "/segments":
                    sendSegments(exchange);
                    return;

                case "/economy":
                    sendEconomy(exchange);
                    return;

                default:
                    ResponseUtil.sendNotFound(
                            exchange,
                            "Unknown server endpoint: " + path
                    );
            }

        } catch (Exception exception) {

            plugin.getLogger().warning(
                    "Server endpoint failure: " + exception.getMessage()
            );

            ResponseUtil.sendInternalError(
                    exchange,
                    "Failed to generate server analytics"
            );
        }
    }

    // -------------------------------------------------------------------------

    private void sendSummary(HttpExchange exchange)
            throws IOException {

        ServerStats stats =
                plugin.getAnalytics().serverStats();

        Map<String, Object> map =
                stats.toMap();

        map.put("server_id", plugin.getAnalytics().serverId());
        map.put("server_name", plugin.getAnalytics().serverName());
        map.put("generated_at", Instant.now().toString());

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(map)
        );
    }

    private void sendHistory(
            HttpExchange exchange,
            Map<String, String> params
    ) throws IOException {

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
                HttpQuery.parseInt(params, "limit", 1000, 1, 10000);

        List<ServerSnapshot> snapshots =
                plugin.getAnalytics()
                        .serverHistory(from, to, limit);

        List<Object> list =
                new ArrayList<>();

        for (ServerSnapshot snapshot : snapshots) {

            Map<String, Object> entry =
                    new LinkedHashMap<>();

            entry.put("timestamp", snapshot.ts);
            entry.put("online", snapshot.online);
            entry.put("unique_today", snapshot.uniqueToday);

            list.add(entry);
        }

        JsonBuilder builder =
                new JsonBuilder()
                        .add("from", from)
                        .add("to", to)
                        .add("count", list.size())
                        .add("history", list);

        ResponseUtil.sendOk(exchange, builder.build());
    }

    private void sendActivity(HttpExchange exchange)
            throws IOException {

        Map<String, Object> heatmap =
                plugin.getAnalytics().heatmap();

        heatmap.put("generated_at", Instant.now().toString());

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(heatmap)
        );
    }

    private void sendRetention(HttpExchange exchange)
            throws IOException {

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(
                        plugin.getAnalytics().retention()
                )
        );
    }

    private void sendEconomy(HttpExchange exchange)
            throws IOException {

        Map<String, Object> map =
                plugin.getAnalytics().economyStats();

        map.put("generated_at", Instant.now().toString());

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(map)
        );
    }

    private void sendSegments(HttpExchange exchange)
            throws IOException {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put(
                "segments",
                plugin.getAnalytics().segments()
        );

        List<Object> players =
                new ArrayList<>();

        for (PlayerProfile profile
                : plugin.getAnalytics().allProfiles()) {

            Map<String, Object> entry =
                    new LinkedHashMap<>();

            entry.put("uuid", profile.uuid);
            entry.put("name", profile.name);
            entry.put("segment", plugin.getAnalytics().segment(profile));

            players.add(entry);
        }

        map.put("players", players);

        ResponseUtil.sendOk(
                exchange,
                JsonBuilder.object(map)
        );
    }
}

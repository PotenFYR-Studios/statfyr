package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.player.PlayerService;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GET /api/players
 * Query Params:
 * - limit
 * - page
 * - order
 * - online_only
 * - search
 */
public final class PlayersListHandler implements HttpHandler {

    private final Statfyr plugin;

    public PlayersListHandler(Statfyr plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        try {

            ConfigManager config =
                    plugin.getConfigManager();

            Map<String, String> queryParams =
                    parseQueryParams(
                            exchange.getRequestURI()
                    );

            int limit =
                    parseIntParam(
                            queryParams.get("limit"),
                            config.getDefaultPageLimit(),
                            1,
                            config.getMaxPageLimit()
                    );

            int page =
                    parseIntParam(
                            queryParams.get("page"),
                            1,
                            1,
                            Integer.MAX_VALUE
                    );

            String order =
                    queryParams.getOrDefault(
                            "order",
                            config.getDefaultSortOrder()
                    );

            boolean ascending =
                    order.equalsIgnoreCase("asc");

            boolean onlineOnly =
                    Boolean.parseBoolean(
                            queryParams.getOrDefault(
                                    "online_only",
                                    "false"
                            )
                    );

            String search =
                    queryParams.getOrDefault(
                            "search",
                            ""
                    ).toLowerCase();

            int offset =
                    (page - 1) * limit;

            CompletableFuture
                    .supplyAsync(
                            () -> buildResponse(
                                    limit,
                                    offset,
                                    ascending,
                                    onlineOnly,
                                    search
                            ),
                            plugin.getExecutorService()
                    )
                    .thenAccept(json -> {

                        try {

                            ResponseUtil.sendOk(
                                    exchange,
                                    json
                            );

                        } catch (IOException exception) {

                            plugin.getLogger().warning(
                                    "Failed to send players response"
                            );
                        }
                    })
                    .exceptionally(exception -> {

                        try {

                            plugin.getLogger().severe(
                                    "Players endpoint failure"
                            );

                            exception.printStackTrace();

                            ResponseUtil.sendInternalError(
                                    exchange,
                                    "Failed to generate players list"
                            );

                        } catch (IOException ignored) {
                        }

                        return null;
                    });

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Players endpoint crashed"
            );

            exception.printStackTrace();

            ResponseUtil.sendInternalError(
                    exchange,
                    "Internal server error"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Build Response
    // -------------------------------------------------------------------------

    private String buildResponse(
            int limit,
            int offset,
            boolean ascending,
            boolean onlineOnly,
            String search
    ) {

        long startTime =
                System.currentTimeMillis();

        List<PlayerService.ResolvedPlayer> resolvedPlayers =
                plugin.getPlayerResolver()
                        .getAllPlayers();

        List<PlayerEntry> players =
                new ArrayList<>();

        for (PlayerService.ResolvedPlayer resolvedPlayer
                : resolvedPlayers) {

            try {

                boolean online =
                        Bukkit.getPlayer(
                                resolvedPlayer.getUuid()
                        ) != null;

                if (onlineOnly && !online) {
                    continue;
                }

                String playerName =
                        resolvedPlayer.getName();

                if (playerName == null
                        || playerName.isBlank()) {

                    playerName = "unknown";
                }

                if (!search.isBlank()
                        && !playerName
                        .toLowerCase()
                        .contains(search)) {

                    continue;
                }

                players.add(
                        new PlayerEntry(
                                resolvedPlayer.getUuid()
                                        .toString(),
                                playerName,
                                online
                        )
                );

            } catch (Exception ignored) {
            }
        }

        Comparator<PlayerEntry> comparator =
                Comparator.comparing(
                        PlayerEntry::getName,
                        String.CASE_INSENSITIVE_ORDER
                );

        if (!ascending) {
            comparator = comparator.reversed();
        }

        players.sort(comparator);

        int total =
                players.size();

        int fromIndex =
                Math.min(offset, total);

        int toIndex =
                Math.min(
                        fromIndex + limit,
                        total
                );

        List<PlayerEntry> pageEntries =
                players.subList(
                        fromIndex,
                        toIndex
                );

        List<Object> playerList =
                new ArrayList<>();

        for (PlayerEntry entry
                : pageEntries) {

            Map<String, Object> map =
                    new LinkedHashMap<>();

            map.put(
                    "uuid",
                    entry.getUuid()
            );

            map.put(
                    "name",
                    entry.getName()
            );

            map.put(
                    "online",
                    entry.isOnline()
            );

            playerList.add(map);
        }

        long executionTime =
                System.currentTimeMillis()
                        - startTime;

        JsonBuilder metadata =
                new JsonBuilder()

                        .add(
                                "generated_at",
                                Instant.now().toString()
                        )

                        .add(
                                "execution_time_ms",
                                executionTime
                        )

                        .add(
                                "ascending",
                                ascending
                        )

                        .add(
                                "online_only",
                                onlineOnly
                        )

                        .add(
                                "search",
                                search
                        );

        return new JsonBuilder()

                .add(
                        "total",
                        total
                )

                .add(
                        "limit",
                        limit
                )

                .add(
                        "page",
                        (offset / limit) + 1
                )

                .add(
                        "offset",
                        offset
                )

                .add(
                        "players",
                        playerList
                )

                .add(
                        "metadata",
                        metadata.buildMap()
                )

                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, String> parseQueryParams(
            URI uri
    ) {

        Map<String, String> params =
                new HashMap<>();

        String query =
                uri.getQuery();

        if (query == null
                || query.isBlank()) {

            return params;
        }

        for (String pair
                : query.split("&")) {

            int equalsIndex =
                    pair.indexOf('=');

            if (equalsIndex > 0) {

                String key =
                        pair.substring(
                                0,
                                equalsIndex
                        );

                String value =
                        pair.substring(
                                equalsIndex + 1
                        );

                params.put(
                        key,
                        value
                );
            }
        }

        return params;
    }

    private int parseIntParam(
            String value,
            int defaultValue,
            int min,
            int max
    ) {

        if (value == null) {
            return defaultValue;
        }

        try {

            int parsed =
                    Integer.parseInt(value);

            return Math.max(
                    min,
                    Math.min(max, parsed)
            );

        } catch (NumberFormatException ignored) {

            return defaultValue;
        }
    }

    // -------------------------------------------------------------------------
    // Inner Class
    // -------------------------------------------------------------------------

    private static final class PlayerEntry {

        private final String uuid;

        private final String name;

        private final boolean online;

        PlayerEntry(
                String uuid,
                String name,
                boolean online
        ) {

            this.uuid = uuid;
            this.name = name;
            this.online = online;
        }

        String getUuid() {
            return uuid;
        }

        String getName() {
            return name;
        }

        boolean isOnline() {
            return online;
        }
    }
}
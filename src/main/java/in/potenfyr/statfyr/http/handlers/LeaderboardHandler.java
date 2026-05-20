package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.config.ConfigManager;
import in.potenfyr.statfyr.model.PlayerStats;
import in.potenfyr.statfyr.player.PlayerService;
import in.potenfyr.statfyr.stats.StatKeys;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GET /api/leaderboard/{stat}
 * <p>
 * Query Params:
 * - limit
 * - page
 * - order
 * - online_only
 */
public final class LeaderboardHandler implements HttpHandler {

    private final Statfyr plugin;

    public LeaderboardHandler(Statfyr plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        long start =
                System.currentTimeMillis();

        try {

            String statName =
                    extractStatName(
                            exchange.getRequestURI()
                                    .getPath()
                    );

            if (statName == null
                    || statName.isBlank()) {

                ResponseUtil.sendBadRequest(
                        exchange,
                        "Stat name is required"
                );

                return;
            }

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

            int offset =
                    (page - 1) * limit;

            CompletableFuture
                    .supplyAsync(
                            () -> buildLeaderboard(
                                    statName,
                                    limit,
                                    offset,
                                    ascending,
                                    onlineOnly
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
                                    "Failed to send leaderboard response"
                            );
                        }
                    })
                    .exceptionally(exception -> {

                        try {

                            plugin.getLogger().severe(
                                    "Leaderboard generation failed"
                            );

                            exception.printStackTrace();

                            ResponseUtil.sendInternalError(
                                    exchange,
                                    "Failed to generate leaderboard"
                            );

                        } catch (IOException ignored) {
                        }

                        return null;
                    });

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Leaderboard endpoint failure"
            );

            exception.printStackTrace();

            ResponseUtil.sendInternalError(
                    exchange,
                    "Internal server error"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Build Leaderboard
    // -------------------------------------------------------------------------

    private String buildLeaderboard(
            String statName,
            int limit,
            int offset,
            boolean ascending,
            boolean onlineOnly
    ) {

        long startTime =
                System.currentTimeMillis();

        List<PlayerService.ResolvedPlayer> allPlayers =
                plugin.getPlayerResolver()
                        .getAllPlayers();

        List<LeaderboardEntry> entries =
                new ArrayList<>();

        for (PlayerService.ResolvedPlayer player
                : allPlayers) {

            try {

                boolean online =
                        Bukkit.getPlayer(
                                player.getUuid()
                        ) != null;

                if (onlineOnly && !online) {
                    continue;
                }

                PlayerStats playerStats =
                        plugin.getStatsManager()
                                .getPlayerStats(
                                        player.getUuid()
                                );

                long value =
                        extractStatValue(
                                playerStats,
                                statName
                        );

                entries.add(
                        new LeaderboardEntry(
                                player.getUuid(),
                                player.getName(),
                                value,
                                online,
                                playerStats
                        )
                );

            } catch (Exception ignored) {
            }
        }

        Comparator<LeaderboardEntry> comparator =
                Comparator.comparingLong(
                        LeaderboardEntry::getValue
                );

        if (!ascending) {
            comparator = comparator.reversed();
        }

        entries.sort(comparator);

        int totalCount =
                entries.size();

        int fromIndex =
                Math.min(offset, totalCount);

        int toIndex =
                Math.min(
                        fromIndex + limit,
                        totalCount
                );

        List<LeaderboardEntry> pageEntries =
                entries.subList(
                        fromIndex,
                        toIndex
                );

        List<Object> entryList =
                new ArrayList<>();

        int rank =
                fromIndex + 1;

        for (LeaderboardEntry entry
                : pageEntries) {

            Map<String, Object> entryMap =
                    new LinkedHashMap<>();

            entryMap.put(
                    "rank",
                    rank
            );

            entryMap.put(
                    "uuid",
                    entry.getUuid().toString()
            );

            entryMap.put(
                    "name",
                    entry.getName() != null
                            ? entry.getName()
                            : "unknown"
            );

            entryMap.put(
                    "online",
                    entry.isOnline()
            );

            entryMap.put(
                    "value",
                    entry.getValue()
            );

            // Extra formatting for playtime
            if (statName.equals("playtime")) {

                entryMap.put(
                        "formatted",
                        entry.getPlayerStats()
                                .getFormattedPlayTime()
                );
            }

            entryList.add(entryMap);

            rank++;
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
                        );

        return new JsonBuilder()

                .add(
                        "stat",
                        statName
                )

                .add(
                        "total",
                        totalCount
                )

                .add(
                        "limit",
                        limit
                )

                .add(
                        "offset",
                        offset
                )

                .add(
                        "page",
                        (offset / limit) + 1
                )

                .add(
                        "entries",
                        entryList
                )

                .add(
                        "metadata",
                        metadata.buildMap()
                )

                .build();
    }

    // -------------------------------------------------------------------------
    // Stat Resolution
    // -------------------------------------------------------------------------

    private long extractStatValue(PlayerStats playerStats, String statName) {

        if (playerStats == null || statName == null || statName.isBlank()) {
            return 0L;
        }

        String trimmed = statName.trim();


        switch (trimmed.toLowerCase()) {
            case "playtime":
                return playerStats.getPlayTimeTicks();
            case "deaths":
                return playerStats.getDeaths();
            case "player_kills":
                return playerStats.getPlayerKills();
            case "mob_kills":
                return playerStats.getMobKills();
            case "blocks_mined":
                return playerStats.getBlocksMined();
            case "items_picked_up":
                return playerStats.getItemsPickedUp();
            case "items_crafted":
                return playerStats.getItemsCrafted();

        }

        if (trimmed.contains(".")) {
            String[] parts = trimmed.split("\\.", 2);
            String categoryAlias = parts[0].toLowerCase();
            String key = parts[1];

            String actualCategory = getCategoryFromAlias(categoryAlias);
            if (actualCategory != null) {
                return playerStats.getStat(actualCategory, normalizeKey(key));
            }
        }

        // 3. Support full vanilla format: minecraft:mined:minecraft:sand
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 3);
            if (parts.length == 2) {
                return playerStats.getStat(parts[0], parts[1]);
            } else if (parts.length == 3) {
                // minecraft:mined:minecraft:sand
                return playerStats.getStat(parts[0] + ":" + parts[1], parts[2]);
            }
        }

        // 4. Direct category lookup (if someone passes raw category name)
        if (playerStats.hasCategory(trimmed)) {
            // If only category is passed, return total of that category
            return playerStats.getCategory(trimmed).values().stream().mapToLong(Long::longValue).sum();
        }

        // 5. Final fallback - custom stat
        return playerStats.getStat(StatKeys.CATEGORY_CUSTOM, trimmed);
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getCategoryFromAlias(String alias) {
        return switch (alias) {
            case "mined", "mine" -> StatKeys.CATEGORY_MINED;
            case "pickedup", "picked_up", "pickup" -> StatKeys.CATEGORY_PICKED_UP;
            case "used", "use" -> StatKeys.CATEGORY_USED;
            case "broken", "break" -> StatKeys.CATEGORY_BROKEN;
            case "crafted", "craft" -> StatKeys.CATEGORY_CRAFTED;
            case "dropped", "drop" -> StatKeys.CATEGORY_DROPPED;
            case "killed", "kill" -> StatKeys.CATEGORY_KILLED;
            case "killedby", "killed_by" -> StatKeys.CATEGORY_KILLED_BY;
            default -> null;
        };
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        String trimmed = key.trim();
        if (trimmed.contains(":")) {
            return trimmed;
        }
        return "minecraft:" + trimmed;
    }


    private String extractStatName(String path) {

        if (path.endsWith("/")) {
            path =
                    path.substring(
                            0,
                            path.length() - 1
                    );
        }

        int lastSlash =
                path.lastIndexOf('/');

        if (lastSlash < 0) {
            return null;
        }

        return path.substring(lastSlash + 1);
    }

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

    private static final class LeaderboardEntry {

        private final UUID uuid;

        private final String name;

        private final long value;

        private final boolean online;

        private final PlayerStats playerStats;

        LeaderboardEntry(
                UUID uuid,
                String name,
                long value,
                boolean online,
                PlayerStats playerStats
        ) {

            this.uuid = uuid;
            this.name = name;
            this.value = value;
            this.online = online;
            this.playerStats = playerStats;
        }

        UUID getUuid() {
            return uuid;
        }

        String getName() {
            return name;
        }

        long getValue() {
            return value;
        }

        boolean isOnline() {
            return online;
        }

        PlayerStats getPlayerStats() {
            return playerStats;
        }
    }
}
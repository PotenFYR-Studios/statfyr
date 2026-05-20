package in.potenfyr.statfyr.http.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.model.PlayerStats;
import in.potenfyr.statfyr.util.JsonBuilder;
import in.potenfyr.statfyr.util.ResponseUtil;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GET /api/player/{uuid|name}
 *
 * Query Params:
 * - summary=true
 * - raw=false
 * - categories=mined,crafted
 */
public final class PlayerStatsHandler implements HttpHandler {

    private final Statfyr plugin;

    public PlayerStatsHandler(Statfyr plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        long startTime =
                System.currentTimeMillis();

        try {

            String identifier =
                    extractIdentifier(
                            exchange.getRequestURI()
                                    .getPath()
                    );

            if (identifier == null
                    || identifier.isBlank()) {

                ResponseUtil.sendBadRequest(
                        exchange,
                        "Player identifier is required"
                );

                return;
            }

            UUID playerUuid =
                    plugin.getPlayerResolver()
                            .resolveUuid(identifier);

            if (playerUuid == null) {

                ResponseUtil.sendNotFound(
                        exchange,
                        "Player not found: " + identifier
                );

                return;
            }

            Map<String, String> queryParams =
                    parseQueryParams(
                            exchange.getRequestURI()
                    );

            boolean includeSummary =
                    Boolean.parseBoolean(
                            queryParams.getOrDefault(
                                    "summary",
                                    "true"
                            )
                    );

            boolean includeRaw =
                    Boolean.parseBoolean(
                            queryParams.getOrDefault(
                                    "raw",
                                    "true"
                            )
                    );

            String categoriesFilter =
                    queryParams.getOrDefault(
                            "categories",
                            ""
                    );

            CompletableFuture
                    .supplyAsync(
                            () -> buildResponse(
                                    playerUuid,
                                    includeSummary,
                                    includeRaw,
                                    categoriesFilter,
                                    startTime
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
                                    "Failed to send player stats response"
                            );
                        }
                    })
                    .exceptionally(exception -> {

                        try {

                            plugin.getLogger().severe(
                                    "Player stats endpoint failure"
                            );

                            exception.printStackTrace();

                            ResponseUtil.sendInternalError(
                                    exchange,
                                    "Failed to generate player stats"
                            );

                        } catch (IOException ignored) {
                        }

                        return null;
                    });

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Player stats handler crashed"
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
            UUID playerUuid,
            boolean includeSummary,
            boolean includeRaw,
            String categoriesFilter,
            long requestStart
    ) {

        PlayerStats playerStats =
                plugin.getStatsManager()
                        .getPlayerStats(playerUuid);

        if (playerStats == null) {

            return new JsonBuilder()

                    .add(
                            "error",
                            true
                    )

                    .add(
                            "message",
                            "Stats not found"
                    )

                    .build();
        }

        boolean online =
                Bukkit.getPlayer(playerUuid) != null;

        String playerName =
                playerStats.getPlayerName();

        if (playerName == null
                || playerName.isBlank()) {

            playerName =
                    Bukkit.getOfflinePlayer(playerUuid)
                            .getName();
        }

        if (playerName == null
                || playerName.isBlank()) {

            playerName = "unknown";
        }

        JsonBuilder response =
                new JsonBuilder()

                        .add(
                                "uuid",
                                playerUuid.toString()
                        )

                        .add(
                                "name",
                                playerName
                        )

                        .add(
                                "online",
                                online
                        )

                        .add(
                                "readTimestamp",
                                playerStats.getReadTimestamp()
                        );

        // ---------------------------------------------------------------------
        // Summary
        // ---------------------------------------------------------------------

        if (includeSummary) {

            Map<String, Object> summary =
                    new LinkedHashMap<>();

            summary.put(
                    "playTimeTicks",
                    playerStats.getPlayTimeTicks()
            );

            summary.put(
                    "playTimeFormatted",
                    playerStats.getFormattedPlayTime()
            );

            summary.put(
                    "deaths",
                    playerStats.getDeaths()
            );

            summary.put(
                    "playerKills",
                    playerStats.getPlayerKills()
            );

            summary.put(
                    "mobKills",
                    playerStats.getMobKills()
            );

            summary.put(
                    "damageDealt",
                    playerStats.getDamageDealt()
            );

            summary.put(
                    "damageTaken",
                    playerStats.getDamageTaken()
            );

            summary.put(
                    "jumps",
                    playerStats.getJumps()
            );

            summary.put(
                    "distanceCm",
                    playerStats.getTotalDistanceCm()
            );

            summary.put(
                    "distanceKm",
                    playerStats.getTotalDistanceKm()
            );

            summary.put(
                    "chestsOpened",
                    playerStats.getChestsOpened()
            );

            summary.put(
                    "itemsCrafted",
                    playerStats.getItemsCrafted()
            );

            summary.put(
                    "itemsBroken",
                    playerStats.getItemsBroken()
            );

            summary.put(
                    "itemsUsed",
                    playerStats.getItemsUsed()
            );

            summary.put(
                    "itemsDropped",
                    playerStats.getItemsDropped()
            );

            summary.put(
                    "itemsPickedUp",
                    playerStats.getItemsPickedUp()
            );

            summary.put(
                    "blocksMined",
                    playerStats.getBlocksMined()
            );

            response.add(
                    "summary",
                    summary
            );
        }

        // ---------------------------------------------------------------------
        // Categories
        // ---------------------------------------------------------------------

        List<String> requestedCategories =
                parseCategories(
                        categoriesFilter
                );

        addCategory(
                response,
                requestedCategories,
                "crafted",
                playerStats.getCraftedItems()
        );

        addCategory(
                response,
                requestedCategories,
                "mined",
                playerStats.getMinedBlocks()
        );

        addCategory(
                response,
                requestedCategories,
                "used",
                playerStats.getUsedItems()
        );

        addCategory(
                response,
                requestedCategories,
                "broken",
                playerStats.getBrokenItems()
        );

        addCategory(
                response,
                requestedCategories,
                "pickedUp",
                playerStats.getPickedUpItems()
        );

        addCategory(
                response,
                requestedCategories,
                "dropped",
                playerStats.getDroppedItems()
        );

        addCategory(
                response,
                requestedCategories,
                "killed",
                playerStats.getKilledEntities()
        );

        addCategory(
                response,
                requestedCategories,
                "killedBy",
                playerStats.getKilledByEntities()
        );

        // ---------------------------------------------------------------------
        // Raw Stats
        // ---------------------------------------------------------------------

        if (includeRaw) {

            Map<String, Object> rawStatsMap =
                    new LinkedHashMap<>();

            for (Map.Entry<String, Map<String, Long>> categoryEntry
                    : playerStats.getRawStats().entrySet()) {

                Map<String, Object> categoryMap =
                        new LinkedHashMap<>();

                for (Map.Entry<String, Long> statEntry
                        : categoryEntry.getValue().entrySet()) {

                    categoryMap.put(
                            statEntry.getKey(),
                            statEntry.getValue()
                    );
                }

                rawStatsMap.put(
                        categoryEntry.getKey(),
                        categoryMap
                );
            }

            response.add(
                    "stats",
                    rawStatsMap
            );
        }

        // ---------------------------------------------------------------------
        // Metadata
        // ---------------------------------------------------------------------

        JsonBuilder metadata =
                new JsonBuilder()

                        .add(
                                "generated_at",
                                Instant.now().toString()
                        )

                        .add(
                                "execution_time_ms",
                                System.currentTimeMillis()
                                        - requestStart
                        )

                        .add(
                                "summary_enabled",
                                includeSummary
                        )

                        .add(
                                "raw_enabled",
                                includeRaw
                        )

                        .add(
                                "categories_filter",
                                categoriesFilter
                        );

        response.add(
                "metadata",
                metadata.buildMap()
        );

        return response.build();
    }

    // -------------------------------------------------------------------------
    // Categories
    // -------------------------------------------------------------------------

    private void addCategory(
            JsonBuilder response,
            List<String> requestedCategories,
            String name,
            Map<String, Long> data
    ) {

        if (!requestedCategories.isEmpty()
                && !requestedCategories.contains(name)) {

            return;
        }

        response.add(
                name,
                data
        );
    }

    private List<String> parseCategories(
            String input
    ) {

        List<String> categories =
                new ArrayList<>();

        if (input == null
                || input.isBlank()) {

            return categories;
        }

        for (String value
                : input.split(",")) {

            String trimmed =
                    value.trim();

            if (!trimmed.isBlank()) {
                categories.add(trimmed);
            }
        }

        return categories;
    }

    // -------------------------------------------------------------------------
    // Query Params
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

    // -------------------------------------------------------------------------
    // Path Parsing
    // -------------------------------------------------------------------------

    static String extractIdentifier(
            String path
    ) {

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

        return path.substring(
                lastSlash + 1
        );
    }
}
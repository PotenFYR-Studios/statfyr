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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GET /api/player/{uuid|name}/summary
 *
 * Query Params:
 * - movement=true
 * - combat=true
 * - activity=true
 */
public final class PlayerSummaryHandler implements HttpHandler {

    private final Statfyr plugin;

    public PlayerSummaryHandler(Statfyr plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        long startTime =
                System.currentTimeMillis();

        try {

            String path =
                    exchange.getRequestURI()
                            .getPath();

            String identifier =
                    extractIdentifier(path);

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

            boolean includeMovement =
                    Boolean.parseBoolean(
                            queryParams.getOrDefault(
                                    "movement",
                                    "true"
                            )
                    );

            boolean includeCombat =
                    Boolean.parseBoolean(
                            queryParams.getOrDefault(
                                    "combat",
                                    "true"
                            )
                    );

            boolean includeActivity =
                    Boolean.parseBoolean(
                            queryParams.getOrDefault(
                                    "activity",
                                    "true"
                            )
                    );

            CompletableFuture
                    .supplyAsync(
                            () -> buildSummaryResponse(
                                    playerUuid,
                                    includeMovement,
                                    includeCombat,
                                    includeActivity,
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
                                    "Failed to send summary response"
                            );
                        }
                    })
                    .exceptionally(exception -> {

                        try {

                            plugin.getLogger().severe(
                                    "Player summary generation failed"
                            );

                            exception.printStackTrace();

                            ResponseUtil.sendInternalError(
                                    exchange,
                                    "Failed to generate player summary"
                            );

                        } catch (IOException ignored) {
                        }

                        return null;
                    });

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Player summary endpoint crashed"
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

    private String buildSummaryResponse(
            UUID playerUuid,
            boolean includeMovement,
            boolean includeCombat,
            boolean includeActivity,
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

        long playtimeTicks =
                playerStats.getPlayTimeTicks();

        long playtimeSeconds =
                playtimeTicks / 20L;

        long totalDistanceCm =
                playerStats.getTotalDistanceCm();

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
                                "playtime_ticks",
                                playtimeTicks
                        )

                        .add(
                                "playtime_seconds",
                                playtimeSeconds
                        )

                        .add(
                                "playtime_formatted",
                                playerStats.getFormattedPlayTime()
                        );

        // ---------------------------------------------------------------------
        // Combat
        // ---------------------------------------------------------------------

        if (includeCombat) {

            JsonBuilder combat =
                    new JsonBuilder()

                            .add(
                                    "deaths",
                                    playerStats.getDeaths()
                            )

                            .add(
                                    "player_kills",
                                    playerStats.getPlayerKills()
                            )

                            .add(
                                    "mob_kills",
                                    playerStats.getMobKills()
                            )

                            .add(
                                    "damage_dealt",
                                    playerStats.getDamageDealt()
                            )

                            .add(
                                    "damage_taken",
                                    playerStats.getDamageTaken()
                            );

            response.add(
                    "combat",
                    combat.buildMap()
            );
        }

        // ---------------------------------------------------------------------
        // Movement
        // ---------------------------------------------------------------------

        if (includeMovement) {

            JsonBuilder movement =
                    new JsonBuilder()

                            .add(
                                    "distance_walked_cm",
                                    playerStats.getWalkDistanceCm()
                            )

                            .add(
                                    "distance_walked_m",
                                    playerStats.getWalkDistanceCm()
                                            / 100L
                            )

                            .add(
                                    "distance_sprinted_cm",
                                    playerStats.getSprintDistanceCm()
                            )

                            .add(
                                    "distance_sprinted_m",
                                    playerStats.getSprintDistanceCm()
                                            / 100L
                            )

                            .add(
                                    "distance_flown_cm",
                                    playerStats.getFlyDistanceCm()
                            )

                            .add(
                                    "distance_flown_m",
                                    playerStats.getFlyDistanceCm()
                                            / 100L
                            )

                            .add(
                                    "distance_swum_cm",
                                    playerStats.getSwimDistanceCm()
                            )

                            .add(
                                    "distance_swum_m",
                                    playerStats.getSwimDistanceCm()
                                            / 100L
                            )

                            .add(
                                    "total_distance_cm",
                                    totalDistanceCm
                            )

                            .add(
                                    "total_distance_m",
                                    totalDistanceCm / 100L
                            )

                            .add(
                                    "total_distance_km",
                                    playerStats.getTotalDistanceKm()
                            )

                            .add(
                                    "jumps",
                                    playerStats.getJumps()
                            );

            response.add(
                    "movement",
                    movement.buildMap()
            );
        }

        // ---------------------------------------------------------------------
        // Activity
        // ---------------------------------------------------------------------

        if (includeActivity) {

            JsonBuilder activity =
                    new JsonBuilder()

                            .add(
                                    "chests_opened",
                                    playerStats.getChestsOpened()
                            )

                            .add(
                                    "items_crafted",
                                    playerStats.getItemsCrafted()
                            )

                            .add(
                                    "items_broken",
                                    playerStats.getItemsBroken()
                            )

                            .add(
                                    "items_used",
                                    playerStats.getItemsUsed()
                            )

                            .add(
                                    "items_picked_up",
                                    playerStats.getItemsPickedUp()
                            )

                            .add(
                                    "items_dropped",
                                    playerStats.getItemsDropped()
                            )

                            .add(
                                    "blocks_mined",
                                    playerStats.getBlocksMined()
                            );

            response.add(
                    "activity",
                    activity.buildMap()
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
                                "movement_enabled",
                                includeMovement
                        )

                        .add(
                                "combat_enabled",
                                includeCombat
                        )

                        .add(
                                "activity_enabled",
                                includeActivity
                        );

        response.add(
                "metadata",
                metadata.buildMap()
        );

        return response.build();
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

    private String extractIdentifier(
            String path
    ) {

        if (path.endsWith("/summary")) {

            path =
                    path.substring(
                            0,
                            path.length()
                                    - "/summary".length()
                    );
        }

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
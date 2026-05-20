package in.potenfyr.statfyr.stats;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central stats management system.
 *
 * Responsibilities:
 * - live player stat collection
 * - offline stat caching
 * - async stat loading
 * - cache expiration
 * - periodic refresh
 * - future websocket broadcasting
 */
public final class StatsManager {

    /**
     * Cache TTL.
     */
    private static final long CACHE_TTL_MS =
            30_000L;

    /**
     * Online refresh interval.
     */
    private static final long UPDATE_INTERVAL_TICKS =
            100L;

    /**
     * Precomputed materials.
     */
    private static final Material[] VALID_ITEMS =
            Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .toArray(Material[]::new);

    /**
     * Precomputed entities.
     */
    private static final EntityType[] VALID_ENTITIES =
            Arrays.stream(EntityType.values())
                    .filter(EntityType::isAlive)
                    .toArray(EntityType[]::new);

    private final Statfyr plugin;

    private final StatsReader statsReader;

    /**
     * Main cache.
     */
    private final ConcurrentHashMap<UUID, CachedStats>
            cache =
            new ConcurrentHashMap<>();

    /**
     * Cache metrics.
     */
    private final AtomicLong cacheHits =
            new AtomicLong();

    private final AtomicLong cacheMisses =
            new AtomicLong();

    public StatsManager(
            Statfyr plugin,
            StatsReader statsReader
    ) {

        this.plugin = plugin;
        this.statsReader = statsReader;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::refreshOnlinePlayers,
                20L,
                UPDATE_INTERVAL_TICKS
        );

        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::cleanupExpiredCache,
                20L * 60L,
                20L * 60L
        );

        plugin.getLogger().info(
                "StatsManager started."
        );
    }

    // -------------------------------------------------------------------------
    // Main Access
    // -------------------------------------------------------------------------

    public PlayerStats getPlayerStats(
            UUID uuid
    ) {

        if (uuid == null) {
            return null;
        }

        CachedStats cached =
                cache.get(uuid);

        if (cached != null
                && !cached.isExpired()) {

            cacheHits.incrementAndGet();

            return cached.stats;
        }

        cacheMisses.incrementAndGet();

        Player onlinePlayer =
                Bukkit.getPlayer(uuid);

        PlayerStats stats;

        // LIVE PLAYER
        if (onlinePlayer != null
                && onlinePlayer.isOnline()) {

            stats =
                    readLiveStats(onlinePlayer);

        } else {

            stats =
                    statsReader.readStats(
                            uuid,
                            null
                    );
        }

        cache.put(
                uuid,
                new CachedStats(stats)
        );

        return stats;
    }

    // -------------------------------------------------------------------------
    // Async Access
    // -------------------------------------------------------------------------

    public CompletableFuture<PlayerStats>
    getPlayerStatsAsync(
            UUID uuid
    ) {

        return CompletableFuture.supplyAsync(
                () -> getPlayerStats(uuid),
                plugin.getExecutorService()
        );
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    private void refreshOnlinePlayers() {

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            try {

                PlayerStats stats =
                        readLiveStats(player);

                cache.put(
                        player.getUniqueId(),
                        new CachedStats(stats)
                );

            } catch (Exception exception) {

                plugin.getLogger().warning(
                        "Failed to refresh stats for "
                                + player.getName()
                );

                exception.printStackTrace();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Live Reading
    // -------------------------------------------------------------------------

    private PlayerStats readLiveStats(
            Player player
    ) {

        Map<String, Map<String, Long>> stats =
                new HashMap<>();

        Map<String, Long> custom =
                new HashMap<>();

        Map<String, Long> mined =
                new HashMap<>();

        Map<String, Long> crafted =
                new HashMap<>();

        Map<String, Long> used =
                new HashMap<>();

        Map<String, Long> broken =
                new HashMap<>();

        Map<String, Long> pickedUp =
                new HashMap<>();

        Map<String, Long> dropped =
                new HashMap<>();

        Map<String, Long> killed =
                new HashMap<>();

        Map<String, Long> killedBy =
                new HashMap<>();

        for (Statistic statistic
                : Statistic.values()) {

            try {

                switch (statistic.getType()) {

                    case UNTYPED -> {

                        int value =
                                player.getStatistic(
                                        statistic
                                );

                        if (value <= 0) {
                            continue;
                        }

                        String key =
                                switch (statistic) {

                                    case PLAY_ONE_MINUTE ->
                                            StatKeys.PLAY_TIME;

                                    default ->
                                            "minecraft:"
                                                    + statistic.name()
                                                    .toLowerCase();
                                };

                        custom.put(
                                key,
                                (long) value
                        );
                    }

                    case BLOCK, ITEM -> {

                        for (Material material
                                : VALID_ITEMS) {

                            try {

                                int value =
                                        player.getStatistic(
                                                statistic,
                                                material
                                        );

                                if (value <= 0) {
                                    continue;
                                }

                                String key =
                                        "minecraft:"
                                                + material.name()
                                                .toLowerCase();

                                switch (statistic) {

                                    case MINE_BLOCK ->
                                            mined.put(
                                                    key,
                                                    (long) value
                                            );

                                    case CRAFT_ITEM ->
                                            crafted.put(
                                                    key,
                                                    (long) value
                                            );

                                    case USE_ITEM ->
                                            used.put(
                                                    key,
                                                    (long) value
                                            );

                                    case BREAK_ITEM ->
                                            broken.put(
                                                    key,
                                                    (long) value
                                            );

                                    case PICKUP ->
                                            pickedUp.put(
                                                    key,
                                                    (long) value
                                            );

                                    case DROP ->
                                            dropped.put(
                                                    key,
                                                    (long) value
                                            );
                                }

                            } catch (Exception ignored) {
                            }
                        }
                    }

                    case ENTITY -> {

                        for (EntityType entityType
                                : VALID_ENTITIES) {

                            try {

                                int value =
                                        player.getStatistic(
                                                statistic,
                                                entityType
                                        );

                                if (value <= 0) {
                                    continue;
                                }

                                String key =
                                        "minecraft:"
                                                + entityType.name()
                                                .toLowerCase();

                                switch (statistic) {

                                    case KILL_ENTITY ->
                                            killed.put(
                                                    key,
                                                    (long) value
                                            );

                                    case ENTITY_KILLED_BY ->
                                            killedBy.put(
                                                    key,
                                                    (long) value
                                            );
                                }

                            } catch (Exception ignored) {
                            }
                        }
                    }
                }

            } catch (Exception ignored) {
            }
        }

        stats.put(StatKeys.CATEGORY_CUSTOM, custom);
        stats.put(StatKeys.CATEGORY_MINED, mined);
        stats.put(StatKeys.CATEGORY_CRAFTED, crafted);
        stats.put(StatKeys.CATEGORY_USED, used);
        stats.put(StatKeys.CATEGORY_BROKEN, broken);
        stats.put(StatKeys.CATEGORY_PICKED_UP, pickedUp);
        stats.put(StatKeys.CATEGORY_DROPPED, dropped);
        stats.put(StatKeys.CATEGORY_KILLED, killed);
        stats.put(StatKeys.CATEGORY_KILLED_BY, killedBy);

        return new PlayerStats(
                player.getUniqueId(),
                player.getName(),
                stats
        );
    }

    // -------------------------------------------------------------------------
    // Cache Cleanup
    // -------------------------------------------------------------------------

    private void cleanupExpiredCache() {

        long now =
                System.currentTimeMillis();

        cache.entrySet().removeIf(entry ->
                now - entry.getValue().timestamp
                        > CACHE_TTL_MS
        );
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    public int getCacheSize() {
        return cache.size();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public void clearCache() {
        cache.clear();
    }

    // -------------------------------------------------------------------------
    // Cached Entry
    // -------------------------------------------------------------------------

    private static final class CachedStats {

        private final PlayerStats stats;

        private final long timestamp;

        CachedStats(
                PlayerStats stats
        ) {

            this.stats = stats;
            this.timestamp =
                    System.currentTimeMillis();
        }

        boolean isExpired() {

            return System.currentTimeMillis()
                    - timestamp
                    > CACHE_TTL_MS;
        }
    }
}
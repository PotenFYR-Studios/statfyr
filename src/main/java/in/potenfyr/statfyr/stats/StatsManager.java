package in.potenfyr.statfyr.stats;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.compat.SchedulerCompat;
import in.potenfyr.statfyr.compat.ServerVersion;
import in.potenfyr.statfyr.compat.StatisticCompat;
import in.potenfyr.statfyr.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central stats management system.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>live player stat collection</li>
 *     <li>offline stat caching</li>
 *     <li>async stat loading</li>
 *     <li>cache expiration</li>
 *     <li>periodic refresh</li>
 * </ul>
 *
 * <p>Live collection is fully version agnostic: the statistic category is
 * derived from the stable enum name and material/entity filtering is resolved
 * through {@link StatisticCompat}. This keeps the same binary working on
 * Minecraft 1.8.x–26.x across Bukkit, Spigot, Paper, Purpur and Folia.
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

        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::refreshOnlinePlayers,
                20L,
                UPDATE_INTERVAL_TICKS
        );

        SchedulerCompat.runAsyncRepeating(
                plugin,
                this::cleanupExpiredCache,
                20L * 60L,
                20L * 60L
        );

        plugin.getLogger().info(
                "StatsManager started on "
                        + ServerVersion.getPlatformLabel()
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

                if (stats != null) {

                    cache.put(
                            player.getUniqueId(),
                            new CachedStats(stats)
                    );
                }

            } catch (Throwable throwable) {

                if (plugin.getConfigManager() != null
                        && plugin.getConfigManager()
                        .isDebug()) {

                    plugin.getLogger().warning(
                            "Failed to refresh stats for "
                                    + player.getName()
                                    + ": "
                                    + throwable.getMessage()
                    );
                }
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

        Material[] itemMaterials =
                StatisticCompat.getItemMaterials();

        Material[] blockMaterials =
                StatisticCompat.getBlockMaterials();

        EntityType[] entityTypes =
                StatisticCompat.getLivingEntityTypes();

        for (Statistic statistic
                : Statistic.values()) {

            try {

                String category =
                        StatisticCompat.categoryOf(statistic);

                if (StatisticCompat.UNTYPED.equals(category)) {

                    collectUntyped(
                            player,
                            statistic,
                            custom
                    );

                } else if (StatisticCompat.BLOCK.equals(category)) {

                    collectMaterials(
                            player,
                            statistic,
                            blockMaterials,
                            mined
                    );

                } else if (StatisticCompat.ITEM.equals(category)) {

                    collectMaterials(
                            player,
                            statistic,
                            itemMaterials,
                            itemTarget(statistic, crafted, used, broken, pickedUp, dropped)
                    );

                } else if (StatisticCompat.ENTITY.equals(category)) {

                    collectEntities(
                            player,
                            statistic,
                            entityTypes,
                            entityTarget(statistic, killed, killedBy)
                    );
                }

            } catch (Throwable ignored) {
                // One bad statistic must never break the whole snapshot.
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
    // Collection helpers
    // -------------------------------------------------------------------------

    private void collectUntyped(
            Player player,
            Statistic statistic,
            Map<String, Long> target
    ) {

        int value =
                player.getStatistic(statistic);

        if (value <= 0) {
            return;
        }

        String key =
                StatisticCompat.isPlayTime(statistic)
                        ? StatKeys.PLAY_TIME
                        : "minecraft:" + statistic.name()
                        .toLowerCase(Locale.ROOT);

        target.put(
                key,
                (long) value
        );
    }

    private void collectMaterials(
            Player player,
            Statistic statistic,
            Material[] materials,
            Map<String, Long> target
    ) {

        if (target == null) {
            return;
        }

        for (Material material : materials) {

            try {

                int value =
                        player.getStatistic(
                                statistic,
                                material
                        );

                if (value <= 0) {
                    continue;
                }

                target.put(
                        "minecraft:"
                                + material.name()
                                .toLowerCase(Locale.ROOT),
                        (long) value
                );

            } catch (Throwable ignored) {
                // Material is not valid for this statistic on this version.
            }
        }
    }

    private void collectEntities(
            Player player,
            Statistic statistic,
            EntityType[] entityTypes,
            Map<String, Long> target
    ) {

        if (target == null) {
            return;
        }

        for (EntityType entityType : entityTypes) {

            try {

                int value =
                        player.getStatistic(
                                statistic,
                                entityType
                        );

                if (value <= 0) {
                    continue;
                }

                target.put(
                        "minecraft:"
                                + entityType.name()
                                .toLowerCase(Locale.ROOT),
                        (long) value
                );

            } catch (Throwable ignored) {
                // Entity type is not valid for this statistic on this version.
            }
        }
    }

    private Map<String, Long> itemTarget(
            Statistic statistic,
            Map<String, Long> crafted,
            Map<String, Long> used,
            Map<String, Long> broken,
            Map<String, Long> pickedUp,
            Map<String, Long> dropped
    ) {

        switch (statistic.name()) {

            case "CRAFT_ITEM":
                return crafted;

            case "USE_ITEM":
                return used;

            case "BREAK_ITEM":
                return broken;

            case "PICKUP":
                return pickedUp;

            case "DROP":
                return dropped;

            default:
                return null;
        }
    }

    private Map<String, Long> entityTarget(
            Statistic statistic,
            Map<String, Long> killed,
            Map<String, Long> killedBy
    ) {

        switch (statistic.name()) {

            case "KILL_ENTITY":
                return killed;

            case "ENTITY_KILLED_BY":
                return killedBy;

            default:
                return null;
        }
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

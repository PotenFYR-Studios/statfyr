package in.potenfyr.statfyr.stats;

import in.potenfyr.statfyr.Statfyr;
import in.potenfyr.statfyr.compat.SchedulerCompat;
import in.potenfyr.statfyr.analytics.AnalyticsManager;
import in.potenfyr.statfyr.analytics.Metrics;
import in.potenfyr.statfyr.analytics.PlayerProfile;
import in.potenfyr.statfyr.compat.ServerVersion;
import in.potenfyr.statfyr.model.PlayerStats;
import in.potenfyr.statfyr.player.PlayerStateManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central stats management system.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>live player stat collection (real-time source for online players)</li>
 *     <li>per-player state updates through {@link PlayerStateManager}</li>
 *     <li>dirty marking and batched persistence hand-off</li>
 *     <li>final synchronization on quit</li>
 *     <li>offline stat resolution (persisted data first, stats file last)</li>
 * </ul>
 *
 * <p><strong>Source-of-truth model:</strong> Minecraft's
 * {@code world/stats/<uuid>.json} files are NOT a real-time source — the game
 * flushes them lazily. Online players are read through the live
 * {@code Player#getStatistic(...)} API on the main thread at a short,
 * configurable interval (default 1s). Results flow into the player's
 * {@code PlayerState} under the player's UUID lock, and only then into the
 * analytics profile and persistence queue. The stats JSON files are used
 * solely for initial import, recovery and as a last-resort offline fallback.
 *
 * <p><strong>Threading:</strong> the collector runs on the main thread
 * (Folia: global region scheduler) because vanilla statistics are plain hash
 * maps mutated by the server thread. Lock scopes are tiny: the collector
 * takes the UUID lock only to swap the immutable snapshot into the state,
 * never while doing I/O. Persistence happens asynchronously through the
 * {@code PersistenceService} with per-player version checks, so a stale
 * snapshot can never overwrite a newer one.
 */
public final class StatsManager {

    private final Statfyr plugin;

    private final StatsReader statsReader;

    /**
     * Shared per-player state (locks + versions + live snapshots).
     */
    private final PlayerStateManager stateManager;

    /**
     * Interval between live collection passes, in ticks. Derived from the
     * {@code collection.live-stats-interval-seconds} config value (1s
     * default = 20 ticks).
     */
    private final long updateIntervalTicks;

    /**
     * Metrics.
     */
    private final AtomicLong cacheHits = new AtomicLong();

    private final AtomicLong cacheMisses = new AtomicLong();

    private volatile boolean started;

    public StatsManager(
            Statfyr plugin,
            StatsReader statsReader,
            PlayerStateManager stateManager
    ) {

        this.plugin = plugin;
        this.statsReader = statsReader;
        this.stateManager = stateManager;

        long seconds =
                Math.max(
                        1L,
                        plugin.getConfig()
                                .getLong(
                                        "collection.live-stats-interval-seconds",
                                        1L
                                )
                );

        this.updateIntervalTicks = seconds * 20L;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() {

        if (started) {
            return;
        }

        started = true;

        /*
         * Vanilla statistic maps are mutated by the server thread, so the
         * collector must run there. The task is cheap: one pass over each
         * online player's stat maps, no I/O.
         */
        SchedulerCompat.runSyncRepeating(
                plugin,
                this::refreshOnlinePlayers,
                updateIntervalTicks,
                updateIntervalTicks
        );

        plugin.getLogger().info(
                "StatsManager started on "
                        + ServerVersion.getPlatformLabel()
                        + " (live interval "
                        + (updateIntervalTicks / 20L)
                        + "s)"
        );
    }

    // -------------------------------------------------------------------------
    // Lifecycle events
    // -------------------------------------------------------------------------

    /**
     * Prepares live tracking for a joining player. Called on the main thread
     * from the join handler.
     *
     * @param uuid joining player's UUID
     * @param name joining player's current name ({@code Player#getName()})
     */
    public void onJoin(UUID uuid, String name) {

        stateManager.lock(uuid);

        try {

            stateManager.markOnline(uuid, name);

        } finally {

            stateManager.unlock(uuid);
        }
    }

    /**
     * Final live synchronization for a quitting player: the very last live
     * statistics (taken on the main thread while the player object is still
     * valid) are written into the player state and scheduled for immediate
     * persistence before the state is marked offline.
     *
     * <p>After this call the persisted state represents the latest available
     * live state; the player stays fully queryable through the API.
     *
     * @param uuid leaving player's UUID
     * @param name leaving player's name
     */
    public void onQuit(UUID uuid, String name) {

        // 1. Final live read + state update + persist request (under lock).
        stateManager.lock(uuid);

        try {

            PlayerStateSnapshot snapshot =
                    readLiveUnderLock(uuid, name);

            if (snapshot != null) {

                // High priority: goes to the front of the next drain.
                plugin.getPersistenceService()
                        .submit(uuid, snapshot.version());
            }

        } finally {

            stateManager.unlock(uuid);
        }

        // 2. Mark offline (own lock scope, never overlapping I/O).
        stateManager.lock(uuid);

        try {

            stateManager.markOffline(uuid);

        } finally {

            stateManager.unlock(uuid);
        }
    }

    /**
     * Reads the player's live statistics into their state; must be called on
     * the main thread (e.g. from the quit handler). Returns the resulting
     * state version so the caller can schedule persistence.
     */
    private PlayerStateSnapshot readLiveUnderLock(UUID uuid, String name) {

        Player player = Bukkit.getPlayer(uuid);

        if (player == null) {
            return null;
        }

        PlayerStats stats = LiveStatisticsReader.read(player);

        PlayerStateManager.PlayerState state =
                stateManager.updateStatistics(uuid, stats);

        if (state == null) {

            state = stateManager.loadOrCreate(uuid, name, false);

            state = stateManager.updateStatistics(uuid, stats);
        }

        return state == null
                ? null
                : new PlayerStateSnapshot(state.uuid(), state.version());
    }

    /**
     * Simple (uuid, version) tuple used for persistence hand-off.
     */
    private static final class PlayerStateSnapshot {

        private final UUID uuid;

        private final long version;

        PlayerStateSnapshot(UUID uuid, long version) {

            this.uuid = uuid;
            this.version = version;
        }

        UUID uuid() {
            return uuid;
        }

        long version() {
            return version;
        }
    }

    // -------------------------------------------------------------------------
    // Main access: resolves stats for any player, online or offline
    // -------------------------------------------------------------------------

    /**
     * Returns the latest statistics for a player.
     *
     * <p>Online players resolve from the live {@code PlayerState} maintained
     * by the collector (at most one collection interval old). Offline players
     * resolve from the persisted analytics profile (updated by the batched
     * persistence pipeline), falling back to the vanilla stats file only when
     * no persisted data exists (e.g. first import or recovery).
     *
     * @param uuid player UUID
     * @return latest available statistics, or {@code null}
     */
    public PlayerStats getPlayerStats(UUID uuid) {

        if (uuid == null) {
            return null;
        }

        PlayerStateManager.PlayerState state =
                stateManager.state(uuid);

        // LIVE PLAYER: latest in-memory snapshot (never blocks, never reads
        // the stats JSON file).
        if (state != null && state.online() && state.statistics() != null) {

            cacheHits.incrementAndGet();

            return state.statistics();
        }

        cacheMisses.incrementAndGet();

        // OFFLINE PLAYER: persisted Statfyr state first (live-state merged
        // with the analytics profile), then the vanilla stats file as a
        // recovery/import fallback.
        AnalyticsManager analytics =
                plugin.getAnalytics();

        PlayerStats stats =
                analytics == null
                        ? null
                        : analytics.getOfflinePlayerStats(uuid);

        if (stats == null) {

            stats =
                    statsReader.readStats(uuid, null);
        }

        return stats;
    }

    /**
     * Async variant of {@link #getPlayerStats(UUID)} for HTTP workers.
     *
     * @param uuid player UUID
     * @return future resolving to the latest statistics
     */
    public CompletableFuture<PlayerStats> getPlayerStatsAsync(UUID uuid) {

        return CompletableFuture.supplyAsync(
                () -> getPlayerStats(uuid),
                plugin.getExecutorService()
        );
    }

    // -------------------------------------------------------------------------
    // Refresh (main thread)
    // -------------------------------------------------------------------------

    /**
     * One collector pass: read live statistics for every online player,
     * update their {@code PlayerState} under the per-UUID lock, mark the
     * state dirty and hand the new version to the batched persistence
     * pipeline. Nothing here touches disk or network.
     */
    private void refreshOnlinePlayers() {

        for (Player player
                : Bukkit.getOnlinePlayers()) {

            UUID uuid =
                    player.getUniqueId();

            try {

                PlayerStats live =
                        LiveStatisticsReader.read(player);

                stateManager.lock(uuid);

                try {

                    PlayerStateManager.PlayerState state =
                            stateManager.updateStatistics(uuid, live);

                    if (state == null) {
                        state =
                                stateManager.loadOrCreate(
                                        uuid,
                                        player.getName(),
                                        true
                                );

                        state =
                                stateManager.updateStatistics(uuid, live);
                    }

                    if (state != null && state.dirty()) {

                        plugin.getPersistenceService()
                                .submit(uuid, state.version());
                    }

                } finally {

                    stateManager.unlock(uuid);
                }

                // Feed the analytics profile outside the UUID lock (the
                // profile has its own synchronisation and this call performs
                // MAX-merging only).
                feedProfile(player, live);

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

    /**
     * MAX-merges the live snapshot into the player's analytics profile so
     * allTime metrics and item breakdowns stay current while the player is
     * online. Runs outside the UUID lock; {@code PlayerProfile} uses
     * concurrent maps internally.
     */
    private void feedProfile(Player player, PlayerStats live) {

        AnalyticsManager analytics =
                plugin.getAnalytics();

        if (analytics == null) {
            return;
        }

        PlayerProfile profile =
                analytics.profileIfPresent(player.getUniqueId());

        if (profile == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        stateManager.lock(uuid);

        try {
            profile.addStatSnapshot(live.getRawStats());

            Map<String, Map<String, Long>> rawStats =
                    live.getRawStats();

            Map<String, Long> customStats =
                    rawStats.get(StatKeys.CATEGORY_CUSTOM);

            if (customStats == null) {
                return;
            }

            for (Map.Entry<String, Long> entry : customStats.entrySet()) {

                String key = entry.getKey();
                Long value = entry.getValue();

                if (key == null || value == null) {
                    continue;
                }

                long existing =
                        profile.allTime(key);

                if (value > existing) {
                    profile.allTime.put(key, value);
                }
            }
        } finally {
            stateManager.unlock(uuid);
        }
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    public int getCacheSize() {
        return stateManager.stateCount();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public void clearCache() {
        // Live state is authoritative; nothing to clear. Kept for API compat.
    }

    public int getLockCount() {
        return stateManager.lockCount();
    }
}

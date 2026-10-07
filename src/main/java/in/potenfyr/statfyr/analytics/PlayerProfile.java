package in.potenfyr.statfyr.analytics;

import in.potenfyr.statfyr.stats.StatKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent per-player analytics profile.
 *
 * <p>Fields are public and serialised directly by Gson to
 * {@code plugins/statfyr/data/players/<uuid>.json}. Transient fields are
 * runtime-only and never written to disk.
 */
public final class PlayerProfile {

    // -- identity ------------------------------------------------------------
    public String uuid;
    public String name;

    // -- presence ------------------------------------------------------------
    public long firstSeen;
    public long lastSeen;
    public long lastJoin;

    // -- sessions ------------------------------------------------------------
    public long totalPlaytimeSeconds;
    public long activeSeconds;
    public long afkSeconds;
    public int totalSessions;
    public long longestSessionSeconds;
    public long currentSessionStart;

    // -- metrics -------------------------------------------------------------
    /** All-time canonical metric totals. */
    public Map<String, Long> allTime =
            new ConcurrentHashMap<>();

    /** Metric value captured at the start of each period window. */
    public Map<String, Map<String, Long>> periodBaselines =
            new ConcurrentHashMap<>();

    /** Epoch millis at which each period window started. */
    public Map<String, Long> periodStarts =
            new ConcurrentHashMap<>();

    /** Third-party metrics registered through the custom metrics API. */
    public Map<String, Double> customMetrics =
            new ConcurrentHashMap<>();

    /** Achieved milestones: milestone key -> epoch millis. */
    public Map<String, Long> milestones =
            new ConcurrentHashMap<>();

    // -- raw stat snapshots (for item breakdowns) ---------------------------
    /**
     * Raw stat snapshots captured from Bukkit statistics.
     * Each snapshot is a map of category -> (item -> count).
     * Used to preserve item breakdowns (mined, crafted, used, broken, etc.)
     * for offline players when vanilla stats files may not have recent data.
     */
    public List<Map<String, Map<String, Long>>> statSnapshots =
            new ArrayList<>();

    /** Maximum number of raw stat snapshots to retain per profile. */
    public int maxSnapshots = 1000;

    // -- runtime only --------------------------------------------------------
    public transient long lastAccrual;
    public transient long lastActivity;
    public transient boolean afk;
    public transient Map<String, Long> sessionStartMetrics = new HashMap<>();
    public transient long lastSnapshotAt;

    public PlayerProfile() {
    }

    public PlayerProfile(UUID id, String name, long now) {

        this.uuid = id.toString();
        this.name = name;
        this.firstSeen = now;
        this.lastSeen = now;
        this.lastJoin = now;
        this.lastAccrual = now;
        this.lastActivity = now;
    }

    /**
     * @return the parsed UUID, or {@code null} when malformed
     */
    public UUID id() {

        try {

            return UUID.fromString(uuid);

        } catch (Exception ignored) {

            return null;
        }
    }

    /**
     * @param metric canonical metric key
     * @return the all-time value, never null
     */
    public long allTime(String metric) {

        Long value =
                allTime.get(metric);

        return value == null ? 0L : value;
    }

    /**
     * Starts a session.
     *
     * @param now           current epoch millis
     * @param currentCounts snapshot of canonical metric values at join
     */
    public void beginSession(long now, Map<String, Long> currentCounts) {

        if (currentSessionStart == 0L) {
            currentSessionStart = now;
        }

        lastJoin = now;
        lastSeen = now;
        lastAccrual = now;
        lastActivity = now;
        afk = false;
        totalSessions++;

        sessionStartMetrics =
                currentCounts == null
                        ? new HashMap<>()
                        : new HashMap<>(currentCounts);
    }

    /**
     * Ends a session.
     *
     * @param now current epoch millis
     * @return the session duration in seconds
     */
    public long endSession(long now) {

        lastSeen = now;

        if (currentSessionStart <= 0L) {
            currentSessionStart = 0L;
            afk = false;
            return 0L;
        }

        long seconds =
                Math.max(
                        0L,
                        (now - currentSessionStart) / 1000L
                );

        totalPlaytimeSeconds += seconds;

        if (seconds > longestSessionSeconds) {
            longestSessionSeconds = seconds;
        }

        currentSessionStart = 0L;
        afk = false;

        return seconds;
    }

    /**
     * Accrues elapsed time into either the active or AFK bucket.
     *
     * @param now        current epoch millis
     * @param isAfk      whether the player is currently AFK
     * @param activeNow  when true, accrue into active time, else AFK time
     */
    public void accrue(long now, boolean isAfk, boolean activeNow) {

        long elapsed =
                Math.max(0L, (now - lastAccrual) / 1000L);

        lastAccrual = now;

        if (elapsed <= 0L) {
            return;
        }

        if (activeNow && !isAfk) {
            activeSeconds += elapsed;
        } else {
            afkSeconds += elapsed;
        }
    }

    /**
     * @return total metric value including session-derived metrics
     */
    public long total(String metric) {

        switch (metric) {

            case Metrics.PLAYTIME:
                // Prefer session-derived playtime when the vanilla value is
                // lower (fresh profile) so the number is always sensible.
                return Math.max(
                        allTime(metric),
                        totalPlaytimeSeconds
                );

            case Metrics.ACTIVE_TIME:
                return activeSeconds;

            case Metrics.AFK_TIME:
                return afkSeconds;

            case Metrics.SESSIONS:
                return totalSessions;

            default:
                return allTime(metric);
        }
    }

    // -------------------------------------------------------------------------
    // Raw stat snapshot management
    // -------------------------------------------------------------------------

    /**
     * Adds a raw stat snapshot to the profile.
     * Snapshots are retained up to {@link #maxSnapshots}, with oldest evicted first.
     *
     * @param rawStats the raw stats map (category -> item -> count)
     */
    public void addStatSnapshot(Map<String, Map<String, Long>> rawStats) {

        if (rawStats == null || rawStats.isEmpty()) {
            return;
        }

        // Add a defensive copy to avoid mutation by external code
        Map<String, Map<String, Long>> snapshotCopy =
                new HashMap<>();

        for (Map.Entry<String, Map<String, Long>> entry : rawStats.entrySet()) {
            snapshotCopy.put(
                    entry.getKey(),
                    new HashMap<>(entry.getValue())
            );
        }

        statSnapshots.add(snapshotCopy);

        // Evict oldest if exceeding maxSnapshots
        while (statSnapshots.size() > maxSnapshots) {
            statSnapshots.remove(0);
        }
    }

    /**
     * Returns the most recent raw stat snapshot, or null if no snapshots exist.
     *
     * @return the latest snapshot, or null
     */
    public Map<String, Map<String, Long>> getLatestSnapshot() {

        if (statSnapshots.isEmpty()) {
            return null;
        }

        return statSnapshots.get(statSnapshots.size() - 1);
    }

    /**
     * Returns merged stats combining profile totals with the latest snapshot's
     * item breakdowns. This is useful for displaying complete stats for offline
     * players.
     *
     * @return merged stats map (category -> item -> count)
     */
    public Map<String, Map<String, Long>> getMergedStats() {

        Map<String, Map<String, Long>> merged =
                new HashMap<>();

        // Start with profile's allTime metrics as a baseline
        Map<String, Long> allTimeCopy =
                new HashMap<>(allTime);

        // Add custom metrics category from allTime
        Map<String, Long> customCategory =
                new HashMap<>();

        for (String key : allTimeCopy.keySet()) {
            if (!key.equals(Metrics.PLAYTIME)
                    && !key.equals(Metrics.ACTIVE_TIME)
                    && !key.equals(Metrics.AFK_TIME)
                    && !key.equals(Metrics.SESSIONS)) {
                customCategory.put(key, allTimeCopy.get(key));
            }
        }

        if (!customCategory.isEmpty()) {
            merged.put(StatKeys.CATEGORY_CUSTOM, customCategory);
        }

        // Add session-derived metrics if they have values
        Map<String, Long> sessionMetrics =
                new HashMap<>();

        if (totalPlaytimeSeconds > 0) {
            sessionMetrics.put(Metrics.PLAYTIME, totalPlaytimeSeconds);
        }
        if (activeSeconds > 0) {
            sessionMetrics.put(Metrics.ACTIVE_TIME, activeSeconds);
        }
        if (afkSeconds > 0) {
            sessionMetrics.put(Metrics.AFK_TIME, afkSeconds);
        }
        if (totalSessions > 0) {
            sessionMetrics.put(Metrics.SESSIONS, (long) totalSessions);
        }

        if (!sessionMetrics.isEmpty()) {
            merged.put(StatKeys.CATEGORY_CUSTOM, sessionMetrics);
        }

        // Merge with latest snapshot item breakdowns if available
        Map<String, Map<String, Long>> latestSnapshot =
                getLatestSnapshot();

        if (latestSnapshot != null) {

            for (Map.Entry<String, Map<String, Long>> entry :
                    latestSnapshot.entrySet()) {

                String category = entry.getKey();
                Map<String, Long> items = entry.getValue();

                if (category.equals(StatKeys.CATEGORY_CUSTOM)) {
                    // Custom category already handled above, skip or merge
                    continue;
                }

                Map<String, Long> existingItems =
                        merged.get(category);

                if (existingItems == null) {
                    merged.put(category, new HashMap<>(items));
                } else {
                    // Merge items, taking the max value for each item
                    for (Map.Entry<String, Long> itemEntry : items.entrySet()) {
                        String item = itemEntry.getKey();
                        Long existing = existingItems.get(item);
                        Long value = itemEntry.getValue();

                        if (existing == null || value > existing) {
                            existingItems.put(item, value);
                        }
                    }
                }
            }
        }

        return merged;
    }
}

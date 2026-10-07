package in.potenfyr.statfyr.analytics;

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
}

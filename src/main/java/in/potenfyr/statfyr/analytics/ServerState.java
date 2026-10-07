package in.potenfyr.statfyr.analytics;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persistent server-level analytics state.
 *
 * <p>Serialised to {@code plugins/statfyr/data/server.json}. Day-keyed maps
 * are pruned according to the configured retention window so this file never
 * grows without bound.
 *
 * <p>All maps are concurrent: sessions are recorded from join/quit events and
 * async snapshot tasks while HTTP threads iterate them for summaries,
 * retention and leaderboards. Per-day player lists are copy-on-write because
 * they are read far more often than they are appended to.
 */
public final class ServerState {

    /** server-id from config (multi-server ready). */
    public String serverId = "server-1";

    /** Friendly server name. */
    public String serverName = "Survival";

    /** Highest concurrent player count ever seen. */
    public volatile int peakAllTime;

    /** Highest concurrent player count per ISO day (yyyy-MM-dd). */
    public final Map<String, Integer> peakByDay = new ConcurrentHashMap<>();

    /** Distinct player UUIDs seen per ISO day. */
    public final Map<String, List<String>> playersByDay = new ConcurrentHashMap<>();

    /** Session count per ISO day. */
    public final Map<String, Integer> sessionsByDay = new ConcurrentHashMap<>();

    /** Players whose first ever join happened on this day. */
    public final Map<String, Integer> newPlayersByDay = new ConcurrentHashMap<>();

    /** Every player UUID that has ever been seen. */
    public final Set<String> knownPlayers = ConcurrentHashMap.newKeySet();

    /** Total sessions recorded. */
    public volatile long totalSessions;

    /** All-time session count grouped by hour of day (0-23). */
    public final Map<String, Integer> sessionsByHour = new ConcurrentHashMap<>();

    /** All-time session count grouped by weekday name. */
    public final Map<String, Integer> sessionsByWeekday = new ConcurrentHashMap<>();

    /** Last archived window start per period token. */
    public final Map<String, Long> periodWindowStarts = new ConcurrentHashMap<>();

    // -- helpers -------------------------------------------------------------

    /**
     * Records a session start.
     *
     * @param day     ISO day string
     * @param hour    hour of day (0-23)
     * @param weekday weekday name
     * @param uuid    player UUID string
     * @param isNew   whether this is the player's first ever session
     */
    public void recordSession(
            String day,
            int hour,
            String weekday,
            String uuid,
            boolean isNew
    ) {

        totalSessions++;

        bump(sessionsByDay, day);
        bump(sessionsByHour, String.valueOf(hour));
        bump(sessionsByWeekday, weekday);

        if (isNew) {
            bump(newPlayersByDay, day);
        }

        List<String> players =
                new CopyOnWriteArrayList<>();

        List<String> existing =
                playersByDay.putIfAbsent(day, players);

        if (existing != null) {
            players = existing;
        }

        if (!players.contains(uuid)) {
            players.add(uuid);
        }
    }

    /**
     * Records a concurrent-player observation.
     *
     * @param day    ISO day string
     * @param online current concurrent players
     */
    public void recordPeak(String day, int online) {

        if (online > peakAllTime) {
            peakAllTime = online;
        }

        Integer current =
                peakByDay.get(day);

        if (current == null || online > current) {

            // putFirst-style "keep the maximum" under concurrency: only the
            // higher value wins regardless of arrival order.
            Integer sentinel =
                    peakByDay.putIfAbsent(day, online);

            if (sentinel != null && online > sentinel) {
                peakByDay.replace(day, sentinel, online);
            }
        }
    }

    /**
     * @param days list of ISO day strings
     * @return distinct players seen across those days
     */
    public Set<String> uniqueAcross(java.util.List<String> days) {

        Set<String> unique =
                new HashSet<>();

        for (String day : days) {

            List<String> players =
                    playersByDay.get(day);

            if (players != null) {
                unique.addAll(players);
            }
        }

        return unique;
    }

    private static void bump(Map<String, Integer> map, String key) {

        Integer current =
                map.get(key);

        if (current == null) {

            Integer race =
                    map.putIfAbsent(key, 1);

            if (race != null) {
                map.replace(key, race, race + 1);
            }

            return;
        }

        map.replace(key, current, current + 1);
    }
}

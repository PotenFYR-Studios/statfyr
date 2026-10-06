package in.potenfyr.statfyr.analytics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persistent server-level analytics state.
 *
 * <p>Serialised to {@code plugins/statfyr/data/server.json}. Day-keyed maps
 * are pruned according to the configured retention window so this file never
 * grows without bound.
 */
public final class ServerState {

    /** server-id from config (multi-server ready). */
    public String serverId = "server-1";

    /** Friendly server name. */
    public String serverName = "Survival";

    /** Highest concurrent player count ever seen. */
    public int peakAllTime;

    /** Highest concurrent player count per ISO day (yyyy-MM-dd). */
    public Map<String, Integer> peakByDay = new HashMap<>();

    /** Distinct player UUIDs seen per ISO day. */
    public Map<String, java.util.List<String>> playersByDay = new HashMap<>();

    /** Session count per ISO day. */
    public Map<String, Integer> sessionsByDay = new HashMap<>();

    /** Players whose first ever join happened on this day. */
    public Map<String, Integer> newPlayersByDay = new HashMap<>();

    /** Every player UUID that has ever been seen. */
    public Set<String> knownPlayers = new HashSet<>();

    /** Total sessions recorded. */
    public long totalSessions;

    /** All-time session count grouped by hour of day (0-23). */
    public Map<String, Integer> sessionsByHour = new HashMap<>();

    /** All-time session count grouped by weekday name. */
    public Map<String, Integer> sessionsByWeekday = new HashMap<>();

    /** Last archived window start per period token. */
    public Map<String, Long> periodWindowStarts = new HashMap<>();

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

        sessionsByDay.merge(day, 1, Integer::sum);
        sessionsByHour.merge(String.valueOf(hour), 1, Integer::sum);
        sessionsByWeekday.merge(weekday, 1, Integer::sum);

        if (isNew) {
            newPlayersByDay.merge(day, 1, Integer::sum);
        }

        java.util.List<String> players =
                playersByDay.computeIfAbsent(
                        day,
                        ignored -> new java.util.ArrayList<>()
                );

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
            peakByDay.put(day, online);
        }
    }

    /**
     * @param days list of ISO day strings
     * @return distinct players seen across those days
     */
    public Set<String> uniqueAcross(java.util.List<String> days) {

        Set<String> unique =
                new LinkedHashSet<>();

        for (String day : days) {

            java.util.List<String> players =
                    playersByDay.get(day);

            if (players != null) {
                unique.addAll(players);
            }
        }

        return unique;
    }
}

package in.potenfyr.statfyr.analytics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregated server-level analytics snapshot.
 */
public final class ServerStats {

    public int online;
    public int peakToday;
    public int peakWeek;
    public int peakMonth;
    public int peakAllTime;

    public double averageConcurrent;

    public int uniqueToday;
    public int uniqueWeek;
    public int uniqueMonth;
    public int totalPlayers;

    public long totalPlaytimeSeconds;
    public double averageSessionSeconds;
    public double sessionsPerDay;

    public int newPlayersToday;
    public int returningPlayersToday;

    public long sessionsTotal;

    /**
     * @return a JSON-friendly ordered map
     */
    public Map<String, Object> toMap() {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("online", online);
        map.put("peak_today", peakToday);
        map.put("peak_week", peakWeek);
        map.put("peak_month", peakMonth);
        map.put("peak_all_time", peakAllTime);
        map.put("average_concurrent", averageConcurrent);
        map.put("unique_players_today", uniqueToday);
        map.put("unique_players_week", uniqueWeek);
        map.put("unique_players_month", uniqueMonth);
        map.put("total_players", totalPlayers);
        map.put("total_playtime_seconds", totalPlaytimeSeconds);
        map.put("average_session_seconds", averageSessionSeconds);
        map.put("sessions_per_day", sessionsPerDay);
        map.put("new_players_today", newPlayersToday);
        map.put("returning_players_today", returningPlayersToday);
        map.put("sessions_total", sessionsTotal);

        return map;
    }
}
